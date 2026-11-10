package app.sewain.config;

import app.sewain.tenant.TenantFilter;
import app.sewain.tenant.TenantAccess;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiSecurityBoundaryTest {
	AnnotationConfigWebApplicationContext context;
	MockMvc mvc;

	@BeforeEach
	void setup() {
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(WebConfig.class);
		context.refresh();
		mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
	}

	@AfterEach
	void close() {
		context.close();
	}

	@Test
	void privateCatalogNeedsAuthenticationAndMigrationIsNeverHttpAccessible() throws Exception {
		mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/products/slug/draft")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/products").header("Authorization", "Bearer valid")).andExpect(status().isOk());
		mvc.perform(post("/api/merchants/migrate")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/merchants/migrate").header("Authorization", "Bearer valid")).andExpect(status().isForbidden());
	}

	@Test
	void publicCatalogAndGuestCartRemainAnonymous() throws Exception {
		mvc.perform(get("/api/storefront/products")).andExpect(status().isOk());
		mvc.perform(get("/api/storefront/collections/id/products")).andExpect(status().isOk());
		mvc.perform(get("/api/cart")).andExpect(status().isOk());
	}

	@Configuration
	@EnableWebMvc
	@Import({SecurityConfig.class, CorsConfig.class})
	static class WebConfig {
		@Bean
		Probe probe() {
			return new Probe();
		}

		@Bean
		UserDetailsService users() {
			return mock(UserDetailsService.class);
		}

		@Bean
		JwtDecoder jwtDecoder() {
			var decoder = mock(JwtDecoder.class);
			when(decoder.decode("valid")).thenReturn(Jwt.withTokenValue("valid").header("alg", "RS256").subject("security-test").claim("user", UUID.randomUUID().toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build());
			return decoder;
		}

		@Bean
		TenantFilter tenantFilter() {
			var access = mock(TenantAccess.class);
			when(access.getSchema(anyString())).thenReturn("public");
			return new TenantFilter(access);
		}
	}

	@RestController
	static class Probe {
		@GetMapping({"/api/products", "/api/products/slug/draft", "/api/storefront/products", "/api/storefront/collections/id/products", "/api/cart"})
		Map<String, Boolean> get() {
			return Map.of("ok", true);
		}

		@PostMapping("/api/merchants/migrate")
		void migrate() {
			throw new AssertionError("Migration must never reach a controller");
		}
	}
}
