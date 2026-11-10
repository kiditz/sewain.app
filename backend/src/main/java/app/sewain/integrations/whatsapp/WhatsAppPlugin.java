package app.sewain.integrations.whatsapp;

import app.sewain.integrations.OutboundWhatsAppProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Component
@Validated
@RequiredArgsConstructor
class WhatsAppPlugin implements OutboundWhatsAppProvider {
	private final WhatsAppService service;

	public String code() {
		return "whatsapp";
	}

	public String name() {
		return "WhatsApp";
	}

	public String description() {
		return "Connect a merchant WhatsApp number using the local WhatsApp Web gateway.";
	}

	public Set<String> capabilities() {
		return Set.of("WHATSAPP_OUTBOUND", "WHATSAPP_INBOUND");
	}

	public Delivery send(Text text) {
		var message = service.send(text);
		return new Delivery(message.id(), message.status());
	}
}
