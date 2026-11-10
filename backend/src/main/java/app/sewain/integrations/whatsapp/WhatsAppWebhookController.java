package app.sewain.integrations.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@RestController
class WhatsAppWebhookController {
    private final WhatsAppService service;
    private final ObjectMapper mapper;
    private final String secret;
    WhatsAppWebhookController(WhatsAppService service,ObjectMapper mapper,@Value("${app.integrations.whatsapp.webhook-secret:}") String secret) {
        this.service=service; this.mapper=mapper; this.secret=secret;
    }
    @PostMapping("/api/integrations/whatsapp/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void receive(@RequestHeader(value="X-Webhook-Timestamp",defaultValue="") String timestamp,
                 @RequestHeader(value="X-Webhook-Signature",defaultValue="") String signature,
                 jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        // Read a bounded raw body: the signature must match exactly the bytes Node transmitted.
        var body=request.getInputStream().readNBytes(65537);
        if (body.length>65536) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        if (!valid(secret,timestamp,signature,body,Instant.now().getEpochSecond())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            var event=mapper.readTree(body);
            var id=UUID.fromString(event.path("sessionId").asString(""));
            var type=event.path("dataType").asString("");
            if (Set.of("qr","ready","authenticated","status","change_state","disconnected","message").contains(type))
                service.callback(id,type,event.path("data"));
        } catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    }
    static boolean valid(String secret,String timestamp,String signature,byte[] body,long now) {
        if (secret.isBlank() || timestamp.length()>12 || signature.length()!=64) return false;
        try {
            long sent=Long.parseLong(timestamp);
            if (sent<now-300 || sent>now+30) return false;
            var mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            mac.update((timestamp+".").getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(mac.doFinal(body),HexFormat.of().parseHex(signature));
        } catch (Exception error) { return false; }
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Void> failed(ResponseStatusException error) { return ResponseEntity.status(error.getStatusCode()).build(); }
}
