package app.sewain.catalogs;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class StorefrontLookupTest {
 @Test void publicLookupsReturnPublicProjectionAndCategoryMetadata(){var products=mock(ProductQueryService.class);var collections=mock(CollectionQueryService.class);var service=new StorefrontCatalogQueryService(products,collections);var id=UUID.randomUUID();var product=mock(ProductResponse.class);when(product.id()).thenReturn(id);when(product.status()).thenReturn(ProductStatus.ACTIVE);when(product.description()).thenReturn("<p>Public</p>");when(products.findById(id)).thenReturn(product);when(products.findBySlug("tent")).thenReturn(product);assertEquals(id,service.product(id).id());assertEquals(id,service.product("tent").id());var type=mock(ProductTypeResponse.class);when(products.findTypes()).thenReturn(List.of(type));assertEquals(List.of(type),service.types());var summary=mock(CollectionsResponse.class);when(collections.findCategories()).thenReturn(List.of(summary));assertEquals(List.of(summary),service.collections());var detail=mock(CollectionDetailResponse.class);when(detail.id()).thenReturn(id);when(detail.name()).thenReturn("Camping");when(detail.slug()).thenReturn("camping");when(collections.findBySlug("camping")).thenReturn(detail);assertEquals(id,service.collection("camping").id());assertEquals("Camping",service.collection("camping").name());}
}
