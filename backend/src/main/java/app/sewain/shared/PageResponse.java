package app.sewain.shared;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long totalItems
) {
}