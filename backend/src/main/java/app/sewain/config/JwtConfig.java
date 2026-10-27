package app.sewain.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@Configuration
class JwtConfig {

	private static final Path PRIVATE_KEY = Path.of("data/jwt-private.pem");
	private static final Path PUBLIC_KEY = Path.of("data/jwt-public.pem");

	@Bean
	KeyPair keyPair() throws Exception {
		if (Files.exists(PRIVATE_KEY) && Files.exists(PUBLIC_KEY)) {
			return loadKeyPair();
		}

		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);

		var keyPair = generator.generateKeyPair();

		saveKeyPair(keyPair);

		return keyPair;
	}

	@Bean
	JwtEncoder jwtEncoder(KeyPair keyPair) {
		var rsaKey = new RSAKey.Builder(
				(RSAPublicKey) keyPair.getPublic()
		)
				.privateKey(keyPair.getPrivate())
				.build();

		JWKSource<SecurityContext> jwkSource =
				(selector, context) ->
						selector.select(new JWKSet(rsaKey));

		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	JwtDecoder jwtDecoder(KeyPair keyPair) {
		return NimbusJwtDecoder
				.withPublicKey((RSAPublicKey) keyPair.getPublic())
				.build();
	}

	private KeyPair loadKeyPair() throws Exception {
		var keyFactory = KeyFactory.getInstance("RSA");

		var privateKey = keyFactory.generatePrivate(
				new PKCS8EncodedKeySpec(
						decodePem(Files.readString(PRIVATE_KEY), "PRIVATE KEY")
				)
		);

		var publicKey = keyFactory.generatePublic(
				new X509EncodedKeySpec(
						decodePem(Files.readString(PUBLIC_KEY), "PUBLIC KEY")
				)
		);

		return new KeyPair(publicKey, privateKey);
	}

	private void saveKeyPair(KeyPair keyPair) throws Exception {
		Files.createDirectories(PRIVATE_KEY.getParent());

		Files.writeString(
				PRIVATE_KEY,
				encodePem(
						"PRIVATE KEY",
						keyPair.getPrivate().getEncoded()
				)
		);

		Files.writeString(
				PUBLIC_KEY,
				encodePem(
						"PUBLIC KEY",
						keyPair.getPublic().getEncoded()
				)
		);
	}

	private byte[] decodePem(String pem, String type) {
		return Base64.getDecoder().decode(
				pem
						.replace("-----BEGIN " + type + "-----", "")
						.replace("-----END " + type + "-----", "")
						.replaceAll("\\s", "")
		);
	}

	private String encodePem(String type, byte[] encoded) {
		var base64 = Base64.getMimeEncoder(64, "\n".getBytes())
				.encodeToString(encoded);

		return """
                -----BEGIN %s-----
                %s
                -----END %s-----
                """.formatted(type, base64, type);
	}
}