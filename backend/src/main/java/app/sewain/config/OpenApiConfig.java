package app.sewain.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
class OpenApiConfig {
	private static final String BEARER_AUTH = "bearerAuth"; // disesuaikan biar rapi
	private static final String API_KEY_AUTH = "apiKeyAuth";
	private static final String TENANT_HEADER = "tenantHeader"; // Nama key component

	@Bean
	public OpenAPI customOpenAPI() {
		Server server1 = new Server();
		server1.setUrl("https://api.sewain.app");
		server1.setDescription("Production Server");

		Server server2 = new Server();
		server2.setUrl("/");
		server2.setDescription("Default");

		return new OpenAPI()
				.servers(Arrays.asList(server2, server1))
				.components(new Components()
						.addSecuritySchemes(BEARER_AUTH,
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer") // ubah jadi "bearer" string kecil
										.bearerFormat("JWT"))
						.addSecuritySchemes(API_KEY_AUTH,
								new SecurityScheme()
										.type(SecurityScheme.Type.APIKEY)
										.in(SecurityScheme.In.HEADER)
										.name("X-API-KEY"))
						// PERBAIKAN DI SINI: Ubah tipe ke APIKEY agar dikenali sebagai Custom Header
						.addSecuritySchemes(TENANT_HEADER,
								new SecurityScheme()
										.type(SecurityScheme.Type.APIKEY)
										.in(SecurityScheme.In.HEADER)
										.name("X-Tenant")
										.description("Tenant Identifier"))
				)
				.info(new Info()
						.title("Sewain API") // Tambah title biar info API lengkap
						.description("Sewain API Documentation")
						.termsOfService("https://sewain.app/terms/")
						.version("1.0.0")
				)
				// Jika semua endpoint butuh ketiganya secara global, aktifkan baris-baris ini:
				.addSecurityItem(new SecurityRequirement()
						.addList(BEARER_AUTH)
						.addList(TENANT_HEADER)
						.addList(API_KEY_AUTH));
	}
}