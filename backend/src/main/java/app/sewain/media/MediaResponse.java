package app.sewain.media;

import java.time.LocalDateTime;
import java.util.UUID;

record MediaResponse(
      UUID id,
      String filename,
      String contentType,
      Long size,
      String url,
      LocalDateTime createdAt
) {
}
