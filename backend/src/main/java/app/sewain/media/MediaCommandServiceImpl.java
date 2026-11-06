package app.sewain.media;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import app.sewain.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.MediaType;
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
    public List<MediaResponse> upload(List<MultipartFile> files, List<Integer> backgroundRemoved) {

        var result = new ArrayList<Media>();
        var tenantSchema = tenantSchemaFolder();
        var processed = new java.util.HashSet<>(backgroundRemoved == null ? List.<Integer>of() : backgroundRemoved);
        for (var index : processed) {
            if (index == null || index < 0 || index >= files.size()) throw new ValidationException(ErrorCode.MEDIA_INVALID_TYPE);

        }

        var normalized = new ArrayList<MultipartFile>();
        long totalBytes=0;
        for(int index=0;index<files.size();index++){
            var file=SafeImageUpload.normalize(files.get(index),processed.contains(index));
            totalBytes+=file.getSize();
            if(totalBytes>20L*1024*1024)throw new ValidationException(ErrorCode.MEDIA_INVALID_TYPE);
            normalized.add(file);
        }
        for (int index = 0; index < normalized.size(); index++) {
            var file = normalized.get(index);

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
            media.setBackgroundRemoved(processed.contains(index));

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
        var media = repository.findByIdForUpdate(id)
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
        var media = repository.findByIdForUpdate(id)
              .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MEDIA_NOT_FOUND));
        mapper.updateFilename(command, media);
        return mapper.toResponse(repository.save(media));
    }

    @Override
    @Transactional
    public MediaResponse replaceFile(UUID id, MultipartFile file) {
        var media = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MEDIA_NOT_FOUND));
        if (media.isBackgroundRemoved()) return mapper.toResponse(media);
        file=SafeImageUpload.normalize(file,true);
        var replacement = storage.replace(file, media.getObjectKey());
        boolean registered = false;
        try {
            media.setBackgroundRemoved(true);
            media.setContentType(MediaType.IMAGE_PNG_VALUE);
            media.setSize(file.getSize());
            var filename = media.getFilename().replaceFirst("\\.[^.]+$", "");
            media.setFilename(filename.substring(0, Math.min(filename.length(), 251)) + ".png");
            var response = mapper.toResponse(repository.saveAndFlush(media));
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        replacement.complete(status == STATUS_COMMITTED);
                    }
                });
            } else {
                replacement.complete(true);
            }
            registered = true;
            return response;
        } finally {
            if (!registered) replacement.complete(false);
        }
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
