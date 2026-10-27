package app.sewain.catalogs;

import java.text.Normalizer;
import java.util.Locale;

final class SlugUtils {

    private SlugUtils() {
    }

    static String slugify(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}