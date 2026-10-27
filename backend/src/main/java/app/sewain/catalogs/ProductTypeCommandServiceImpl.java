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
class ProductTypeCommandServiceImpl implements ProductTypeCommandService {

    private final ProductTypeRepository repository;
    private final ProductTypeMapper mapper;

    @Override
    @Transactional
    public ProductTypeResponse create(ProductTypeCommand command) {
        ProductType entity = mapper.toEntity(command);
        entity.setSlug(generateUniqueSlug(command.name(), null));

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public ProductTypeResponse update(UUID id, ProductTypeCommand command) {
        ProductType entity = findById(id);

        mapper.update(command, entity);
        entity.setSlug(generateUniqueSlug(command.name(), id));

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ProductType entity = findById(id);
        repository.delete(entity);
    }

    private ProductType findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.PRODUCT_TYPE_NOT_FOUND
                        )
                );
    }

    private String generateUniqueSlug(String name, UUID currentId) {
        String baseSlug = SlugUtils.slugify(name);

        if (baseSlug.isBlank()) {
            throw new ValidationException(
                    ErrorCode.MESSAGE_PRODUCT_TYPE_NAME_MUST_GENERATE_A_VALID_SLUG
            );
        }

        String slug = baseSlug;
        int suffix = 2;

        while (currentId == null
                ? repository.existsBySlug(slug)
                : repository.existsBySlugAndIdNot(slug, currentId)) {
            slug = baseSlug + "-" + suffix++;
        }

        return slug;
    }
}