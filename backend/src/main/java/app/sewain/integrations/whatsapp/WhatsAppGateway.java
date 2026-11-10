package app.sewain.integrations.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.web.client.RestClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.util.UUID;

@Component
class WhatsAppGateway {
	private final String key;
	private final ObjectMapper mapper;
	private final RestClient client;

	WhatsAppGateway(ObjectMapper mapper, RestClient.Builder builder, @Value("${app.integrations.whatsapp.base-url:http://localhost:3001}") String base,
	                @Value("${app.integrations.whatsapp.api-key:}") String key) {
		this.mapper = mapper;
		this.client = builder.clone().baseUrl(base.replaceAll("/+$", "")).defaultHeader("x-api-key", key).build();
		this.key = key;
	}

	boolean configured() {
		return !key.isBlank();
	}

	JsonNode snapshot(UUID id) {
		return request("GET", "/session/snapshot/" + id, null);
	}

	void connect(UUID id) {
		request("POST", "/session/connect/" + id, null);
	}

	void disconnect(UUID id) {
		request("DELETE", "/session/disconnect/" + id, null);
	}

	JsonNode send(UUID id, String peer, String body) {
		var payload = mapper.createObjectNode();
		payload.put("chatId", peer);
		payload.put("contentType", "string");
		payload.put("content", body);
		return request("POST", "/client/sendMessage/" + id, payload.toString());
	}

	private JsonNode request(String method, String path, String body) {
		if (!configured())
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WhatsApp gateway belum dikonfigurasi");
		try {
            var request=client.method(HttpMethod.valueOf(method)).uri(path);
            if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body);
            // Streaming exchange retains the response limit even for chunked/malformed responses.
            return request.exchange((httpRequest,response) -> {
                var bytes=response.getBody().readNBytes(65537);
                if (bytes.length>65536 || !response.getStatusCode().is2xxSuccessful()) throw new IllegalStateException();
                var result=mapper.readTree(bytes);
                if (!result.path("success").asBoolean(false)) throw new IllegalStateException();
                return result;
            });
		} catch (Exception error) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WhatsApp gateway tidak tersedia atau belum siap");
		}
	}
}
