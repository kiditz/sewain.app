package app.sewain.media;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.Arrays;

/** Detect PNG content when a replaced image retains a legacy JPEG/WebP URL. */
class MediaResourceResolver extends PathResourceResolver {
    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        var resource = super.getResource(resourcePath, location);
        if (resource == null) return null;
        try (var input = resource.getInputStream()) {
            var png = new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 };
            if (!Arrays.equals(png, input.readNBytes(8))) return resource;
        }
        return new FileSystemResource(resource.getFile()) {
            @Override
            public String getFilename() {
                var filename = super.getFilename();
                return filename == null ? "image.png" : filename.replaceFirst("\\.[^.]+$", "") + ".png";
            }
        };
    }
}
