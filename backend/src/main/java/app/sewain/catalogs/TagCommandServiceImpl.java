package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class TagCommandServiceImpl implements TagCommandService {

    private final TagRepository repository;
    private final TagMapper mapper;

    @Override
    @Transactional
    public TagResponse create(TagCommand command) {
        Tag entity = mapper.toEntity(command);
        entity.setSlug(generateUniqueSlug(command.name(), null));

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public TagResponse update(UUID id, TagCommand command) {
        Tag entity = findById(id);

        mapper.update(command, entity);
        entity.setSlug(generateUniqueSlug(command.name(), id));

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Tag entity = findById(id);
        repository.delete(entity);
    }

    private Tag findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.TAG_NOT_FOUND
                        )
                );
    }

    private String generateUniqueSlug(String name, UUID currentId) {
        String baseSlug = SlugUtils.slugify(name);

        if (baseSlug.isBlank()) {
            throw new ValidationException(
                    ErrorCode.TAG_NAME_INVALID
            );
        }

        String slug = baseSlug;
        int suffix = 2;

        while (currentId == null
                ? repository.existsBySlug(slug)
                : repository.existsBySlugAndIdNot(slug, currentId)) {
            String ending = "-" + suffix++;
            slug = baseSlug.substring(0, Math.min(baseSlug.length(), 100 - ending.length())) + ending;
        }

        return slug;
    }
}