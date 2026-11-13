package app.sewain.merchants;

import app.sewain.SewainApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.simp.stomp.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = SewainApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.docker.compose.enabled=false", "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=never", "spring.jpa.hibernate.ddl-auto=none",
                "spring.datasource.hikari.maximum-pool-size=4", "app.maintenance.migrate-tenants=false",
                "app.websocket.allowed-origin-patterns=http://sewain.app.localhost:3000",
                "app.integrations.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "logging.level.org.springframework.security=WARN"
        })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BackendStartupIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("sewain_startup").withUsername("sewain_test").withPassword("integration_test_only");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET TIME ZONE 'UTC'");
        properties.add("app.media.storage-path", () -> "build/integration-test-media/" + POSTGRES.getContainerId());
    }

    // Keep JWT signing/verification real, but never read or generate development key files.
    @TestBean(name = "keyPair", methodName = "testKeyPair", enforceOverride = true)
    KeyPair keyPair;

    static KeyPair testKeyPair() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @Autowired ObjectMapper mapper;
    @Autowired MerchantSchemaService schemas;
    @Autowired MerchantProvisioningService provisioning;
    @Value("${local.server.port}") int port;
    private UUID owner;
    private Store first;
    private Store second;

    @BeforeEach
    void seedMerchants() {
        owner = user();
        first = store(owner);
        second = store(user());
    }

    @Test
    void applicationStartsWithFlywayRepositoriesAndUsableTenantDefaults() {
        assertEquals("UTC", jdbc.queryForObject("SHOW TIME ZONE", String.class));
        assertTrue(jdbc.queryForObject("SELECT count(*) > 0 FROM public.flyway_schema_history", Boolean.class));
        assertNotNull(jdbc.queryForObject("SELECT to_regclass('public.batch_job_instance')::text", String.class));
        assertEquals("READY", provisioning.snapshot(first.id(), owner).status());
        assertEquals(5L, jdbc.queryForObject("SELECT count(*) FROM " + first.schema() + ".settings", Long.class));
    }

    @Test
    void provisioningPersistsReadyStatusAndVersionAfterRealMigration() {
        jdbc.update("UPDATE public.merchants SET provisioning_status='PROVISIONING' WHERE id=?", first.id());
        var previous = provisioning.snapshot(first.id(), owner);

        var result = provisioning.process(first.schema());

        assertEquals("READY", result.status().status());
        var persisted = provisioning.snapshot(first.id(), owner);
        assertEquals(result.status(), persisted);
        assertTrue(persisted.version() > previous.version());
    }

    @Test
    void migrationValidationFailurePersistsFailedStatusInsteadOfLeavingProvisioningStuck() {
        jdbc.update("UPDATE public.merchants SET provisioning_status='PROVISIONING' WHERE id=?", first.id());
        jdbc.update("UPDATE " + first.schema() + ".flyway_schema_history SET checksum=checksum+1 WHERE version='37'");

        var result = provisioning.process(first.schema());

        assertEquals("FAILED", result.status().status());
        assertEquals(result.status(), provisioning.snapshot(first.id(), owner));
    }

    @Test
    void signedJwtCannotReadOrDeleteAnotherTenantsKnownProductId() throws Exception {
        var ownProduct = product(first.schema());
        var foreignProduct = product(second.schema());
        var authorization = "Bearer " + token(owner);

        mvc.perform(get("/api/products/{id}", ownProduct).header("Authorization", authorization)
                .header("X-Tenant", first.slug())).andExpect(status().isOk());
        mvc.perform(get("/api/products/{id}", foreignProduct).header("Authorization", authorization)
                .header("X-Tenant", first.slug())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/products/{id}", foreignProduct).header("Authorization", authorization)
                .header("X-Tenant", first.slug())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/{id}", foreignProduct).header("Authorization", authorization)
                .header("X-Tenant", second.slug())).andExpect(status().isForbidden());

        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM " + second.schema() + ".products WHERE id=?",
                Long.class, foreignProduct));
    }

    @Test
    void protectedApiRequiresAuthenticationAndIncompleteTenantReturnsUnavailable() throws Exception {
        mvc.perform(get("/api/products").header("X-Tenant", first.slug())).andExpect(status().isUnauthorized());
        jdbc.update("UPDATE public.merchants SET provisioning_status='PROVISIONING' WHERE id=?", first.id());
        mvc.perform(get("/api/settings/REGIONAL").header("Authorization", "Bearer " + token(owner))
                .header("X-Tenant", first.slug())).andExpect(status().isServiceUnavailable());
    }

    @Test
    void reconnectingStompClientRetrievesPersistedStatusAndCannotRequestAnotherOwnersMerchant() throws Exception {
        var client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new SimpleMessageConverter());
        client.start();
        try {
            for (int connection = 0; connection < 2; connection++) {
                var headers = new StompHeaders();
                headers.add("Authorization", "Bearer " + token(owner));
                var http = new WebSocketHttpHeaders();
                http.setOrigin("http://sewain.app.localhost:3000");
                var reply = new CompletableFuture<JsonNode>();
                var session = client.connectAsync("ws://localhost:" + port + "/ws/provisioning", http, headers,
                        new StompSessionHandlerAdapter() {
                            @Override public void handleTransportError(StompSession session, Throwable error) {
                                reply.completeExceptionally(error);
                            }
                        }).get(15, TimeUnit.SECONDS);
                try {
                    var subscription = session.subscribe("/user/queue/provisioning", frameHandler(reply));
                    requestStatus(session, first.id());
                    var status = reply.get(15, TimeUnit.SECONDS);
                    assertEquals(first.id().toString(), status.path("merchantId").asString());
                    assertEquals("READY", status.path("status").asString());
                    assertEquals(provisioning.snapshot(first.id(), owner).version(), status.path("version").asLong());
                    subscription.unsubscribe();

                    var denied = new CompletableFuture<JsonNode>();
                    session.subscribe("/user/queue/provisioning", frameHandler(denied));
                    requestStatus(session, second.id());
                    var failure = denied.get(15, TimeUnit.SECONDS);
                    assertEquals("provisioning.requestFailed", failure.path("error").asString());
                    assertFalse(failure.has("merchantId"));
                } finally {
                    if (session.isConnected()) session.disconnect();
                }
            }
        } finally {
            client.stop();
        }
    }

    private StompFrameHandler frameHandler(CompletableFuture<JsonNode> response) {
        return new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return byte[].class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                try { response.complete(mapper.readTree((byte[]) payload)); }
                catch (Exception error) { response.completeExceptionally(error); }
            }
        };
    }

    private void requestStatus(StompSession session, UUID merchant) {
        var headers = new StompHeaders();
        headers.setDestination("/app/provisioning/status");
        headers.setContentType(MediaType.APPLICATION_JSON);
        session.send(headers, ("{\"merchantId\":\"" + merchant + "\"}").getBytes(StandardCharsets.UTF_8));
    }

    private String token(UUID user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(user.toString()).issuedAt(now)
                .expiresAt(now.plusSeconds(300)).claim("user", user.toString()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    private UUID user() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO public.users(id,email,password,full_name) VALUES (?,?,?,?)",
                id, id + "@example.invalid", "unused-integration-password", "Integration Owner");
        return id;
    }

    private Store store(UUID user) {
        var id = UUID.randomUUID();
        var slug = "it-" + id.toString().replace("-", "");
        var schema = slug.replace('-', '_');
        schemas.createTenantSchema(schema);
        jdbc.update("INSERT INTO public.merchants(id,user_id,name,slug,\"schema\",provisioning_status) VALUES (?,?,?,?,?,'READY')",
                id, user, "Integration Store", slug, schema);
        jdbc.update("INSERT INTO public.merchant_users(merchant_id,user_id,role) VALUES (?,?,'OWNER')", id, user);
        return new Store(id, slug, schema);
    }

    private UUID product(String schema) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO " + schema + ".products(id,name,slug,status) VALUES (?,?,?,'ACTIVE')", id, "Test Tent", "tent-" + id);
        return id;
    }

    private record Store(UUID id, String slug, String schema) { }
}
