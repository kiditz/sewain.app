package app.sewain.catalogs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface ProductTypeRepository extends JpaRepository<ProductType, UUID>, JpaSpecificationExecutor<ProductType> {

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, UUID id);
}