package app.sewain.media;

import app.sewain.exception.ErrorCode;

import app.sewain.exception.MediaStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
class LocalMediaStorage implements MediaStorage {

    private final MediaProperties properties;

    @Override
    public StoredMedia store(
            MultipartFile file,
            String objectKey
    ) {
        var path = Path.of(properties.storagePath())
                .resolve(objectKey)
                .normalize();

        try {
            Files.createDirectories(path.getParent());

            file.transferTo(path);

            return new StoredMedia(
                    objectKey,
                    getUrl(objectKey)
            );
        } catch (IOException e) {
            throw new MediaStorageException(
                    ErrorCode.MESSAGE_FAILED_TO_STORE_MEDIA + objectKey,
                    e
            );
        }
    }

    @Override
    public String getUrl(String objectKey) {
        return UriComponentsBuilder
                .fromUriString(properties.baseUrl())
                .pathSegment(
                        objectKey.split("/")
                )
                .build()
                .toUriString();
    }

    @Override
    public void delete(String objectKey) {
        var path = Path.of(properties.storagePath())
                .resolve(objectKey)
                .normalize();

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new MediaStorageException(
                    ErrorCode.MESSAGE_FAILED_TO_DELETE_MEDIA + objectKey,
                    e
            );
        }
    }
}