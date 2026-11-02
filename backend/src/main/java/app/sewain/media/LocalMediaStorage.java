package app.sewain.media;

import app.sewain.exception.ErrorCode;

import app.sewain.exception.MediaStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

@Component
@RequiredArgsConstructor
class LocalMediaStorage implements MediaStorage {

    private final MediaProperties properties;

    private Path resolve(String objectKey) {
        var root = Path.of(properties.storagePath()).toAbsolutePath().normalize();
        var target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) throw new MediaStorageException("Invalid media storage path");
        return target;
    }

    @Override
    public Resource read(String objectKey) {
        return new FileSystemResource(resolve(objectKey));
    }

    @Override
    public MediaReplacement replace(MultipartFile file, String objectKey) {
        var target = resolve(objectKey);
        Path staged = null;
        Path backup = null;
        try {
            staged = Files.createTempFile(target.getParent(), ".media-replacement-", ".tmp");
            backup = Files.createTempFile(target.getParent(), ".media-backup-", ".tmp");
            Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            file.transferTo(staged);
            move(staged, target);
            var original = backup;
            return committed -> {
                try {
                    if (committed) Files.deleteIfExists(original);
                    else move(original, target);
                } catch (IOException error) {
                    throw new MediaStorageException("Failed to complete media replacement", error);
                }
            };
        } catch (IOException error) {
            if (backup != null) {
                try { Files.deleteIfExists(backup); } catch (IOException cleanup) { error.addSuppressed(cleanup); }
            }
            throw new MediaStorageException("Failed to replace media", error);
        } finally {
            if (staged != null) {
                try { Files.deleteIfExists(staged); } catch (IOException ignored) { /* A completed move removes the staging file. */ }
            }
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException error) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); }
    }

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
