package app.sewain.catalogs;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ResourceNotFoundException;
import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import app.sewain.shared.ContentSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

/** Public projections deliberately exclude purchase costs and physical inventory identifiers. */
@Service
@Transactional(readOnly=true)
@RequiredArgsConstructor
class StorefrontCatalogQueryService {
    private final ProductQueryService products;
    private final CollectionQueryService collections;

    PageResponse<PublicProduct> products(Map<String,String> params) {
        return publicPage(products.search(publicQuery(params)));
    }
    List<ProductTypeResponse> types() { return products.findTypes(); }
    PublicProduct product(UUID id) { return visible(products.findById(id)); }
    PublicProduct product(String slug) { return visible(products.findBySlug(slug)); }
    List<CollectionsResponse> collections() { return collections.findCategories(); }
    PublicCollection collection(String slug) {
        var collection=collections.findBySlug(slug);
        return new PublicCollection(collection.id(),collection.name(),collection.slug(),collection.imageUrl(),collection.parentId(),collection.description());
    }
    PageResponse<PublicProduct> collectionProducts(UUID id,Map<String,String> params) {
        return publicPage(collections.findProducts(id,publicQuery(params)));
    }
    private PageQuery publicQuery(Map<String,String> params) {
        var filters=new HashMap<>(params);filters.put("status",ProductStatus.ACTIVE.name());
        return PageQuery.from(filters);
    }
    private PageResponse<PublicProduct> publicPage(PageResponse<ProductResponse> page) {
        return new PageResponse<>(page.items().stream().map(this::visible).toList(),page.totalItems());
    }
    private PublicProduct visible(ProductResponse product) {
        if(product.status()!=ProductStatus.ACTIVE)throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
        return new PublicProduct(product.id(),product.type(),product.tags(),product.name(),product.slug(),product.media(),ContentSecurity.cleanHtml(product.description()),product.status(),
            product.variants()==null?List.of():product.variants().stream().filter(ProductVariantResponse::active).map(variant->new PublicVariant(variant.id(),variant.name(),variant.sku(),variant.mediaList(),variant.depositAmount(),variant.active(),variant.values(),
              variant.rentalPlans()==null?List.of():variant.rentalPlans().stream().filter(VariantRentalPlanResponse::active).toList(),
              variant.inventoryUnits()==null?0:(int)variant.inventoryUnits().stream().filter(unit->unit.status()==InventoryUnitStatus.AVAILABLE).count(),variant.createdAt(),variant.updatedAt())).toList(),product.createdAt(),product.updatedAt());
    }
    record PublicCollection(UUID id,String name,String slug,String imageUrl,UUID parentId,String description) {}
    record PublicProduct(UUID id,ProductTypeResponse type,List<TagResponse> tags,String name,String slug,String media,String description,ProductStatus status,List<PublicVariant> variants,LocalDateTime createdAt,LocalDateTime updatedAt) {}
    record PublicVariant(UUID id,String name,String sku,List<String> mediaList,BigDecimal depositAmount,boolean active,List<ProductVariantValueResponse> values,List<VariantRentalPlanResponse> rentalPlans,int inventoryQuantity,LocalDateTime createdAt,LocalDateTime updatedAt) {}
}
