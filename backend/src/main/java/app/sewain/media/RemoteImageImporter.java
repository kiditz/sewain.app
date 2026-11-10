package app.sewain.media;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.DnsResolver;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.List;

/** Untrusted CSV URLs: validate at socket DNS resolution, with redirects/proxies disabled. */
@Service
@RequiredArgsConstructor
public class RemoteImageImporter {
    private final MediaCommandService media;
    private final RestClient.Builder restClientBuilder;

    public String download(String source) throws IOException {
        URI uri;
        try { uri = URI.create(source); } catch (IllegalArgumentException error) { throw new IOException("URL gambar tidak valid"); }
        if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null ||
                (uri.getPort() != -1 && uri.getPort() != (uri.getScheme().equals("https") ? 443 : 80)))
            throw new IOException("Gunakan URL gambar HTTP/HTTPS publik dengan port standar");
        // Validate literals too: HttpClient may bypass DNS for numeric hosts.
        publicAddresses(uri.getHost());
        var resolver = new DnsResolver() {
            public InetAddress[] resolve(String host) throws UnknownHostException { return publicAddresses(host); }
            public String resolveCanonicalHostname(String host) throws UnknownHostException { publicAddresses(host); return host; }
        };
        var manager = PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(resolver).build();
        var config = RequestConfig.custom().setConnectTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(15)).setConnectionRequestTimeout(Timeout.ofSeconds(5)).build();
        try (var client = HttpClients.custom().setConnectionManager(manager).setDefaultRequestConfig(config)
                .disableRedirectHandling().disableAutomaticRetries().disableContentCompression().disableCookieManagement().build()) {
            var restClient = restClientBuilder.clone().requestFactory(new HttpComponentsClientHttpRequestFactory(client)).build();
            // Whole-request deadline also bounds a server that trickles bytes indefinitely.
            try (var deadline = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()) {
                deadline.schedule(() -> client.close(CloseMode.IMMEDIATE), 30, java.util.concurrent.TimeUnit.SECONDS);
                try { return restClient.get().uri(uri).exchange((request, response) -> {
                    if (response.getStatusCode().value() != 200) throw new IOException("Download gambar gagal (HTTP " + response.getStatusCode().value() + ")");
                    try (var input = response.getBody()) {
                        var bytes = input.readNBytes(5 * 1024 * 1024 + 1);
                        if (bytes.length > 5 * 1024 * 1024) throw new IOException("Gambar melebihi 5 MB");
                        return media.upload(List.of(new Download(bytes)), List.of()).getFirst().url();
                    }
                }); } finally { deadline.shutdownNow(); }
            }
        } catch (org.springframework.web.client.RestClientException error) {
            throw new IOException("Download gambar gagal", error);
        }
    }

    static InetAddress[] publicAddresses(String host) throws UnknownHostException {
        var addresses = InetAddress.getAllByName(host);
        for (var address : addresses) {
            var bytes = address.getAddress();
            boolean blocked = address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress();
            if (bytes.length == 4) {
                int a = bytes[0] & 255, b = bytes[1] & 255;
                blocked |= a == 0 || a == 10 || a == 127 || a >= 224 || (a == 100 && b >= 64 && b <= 127)
                        || (a == 169 && b == 254) || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168)
                        || (a == 192 && b == 0) || (a == 198 && (b == 18 || b == 19 || b == 51)) || (a == 203 && b == 0);
            } else {
                // Only global-unicast IPv6; exclude transition/embedded IPv4 and documentation ranges.
                blocked |= (bytes[0] & 0xe0) != 0x20 || (bytes[0] == 0x20 && bytes[1] == 0x02)
                        || (bytes[0] == 0x20 && bytes[1] == 0x01 && ((bytes[2] & 255) < 0x20));
            }
            if (blocked) throw new UnknownHostException("Alamat gambar harus berada di internet publik");
        }
        return addresses;
    }
    private record Download(byte[] bytes) implements MultipartFile {
        public String getName() { return "file"; }
        public String getOriginalFilename() { return "import-image"; }
        public String getContentType() { return "application/octet-stream"; }
        public boolean isEmpty() { return bytes.length == 0; }
        public long getSize() { return bytes.length; }
        public byte[] getBytes() { return bytes.clone(); }
        public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), bytes); }
    }
}
