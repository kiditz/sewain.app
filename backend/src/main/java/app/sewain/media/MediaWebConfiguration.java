package app.sewain.media;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.CacheControl;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
class MediaWebConfiguration implements WebMvcConfigurer {

    private final MediaProperties properties;

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response, Object handler) {
                // SVG CSS is preserved; browsers must not run scripts or fetch external resources.
                response.setHeader("Content-Security-Policy",
                        "sandbox; default-src 'none'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'");
                response.setHeader("X-Content-Type-Options", "nosniff");
                return true;
            }
        }).addPathPatterns("/media/**", "/api/media/*/file");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(
                        "file:"
                                + Path.of(properties.storagePath())
                                .toAbsolutePath()
                                .normalize()
                                + "/media/"
                )
                .setCacheControl(CacheControl.noCache())
                .resourceChain(false)
                .addResolver(new MediaResourceResolver());
    }
}
