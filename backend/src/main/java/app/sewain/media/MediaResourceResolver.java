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
        var extension=org.springframework.util.StringUtils.getFilenameExtension(resourcePath);
        boolean rasterExtension=extension!=null&&java.util.Set.of("png","jpg","jpeg","webp","gif").contains(extension.toLowerCase(java.util.Locale.ROOT));
        // Replacement preserves the old URL, including a former SVG URL.
        // Inspect actual PNG bytes before dispatching extension-specific validation.
        boolean pngContent;
        try (var input = resource.getInputStream()) {
            var png = new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 };
            pngContent = Arrays.equals(png, input.readNBytes(8));
        }
        if (!pngContent) {
            if ("svg".equalsIgnoreCase(extension)) {
                try (var input = resource.getInputStream()) {
                    var bytes = SafeSvgUpload.normalize(input.readNBytes(5 * 1024 * 1024 + 1));
                    return new org.springframework.core.io.ByteArrayResource(bytes) {
                        @Override public String getFilename() { return resource.getFilename(); }
                        @Override public long lastModified() throws IOException { return resource.lastModified(); }
                    };
                } catch (app.sewain.exception.ValidationException error) {
                    return null;
                }
            }
            return rasterExtension ? resource : null;
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
