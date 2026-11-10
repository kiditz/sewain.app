# Backend outbound HTTP standard

Use the injected Spring `RestClient.Builder` for application HTTP calls. Clone the builder for each integration and configure its base URL and credentials on that private clone. Do not construct RestTemplate, JDK HttpClient, or execute Apache HTTP calls directly in business services. SMTP, IMAP and WebSocket traffic are separate protocols.

`OutboundHttpConfiguration` applies application-wide defaults through Boot's `RestClientCustomizer`: 5-second connection and pool-acquisition timeouts, 30-second response/socket timeouts, no redirects, no automatic retries, no cookies, no transparent content decompression. The managed Apache transport closes at application shutdown. This is the HTTP transport underneath RestClient, not another application-level API. Keep credentials and tenant headers out of global defaults.

WhatsAppGateway uses its own cloned RestClient with the configured base URL/API key. It streams and bounds JSON responses to 64 KB and checks both HTTP status and the gateway success flag. HTTP errors become sanitized gateway errors. RestClient `exchange` is intentional here because automatic body conversion would lose the explicit streaming size bound.

RemoteImageImporter also uses RestClient, with an isolated per-download transport overriding the request factory. This preserves socket-time public-IP DNS validation, literal-IP checks, no redirects/proxies/retries/cookies, a 5 MB body limit, 15-second response timeout, and a 30-second whole-download deadline that immediately closes that isolated transport. Its URL policy must not be weakened by using the general integration transport.

No Gradle build/test was run for this change. Verify manually from `backend`:

```sh
./gradlew test --tests app.sewain.integrations.whatsapp.WhatsAppGatewayTest --tests app.sewain.media.RemoteImageImporterTest
```

References:
- https://docs.spring.io/spring-boot/api/java/org/springframework/boot/restclient/RestClientCustomizer.html
- https://docs.spring.io/spring-framework/docs/7.0.1/javadoc-api/org/springframework/http/client/HttpComponentsClientHttpRequestFactory.html
