package app.sewain.integrations.whatsapp;

import app.sewain.integrations.OutboundWhatsAppProvider;
import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WhatsAppServiceTest {
	@AfterEach
	void clear() {
		TenantContext.clear();
	}

	@Test
	void ignoresUnknownOrDisabledSessionCallbacks() {
		var jdbc = mock(JdbcTemplate.class);
		when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class))).thenReturn(List.of());
		var service = new WhatsAppService(jdbc, mock(TransactionTemplate.class), mock(WhatsAppGateway.class), mock(SimpMessagingTemplate.class));
		service.callback(UUID.randomUUID(), "message", JsonMapper.builder().build().createObjectNode());
		verify(jdbc, never()).update(anyString(), any(Object[].class));
	}

	@Test
	void refusesPublicContextBeforeAccessingMessages() {
		var jdbc = mock(JdbcTemplate.class);
		var service = new WhatsAppService(jdbc, mock(TransactionTemplate.class), mock(WhatsAppGateway.class), mock(SimpMessagingTemplate.class));
		TenantContext.set("public");
		assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.messages(0));
		verifyNoInteractions(jdbc);
	}

	@Test
	void duplicateRequestIdReturnsPriorDeliveryWithoutSendingAgain() {
		TenantContext.set("shop_a");
		var jdbc = mock(JdbcTemplate.class);
		var gateway = mock(WhatsAppGateway.class);
		var id = UUID.randomUUID();
		var session = UUID.randomUUID();
		var request = UUID.randomUUID();
		when(jdbc.query(eq("SELECT * FROM public.whatsapp_sessions WHERE tenant=?"), any(RowMapper.class), eq("shop_a")))
				.thenReturn(List.of(new WhatsAppService.Session(session, "shop_a", true)));
		when(jdbc.query(contains("WHERE request_id=?"), any(RowMapper.class), eq(request)))
				.thenReturn(List.of(new WhatsAppService.Message(id, "OUTBOUND", "SENT", "628123456789@c.us", "Hello", java.time.Instant.now())));
		var service = new WhatsAppService(jdbc, mock(TransactionTemplate.class), gateway, mock(SimpMessagingTemplate.class));
		var result = service.send(new OutboundWhatsAppProvider.Text(request, "628123456789", "Hello"));
		assertEquals(id, result.id());
		verifyNoInteractions(gateway);
		assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.send(new OutboundWhatsAppProvider.Text(request, "628123456789", "Different")));
		verifyNoInteractions(gateway);
	}
}
