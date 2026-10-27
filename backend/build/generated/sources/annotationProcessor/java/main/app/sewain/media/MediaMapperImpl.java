package app.sewain.media;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-26T13:54:45+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class MediaMapperImpl implements MediaMapper {

    @Override
    public MediaResponse toResponse(Media media) {
        if ( media == null ) {
            return null;
        }

        UUID id = null;
        String filename = null;
        String contentType = null;
        Long size = null;
        String url = null;
        LocalDateTime createdAt = null;

        id = media.getId();
        filename = media.getFilename();
        contentType = media.getContentType();
        size = media.getSize();
        url = media.getUrl();
        createdAt = media.getCreatedAt();

        MediaResponse mediaResponse = new MediaResponse( id, filename, contentType, size, url, createdAt );

        return mediaResponse;
    }

    @Override
    public void updateFilename(UpdateMediaFilenameCommand command, Media media) {
        if ( command == null ) {
            return;
        }

        media.setFilename( command.filename() );
    }
}
