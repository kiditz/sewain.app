package app.sewain.catalogs;

import org.springframework.data.jpa.domain.Specification;

class ProductTypeSpecification {

    private ProductTypeSpecification() {
    }

    static Specification<ProductType> search(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String keyword = "%" + search.trim().toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")),
                            keyword
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("slug")),
                            keyword
                    )
            );
        };
    }
}