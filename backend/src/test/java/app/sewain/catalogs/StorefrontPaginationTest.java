package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StorefrontPaginationTest {
	@Test
	void productSearchUsesRequestedPageAndTypesUseOnlyMetadataQuery() throws Exception {
		var products = mock(ProductRepository.class);
		when(products.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
		var type = new ProductType();
		type.setId(UUID.randomUUID());
		type.setName("Tents");
		type.setSlug("tents");
		when(products.findTypesByStatus(ProductStatus.ACTIVE)).thenReturn(List.of(type));
		var service = new ProductQueryServiceImpl(products, mock(ProductMapper.class), Mappers.getMapper(ProductTypeMapper.class));
		service.search(PageQuery.from(Map.of("page", "4", "size", "12", "status", "ACTIVE")));
		verify(products).findAll(any(Specification.class), argThat((Pageable page) -> page.getPageNumber() == 4 && page.getPageSize() == 12 && page.getSort().getOrderFor("id") != null));
		clearInvocations(products);
		MockMvcBuilders.standaloneSetup(new ProductQueryController(service)).build()
				.perform(get("/api/products/types"))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Tents"));
		verify(products).findTypesByStatus(ProductStatus.ACTIVE);
		verify(products, never()).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	void manualCollectionFiltersBeforePaginationAndRetainsDefaultCompatibility() {
		var collections = mock(CollectionRepository.class);
		var items = mock(CollectionItemRepository.class);
		var products = mock(ProductRepository.class);
		var id = UUID.randomUUID();
		var productId = UUID.randomUUID();
		var collection = new Collection();
		collection.setId(id);
		collection.setType(CollectionType.MANUAL);
		when(collections.findById(id)).thenReturn(Optional.of(collection));
		when(items.findProducts(eq(id), anyString(), any(), anyBoolean(), anyCollection(), anyBoolean(), any(Pageable.class))).thenReturn(Page.empty());
		var service = new CollectionQueryServiceImpl(collections, items, products, mock(CollectionMapper.class), mock(ProductMapper.class));
		service.findProducts(id, PageQuery.from(Map.of("page", "2", "size", "3", "status", "ACTIVE", "id", productId.toString(), "available", "true")));
		verify(items).findProducts(eq(id), eq("%"), eq(ProductStatus.ACTIVE), eq(true), eq(List.of(productId)), eq(true),
				argThat((Pageable page) -> page.getPageNumber() == 2 && page.getPageSize() == 3));
		service.findProducts(id, PageQuery.defaultQuery());
		verify(items).findProducts(eq(id), eq("%"), isNull(), eq(false), anyCollection(), eq(false),
				argThat((Pageable page) -> page.getPageNumber() == 0 && page.getPageSize() == 10));
	}

	@Test
	void automatedCollectionAppliesProductFiltersWithRequestedPageSize() {
		var collections = mock(CollectionRepository.class);
		var products = mock(ProductRepository.class);
		var id = UUID.randomUUID();
		var collection = new Collection();
		collection.setId(id);
		collection.setType(CollectionType.AUTOMATED);
		when(collections.findById(id)).thenReturn(Optional.of(collection));
		when(products.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
		var service = new CollectionQueryServiceImpl(collections, mock(CollectionItemRepository.class), products, mock(CollectionMapper.class), mock(ProductMapper.class));
		service.findProducts(id, PageQuery.from(Map.of("page", "1", "size", "8", "status", "ACTIVE", "available", "true")));
		verify(products).findAll(any(Specification.class), argThat((Pageable page) -> page.getPageNumber() == 1 && page.getPageSize() == 8));
	}
}
