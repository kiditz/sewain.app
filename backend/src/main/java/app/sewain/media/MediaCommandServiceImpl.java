package app.sewain.media;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import app.sewain.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class MediaCommandServiceImpl implements MediaCommandService {

    private final MediaRepository repository;
    private final MediaStorage storage;
    private final MediaMapper mapper;

    @Override
    @Transactional
    public List<MediaResponse> upload(List<MultipartFile> files) {

        var result = new ArrayList<Media>();
        var tenantSchema = tenantSchemaFolder();

        for (var file : files) {
            validate(file);

            var objectKey = generateObjectKey(file, tenantSchema);

            var stored = storage.store(
                  file,
                  objectKey
            );

            var media = new Media();

            media.setFilename(
                  StringUtils.cleanPath(
                        Objects.requireNonNull(
                              file.getOriginalFilename()
                        )
                  )
            );
            media.setObjectKey(stored.objectKey());
            media.setContentType(file.getContentType());
            media.setSize(file.getSize());
            media.setUrl(stored.url());

            result.add(media);
        }

        return repository.saveAll(result)
              .stream()
              .map(mapper::toResponse)
              .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        var media = repository.findById(id)
              .orElseThrow(() ->
                    new ResourceNotFoundException(
                          ErrorCode.MEDIA_NOT_FOUND
                    )
              );

        storage.delete(media.getObjectKey());
        repository.delete(media);
    }

    @Override
    @Transactional
    public MediaResponse updateFilename(UUID id, UpdateMediaFilenameCommand command) {
        var media = repository.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MEDIA_NOT_FOUND));
        mapper.updateFilename(command, media);
        return mapper.toResponse(repository.save(media));
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException(
                  ErrorCode.MEDIA_EMPTY
            );
        }

        if (!isImage(file)) {
            throw new ValidationException(
                  ErrorCode.MEDIA_INVALID_TYPE
            );
        }
    }

    private boolean isImage(MultipartFile file) {
        return StringUtils.startsWithIgnoreCase(
                file.getContentType(),
                "image/"
        );
    }

    private String generateObjectKey(MultipartFile file, String tenantSchema) {
        var extension = getExtension(file);

        return "media/"
              + tenantSchema
              + "/"
              + LocalDate.now(ZoneId.systemDefault())
              + "/"
              + UUID.randomUUID()
              + extension;
    }

    private String tenantSchemaFolder() {
        var tenantSchema = TenantContext.get();
        return tenantSchema == null || tenantSchema.isBlank() ? "public" : tenantSchema;
    }

    private String getExtension(MultipartFile file) {
        var filename = file.getOriginalFilename();

        if (filename == null) {
            return "";
        }

        var extension = FilenameUtils.getExtension(filename);

        return StringUtils.hasText(extension)
              ? "." + extension.toLowerCase()
              : "";
    }
}
