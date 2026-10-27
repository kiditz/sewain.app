package app.sewain.catalogs;

import java.util.UUID;

interface ProductCommandService {

    ProductResponse create(ProductCommand command);

    ProductResponse update(UUID id, ProductCommand command);

    void delete(UUID id);
}