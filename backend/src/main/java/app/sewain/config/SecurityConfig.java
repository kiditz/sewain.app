package app.sewain.config;

import app.sewain.tenant.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
class SecurityConfig {
	private final CorsConfigurationSource corsConfigurationSource;

	@Bean
	SecurityFilterChain jwtChain(
			HttpSecurity http,
			TenantFilter tenantFilter
	) {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session ->
						session.sessionCreationPolicy(
								SessionCreationPolicy.STATELESS
						)
				)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/register", "/api/auth/login", "/api/merchants/migrate").permitAll()

						// Public storefront endpoints
						.requestMatchers(HttpMethod.GET, "/api/products/slug/**", "/api/products/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/collections").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/storefront/published", "/api/storefront/branding").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/merchants").permitAll()
						.requestMatchers("/api/cart", "/api/cart/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/merchants").permitAll()

						// All other API endpoints require authentication
						.requestMatchers("/api/**").authenticated()

						.anyRequest().permitAll()
				)
				.cors(cors ->
						cors.configurationSource(corsConfigurationSource)
				)
				.oauth2ResourceServer(oauth2 ->
						oauth2.jwt(Customizer.withDefaults())
				)
				.addFilterAfter(
						tenantFilter,
						BearerTokenAuthenticationFilter.class
				)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder
	) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}
}
