package app.sewain.integrations.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WhatsAppGatewayTest {
    private final RestClient.Builder builder=RestClient.builder();
    private final MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
    private final WhatsAppGateway gateway=new WhatsAppGateway(JsonMapper.builder().build(),builder,"http://gateway.test/","private-test-key");
    @Test void sendsTextAsJsonWithApiKeyToServerSelectedSession() {
        var id=UUID.randomUUID();
        server.expect(requestTo("http://gateway.test/client/sendMessage/"+id)).andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key","private-test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"chatId":"628123456789@c.us","contentType":"string","content":"Hello"}
                        """))
                .andRespond(withSuccess("{\"success\":true}",MediaType.APPLICATION_JSON));
        assertTrue(gateway.send(id,"628123456789@c.us","Hello").path("success").asBoolean());
        server.verify();
    }
    @Test void disconnectUsesDeleteAndDoesNotAutomaticallyRetryFailure() {
        var id=UUID.randomUUID();
        server.expect(requestTo("http://gateway.test/session/disconnect/"+id)).andExpect(method(HttpMethod.DELETE)).andRespond(withServerError());
        var error=assertThrows(ResponseStatusException.class,()->gateway.disconnect(id));
        assertEquals(502,error.getStatusCode().value()); server.verify();
    }
    @Test void rejectsGatewayFailureEvenWithHttp200() {
        var id=UUID.randomUUID();
        server.expect(requestTo("http://gateway.test/session/snapshot/"+id))
                .andRespond(withSuccess("{\"success\":false,\"error\":\"private diagnostics\"}",MediaType.APPLICATION_JSON));
        var error=assertThrows(ResponseStatusException.class,()->gateway.snapshot(id));
        assertEquals(502,error.getStatusCode().value()); assertFalse(error.getMessage().contains("private diagnostics")); server.verify();
    }
    @Test void rejectsOversizedResponseBeforeJsonParsing() {
        var id=UUID.randomUUID();
        server.expect(requestTo("http://gateway.test/session/snapshot/"+id)).andRespond(withSuccess(" ".repeat(65537),MediaType.APPLICATION_JSON));
        assertEquals(502,assertThrows(ResponseStatusException.class,()->gateway.snapshot(id)).getStatusCode().value()); server.verify();
    }
    @Test void missingApiKeyNeverCallsGateway() {
        var unconfigured=new WhatsAppGateway(JsonMapper.builder().build(),builder,"http://gateway.test","");
        assertEquals(503,assertThrows(ResponseStatusException.class,()->unconfigured.connect(UUID.randomUUID())).getStatusCode().value());
        server.verify();
    }
}
