package app.sewain.catalogs;

import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import app.sewain.catalogs.StorefrontCatalogQueryService.PublicProduct;
import app.sewain.catalogs.StorefrontCatalogQueryService.PublicCollection;

@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
class StorefrontCatalogController {
    private final StorefrontCatalogQueryService queries;
    @GetMapping("/products") PageResponse<PublicProduct> products(@RequestParam Map<String,String> params){return queries.products(params);}
    @GetMapping("/products/types") List<ProductTypeResponse> types(){return queries.types();}
    @GetMapping("/products/{id}") PublicProduct product(@PathVariable UUID id){return queries.product(id);}
    @GetMapping("/products/slug/{slug}") PublicProduct product(@PathVariable String slug){return queries.product(slug);}
    @GetMapping("/collections") List<CollectionsResponse> collections(){return queries.collections();}
    @GetMapping("/collections/{slug}") PublicCollection collection(@PathVariable String slug){return queries.collection(slug);}
    @GetMapping("/collections/{id}/products") PageResponse<PublicProduct> collectionProducts(@PathVariable UUID id,@RequestParam Map<String,String> params){return queries.collectionProducts(id,params);}
}
