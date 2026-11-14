package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InventorySortTest {
    @Test void defaultSortUsesNewestFirstAndStableIdentityTieBreaker() {
        var query=PageQuery.defaultQuery();query.setSort(List.of());var sort=InventorySpecifications.toSort(query);
        assertEquals(Sort.Direction.DESC,sort.getOrderFor("createdAt").getDirection());assertEquals(Sort.Direction.ASC,sort.getOrderFor("id").getDirection());
    }
    @Test void approvedSortsPreserveOrderAndAlwaysAppendIdentity() {
        var query=PageQuery.defaultQuery();query.setSort(List.of(Map.of("key","variant.product.name","order","desc"),Map.of("key","skuBarcode","order","asc")));
        var sort=InventorySpecifications.toSort(query);assertEquals(Sort.Direction.DESC,sort.getOrderFor("variant.product.name").getDirection());
        assertEquals(List.of("variant.product.name","skuBarcode","id"),sort.stream().map(Sort.Order::getProperty).toList());
    }
    @Test void unknownFieldsAreIgnoredAndInvalidDirectionDefaultsToAscending() {
        var query=PageQuery.defaultQuery();query.setSort(List.of(Map.of("key","secret","order","desc")));
        assertNotNull(InventorySpecifications.toSort(query).getOrderFor("createdAt"));
        query.setSort(List.of(Map.of("key","status","order","invalid")));
        assertEquals(Sort.Direction.ASC,InventorySpecifications.toSort(query).getOrderFor("status").getDirection());
    }
}
