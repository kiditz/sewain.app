package app.sewain.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/** All application HTTP calls use Boot's injected RestClient.Builder. */
@Configuration
class OutboundHttpConfiguration {
    @Bean(destroyMethod="close")
    CloseableHttpClient outboundHttpTransport() {
        var manager=PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(5))
                        .setSocketTimeout(Timeout.ofSeconds(30)).build()).build();
        return HttpClients.custom().setConnectionManager(manager)
                .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(5))
                        .setResponseTimeout(Timeout.ofSeconds(30)).build())
                .disableRedirectHandling().disableAutomaticRetries().disableCookieManagement().disableContentCompression().build();
    }
    @Bean RestClientCustomizer outboundHttpDefaults(CloseableHttpClient outboundHttpTransport) {
        return builder -> builder.requestFactory(new HttpComponentsClientHttpRequestFactory(outboundHttpTransport));
    }
}
