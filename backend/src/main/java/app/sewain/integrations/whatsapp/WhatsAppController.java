package app.sewain.integrations.whatsapp;

import app.sewain.integrations.OutboundWhatsAppProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/integrations/whatsapp")
@RequiredArgsConstructor
class WhatsAppController {
	private final WhatsAppService service;

	@GetMapping
	ResponseEntity<WhatsAppService.Status> status() {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.status());
	}

	@PostMapping("/connect")
	WhatsAppService.Status connect() {
		return service.connect();
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void disconnect() {
		service.disconnect();
	}

	@GetMapping("/messages")
	WhatsAppService.Messages messages(@RequestParam(defaultValue = "0") int page) {
		return service.messages(page);
	}

	@PostMapping("/messages")
	WhatsAppService.Message send(@Valid @RequestBody OutboundWhatsAppProvider.Text text) {
		return service.send(text);
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<Map<String, String>> failed(ResponseStatusException error) {
		return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", java.util.Objects.requireNonNullElse(error.getReason(), "Integrasi WhatsApp gagal")));
	}
}
