package app.sewain.media;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import io.github.borewit.sanitize.SVGSanitizer;

import javax.xml.stream.XMLInputFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/** Delegates SVG sanitization to the library while enforcing our upload limits and format. */
final class SafeSvgUpload {
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    static byte[] normalize(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw invalid();
        try (var input = new ByteArrayInputStream(bytes);
             var output = new ByteArrayOutputStream()) {
            SVGSanitizer.sanitize(input, output);
            if (output.size() > MAX_BYTES) throw invalid();
            var sanitized = output.toByteArray();
            var factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            var reader = factory.createXMLStreamReader(new ByteArrayInputStream(sanitized));
            try {
                reader.nextTag();
                if (!"svg".equals(reader.getLocalName()) ||
                        !"http://www.w3.org/2000/svg".equals(reader.getNamespaceURI())) throw invalid();
            } finally {
                reader.close();
            }
            return sanitized;
        } catch (Exception error) {
            throw invalid();
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(ErrorCode.MEDIA_INVALID_TYPE);
    }
}
