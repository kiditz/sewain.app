package app.sewain.media;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

interface MediaCommandService {

    List<MediaResponse> upload(List<MultipartFile> files);

    void delete(UUID id);

    MediaResponse updateFilename(UUID id, UpdateMediaFilenameCommand command);
}
