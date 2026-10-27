package app.sewain.media;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.media")
record MediaProperties(
		String storagePath,
		String baseUrl
) {
}