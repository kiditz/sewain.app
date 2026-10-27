package app.sewain.media;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
class MediaWebConfiguration implements WebMvcConfigurer {

    private final MediaProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "file:"
                                + Path.of(properties.storagePath())
                                .toAbsolutePath()
                                .normalize()
                                + "/"
                );
    }
}