package app.sewain.integrations.whatsapp;

import app.sewain.integrations.OutboundWhatsAppProvider;
import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SuppressWarnings({"unchecked","rawtypes"})
class WhatsAppWorkflowTest {
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final TransactionTemplate transactions=mock(TransactionTemplate.class);
    final WhatsAppGateway gateway=mock(WhatsAppGateway.class);
    final SimpMessagingTemplate socket=mock(SimpMessagingTemplate.class);
    final WhatsAppService service=new WhatsAppService(jdbc,transactions,gateway,socket);
    final ObjectMapper json=new ObjectMapper();
    final UUID session=UUID.randomUUID(),user=UUID.randomUUID();
    @BeforeEach void setup() {
        TenantContext.set("shop");when(gateway.configured()).thenReturn(true);
        when(transactions.execute(any())).thenAnswer(c->((TransactionCallback<?>)c.getArgument(0)).doInTransaction(null));
        doAnswer(c->{((Consumer<org.springframework.transaction.TransactionStatus>)c.getArgument(0)).accept(null);return null;}).when(transactions).executeWithoutResult(any());
    }
    @AfterEach void clear(){TenantContext.clear();}
    void installed(boolean enabled) {
        when(jdbc.query(eq("SELECT * FROM public.whatsapp_sessions WHERE tenant=?"),any(RowMapper.class),eq("shop")))
                .thenReturn(List.of(new WhatsAppService.Session(session,"shop",enabled)));
    }
    void callbackSession() {
        when(jdbc.query(contains("session_id=? AND enabled=true"),any(RowMapper.class),eq(session)))
                .thenReturn(List.of(new WhatsAppService.Session(session,"shop",true)));
    }
    @Test void absentDisabledConnectedAndUnavailableStatusesAreDistinct() {
        assertEquals("NOT_CONNECTED",service.status().state());installed(false);assertEquals("DISCONNECTED",service.status().state());
        installed(true);when(gateway.snapshot(session)).thenReturn(json.readTree("{\"state\":\"CONNECTED\",\"phone\":\"628123\",\"qr\":null}"));
        var status=service.status();assertEquals("CONNECTED",status.state());assertEquals("628123",status.phone());assertNull(status.qr());
        when(gateway.snapshot(session)).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));assertEquals("UNAVAILABLE",service.status().state());
    }
    @Test void connectRequiresConfiguredGatewayAndEnablesPersistedInstallation() {
        when(gateway.configured()).thenReturn(false);assertThrows(ResponseStatusException.class,service::connect);verifyNoInteractions(jdbc);
        when(gateway.configured()).thenReturn(true);installed(true);when(gateway.snapshot(session)).thenReturn(json.createObjectNode());
        assertEquals("STARTING",service.connect().state());verify(gateway).connect(session);
        verify(jdbc).update(eq("UPDATE public.whatsapp_sessions SET enabled=true WHERE session_id=?"),eq(session));
        verify(jdbc).update(contains("integration_installations"),any(UUID.class),eq(true));
    }
    @Test void disconnectDisablesDeliveryBeforeGatewayTerminationAndNotifiesMembers() {
        installed(true);when(jdbc.queryForList(contains("merchant_users"),eq(UUID.class),eq("shop"))).thenReturn(List.of(user));
        service.disconnect();var sequence=inOrder(jdbc,gateway);
        sequence.verify(jdbc).update(eq("UPDATE public.whatsapp_sessions SET enabled=false WHERE session_id=?"),eq(session));
        sequence.verify(gateway).disconnect(session);verify(socket).convertAndSendToUser(eq(user.toString()),eq("/queue/whatsapp"),any(Object.class));
    }
    @Test void disconnectWithoutSessionDoesNothing() {service.disconnect();verifyNoInteractions(gateway);}
    @Test void recoveryMarksSendingMessagesUnknownAndRejectsUnsafeSchemaNames() {
        when(jdbc.queryForList(anyString(),eq(String.class))).thenReturn(List.of("shop"));service.recoverPendingMessages();
        verify(jdbc).update(contains("UPDATE \"shop\".integration_whatsapp_messages SET status='UNKNOWN'"));
        when(jdbc.queryForList(anyString(),eq(String.class))).thenReturn(List.of("unsafe;drop"));assertThrows(IllegalArgumentException.class,service::recoverPendingMessages);
    }
    @Test void sendRequiresEnabledSession() {
        var text=new OutboundWhatsAppProvider.Text(UUID.randomUUID(),"628123456789","Hello");
        assertThrows(ResponseStatusException.class,()->service.send(text));installed(false);assertThrows(ResponseStatusException.class,()->service.send(text));verify(gateway,never()).send(any(),anyString(),anyString());
    }
    @Test void successfulSendNormalizesPeerAndStoresGatewayMessageId() throws Exception {
        installed(true);when(jdbc.update(startsWith("INSERT INTO \"shop\".integration_whatsapp_messages"),any(Object[].class))).thenReturn(1);
        when(gateway.send(eq(session),eq("628123456789@c.us"),eq("Hello"))).thenReturn(json.readTree("{\"message\":{\"id\":{\"_serialized\":\"external-123\"}}}"));
        var expected=new WhatsAppService.Message(UUID.randomUUID(),"OUTBOUND","SENT","628123456789@c.us","Hello",Instant.now());
        when(jdbc.query(contains("WHERE id=?"),any(RowMapper.class),any(UUID.class))).thenReturn(List.of(expected));
        assertEquals(expected,service.send(new OutboundWhatsAppProvider.Text(UUID.randomUUID(),"+628123456789","Hello")));
        verify(jdbc).update(contains("SET status='SENT',external_id=?"),eq("external-123"),any(UUID.class));
    }
    @Test void timeoutRecordsUnknownWithoutAutomaticResend() {
        installed(true);when(jdbc.update(startsWith("INSERT INTO \"shop\".integration_whatsapp_messages"),any(Object[].class))).thenReturn(1);
        when(gateway.send(any(),anyString(),anyString())).thenThrow(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT));
        when(jdbc.query(contains("WHERE id=?"),any(RowMapper.class),any(UUID.class))).thenReturn(List.of(new WhatsAppService.Message(UUID.randomUUID(),"OUTBOUND","UNKNOWN","628123@c.us","Hello",Instant.now())));
        service.send(new OutboundWhatsAppProvider.Text(UUID.randomUUID(),"628123","Hello"));
        verify(jdbc).update(contains("SET status='UNKNOWN' WHERE id=?"),any(UUID.class));verify(gateway,times(1)).send(any(),anyString(),anyString());
    }
    @Test void inboundTextIsIdempotentlyInsertedAndTruncated() {
        callbackSession();var data=json.createObjectNode();var message=data.putObject("message");message.putObject("id").put("_serialized","external");
        message.put("from","628123@c.us").put("body","x".repeat(5000)).put("type","chat").put("fromMe",false);
        service.callback(session,"message",data);
        verify(jdbc).update(contains("ON CONFLICT(session_id,external_id) DO NOTHING"),any(UUID.class),eq(session),eq("external"),eq("628123@c.us"),eq("x".repeat(4096)));
    }
    @Test void invalidPeerOwnMessagesAttachmentsAndBlankTextAreIgnored() {
        callbackSession();
        for(var override:List.of(Map.of("from","group@g.us"),Map.of("fromMe",true),Map.of("type","image"),Map.of("body"," "))) {
            var data=json.createObjectNode();var message=data.putObject("message");message.putObject("id").put("_serialized","external");
            message.put("from","628123@c.us").put("body","Hello").put("type","chat").put("fromMe",false);
            override.forEach((key,value)->message.set(key,json.valueToTree(value)));service.callback(session,"message",data);
        }
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void notificationFailureDoesNotBreakConnectionCallback() {
        callbackSession();when(jdbc.queryForList(contains("merchant_users"),eq(UUID.class),eq("shop"))).thenReturn(List.of(user));
        doThrow(new IllegalStateException("Disconnected browser")).when(socket).convertAndSendToUser(anyString(),anyString(),any(Object.class));
        assertDoesNotThrow(()->service.callback(session,"connected",json.createObjectNode()));
    }
    @Test void messagesEnforcePaginationAndMapDatabaseTimestamps() throws Exception {
        assertThrows(ResponseStatusException.class,()->service.messages(-1));assertThrows(ResponseStatusException.class,()->service.messages(10001));
        when(jdbc.queryForObject(anyString(),eq(Long.class))).thenReturn(1L);
        var id=UUID.randomUUID();var at=Instant.parse("2026-01-01T00:00:00Z");var rs=mock(ResultSet.class);
        when(rs.getObject("id",UUID.class)).thenReturn(id);when(rs.getTimestamp("occurred_at")).thenReturn(Timestamp.from(at));
        when(rs.getString("direction")).thenReturn("INBOUND");when(rs.getString("body")).thenReturn("Hello");
        when(jdbc.query(contains("ORDER BY occurred_at"),any(RowMapper.class),eq(60)))
                .thenAnswer(c->List.of(((RowMapper<?>)c.getArgument(1)).mapRow(rs,0)));
        var result=service.messages(2);assertEquals(1,result.total());assertEquals(id,result.items().getFirst().id());assertEquals(at,result.items().getFirst().occurredAt());
    }
}
