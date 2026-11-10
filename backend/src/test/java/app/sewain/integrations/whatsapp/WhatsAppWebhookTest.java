package app.sewain.integrations.whatsapp;

import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class WhatsAppWebhookTest {
    private static final String SECRET="test-webhook-secret";
    private String sign(String timestamp,byte[] body) throws Exception {
        var mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        mac.update((timestamp+".").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
    @Test void authenticatesExactBodyAndRejectsTamperingStaleOrFutureRequests() throws Exception {
        var body="{\"dataType\":\"ready\"}".getBytes(StandardCharsets.UTF_8);
        var signature=sign("1000",body);
        assertTrue(WhatsAppWebhookController.valid(SECRET,"1000",signature,body,1000));
        assertFalse(WhatsAppWebhookController.valid(SECRET,"1000",signature,new byte[]{1},1000));
        assertFalse(WhatsAppWebhookController.valid(SECRET,"1000",signature,body,1301));
        assertFalse(WhatsAppWebhookController.valid(SECRET,"1000",signature,body,900));
        assertFalse(WhatsAppWebhookController.valid("","1000",signature,body,1000));
        assertFalse(WhatsAppWebhookController.valid(SECRET,"1000","x".repeat(64),body,1000));
    }
}
