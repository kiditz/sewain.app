package app.sewain.media;

import org.springframework.core.io.Resource;

record MediaFileResponse(Resource resource, String contentType, long size) {
}
