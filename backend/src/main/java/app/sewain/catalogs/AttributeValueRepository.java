package app.sewain.catalogs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AttributeValueRepository
        extends JpaRepository<AttributeValue, UUID> {

    boolean existsByAttributeIdAndSlug(
            UUID attributeId,
            String slug
    );

    boolean existsByAttributeIdAndSlugAndIdNot(
            UUID attributeId,
            String slug,
            UUID id
    );
}