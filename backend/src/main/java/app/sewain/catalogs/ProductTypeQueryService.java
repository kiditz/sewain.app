package app.sewain.catalogs;

import java.util.List;
import java.util.UUID;

interface ProductTypeQueryService {

    List<ProductTypeResponse> find(String search);

    ProductTypeResponse findById(UUID id);
}