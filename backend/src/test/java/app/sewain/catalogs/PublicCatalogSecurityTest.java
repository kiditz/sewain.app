package app.sewain.catalogs;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicCatalogSecurityTest {
	ProductResponse product(ProductStatus status) {
		var id = UUID.randomUUID();
		var unit = new InventoryUnitResponse(UUID.randomUUID(), "PRIVATE-BARCODE", "PRIVATE-SERIAL", InventoryUnitStatus.AVAILABLE, "PRIVATE-NOTES", null, null);
		var variant = new ProductVariantResponse(UUID.randomUUID(), "Standard", "PUBLIC-SKU", List.of(), new BigDecimal("987654"), BigDecimal.TEN, true, List.of(), List.of(), List.of(unit), null, null);
		return new ProductResponse(id, null, List.of(), "Public product", "public-product", "", "<p onclick='x()'>Safe</p>", status, List.of(variant), null, null);
	}

	@Test
	void forcesActiveOnProductAndCollectionQueriesAndSerializesOnlyPublicFields() {
		var products = mock(ProductQueryService.class);
		var collections = mock(CollectionQueryService.class);
		var source = product(ProductStatus.ACTIVE);
		when(products.search(any())).thenReturn(new PageResponse<>(List.of(source), 1));
		when(collections.findProducts(any(), any())).thenReturn(new PageResponse<>(List.of(source), 1));
		var controller = new StorefrontCatalogQueryService(products, collections);
		var response = controller.products(Map.of("status", "DRAFT", "size", "2"));
		verify(products).search(argThat(query -> "ACTIVE".equals(query.getSearch().get("status")) && query.getSize() == 2));
		controller.collectionProducts(UUID.randomUUID(), Map.of("status", "UNLISTED"));
		verify(collections).findProducts(any(), argThat(query -> "ACTIVE".equals(query.getSearch().get("status"))));
		var json = JsonMapper.builder().build().writeValueAsString(response);
		for (var secret : List.of("buyPrice", "inventoryUnits", "PRIVATE-BARCODE", "PRIVATE-SERIAL", "PRIVATE-NOTES", "987654", "onclick"))
			assertFalse(json.contains(secret), secret);
		assertTrue(json.contains("inventoryQuantity"));
		assertEquals(1, response.items().get(0).variants().get(0).inventoryQuantity());
	}

	@Test
	void hidesDraftAndUnlistedDetailThroughBothLookupRoutes() {
		var products = mock(ProductQueryService.class);
		var controller = new StorefrontCatalogQueryService(products, mock(CollectionQueryService.class));
		for (var status : List.of(ProductStatus.DRAFT, ProductStatus.UNLISTED)) {
			var source = product(status);
			when(products.findById(source.id())).thenReturn(source);
			when(products.findBySlug(source.slug())).thenReturn(source);
			assertThrows(app.sewain.exception.ResourceNotFoundException.class, () -> controller.product(source.id()));
			assertThrows(app.sewain.exception.ResourceNotFoundException.class, () -> controller.product(source.slug()));
		}
	}
}
