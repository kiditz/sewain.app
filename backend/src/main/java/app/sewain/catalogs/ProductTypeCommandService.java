package app.sewain.catalogs;

import java.util.UUID;

interface ProductTypeCommandService {

    ProductTypeResponse create(ProductTypeCommand command);

    ProductTypeResponse update(UUID id, ProductTypeCommand command);

    void delete(UUID id);
}