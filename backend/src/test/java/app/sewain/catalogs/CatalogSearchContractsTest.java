package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SuppressWarnings({"unchecked","rawtypes"})
class CatalogSearchContractsTest {
    final Root<Product> root=mock(Root.class,RETURNS_DEEP_STUBS);
    final CriteriaQuery query=mock(CriteriaQuery.class,RETURNS_DEEP_STUBS);
    final CriteriaBuilder cb=mock(CriteriaBuilder.class);
    final Expression<String> lower=mock(Expression.class);
    CatalogSearchContractsTest(){doReturn(Product.class).when(root).getJavaType();var type=root.get("type");doReturn(ProductType.class).when(type).getJavaType();when(cb.lower(any(Expression.class))).thenReturn(lower);}
    @Test void productKeywordAndApprovedScalarFiltersUseExpectedOperators() {
        var id=UUID.randomUUID();ProductSpecifications.search(Map.of("keyword","tent","status","ACTIVE","id",id.toString())).toPredicate(root,query,cb);
        verify(cb,times(2)).like(lower,"%tent%",'!');verify(cb).equal(root.get("status"),ProductStatus.ACTIVE);verify(cb).equal(root.get("id"),id);
    }
    @Test void multipleProductAndTypeIdsUseInAndEmptyUnknownFieldsAreIgnored() {
        var a=UUID.randomUUID();var b=UUID.randomUUID();
        ProductSpecifications.search(Map.of("id",a+","+b,"type.id",a+","+b)).toPredicate(root,query,cb);
        verify(root.get("id")).in(List.of(a,b));verify(root.get("type").get("id")).in(List.of(a,b));
        var empty=new HashMap<String,String>();empty.put("unknown","ignored");empty.put("name",null);empty.put("slug"," ");empty.put("createdAt","ignored");
        assertNull(ProductSpecifications.search(empty).toPredicate(root,query,cb));assertNull(ProductSpecifications.search(null).toPredicate(root,query,cb));
    }
    @Test void priceRangeBuildsInclusiveBoundsAndPriceSortSkipsCountQueries() {
        Subquery<BigDecimal> price=query.subquery(BigDecimal.class);when(price.correlate(root)).thenReturn(root);
        doReturn(Product.class).when(query).getResultType();
        ProductSpecifications.search(Map.of("minimumRentalPrice","10","maximumRentalPrice","20"),List.of(Map.of("key","rentalPrice","order","desc"))).toPredicate(root,query,cb);
        verify(cb).greaterThanOrEqualTo(price,new BigDecimal("10"));verify(cb).lessThanOrEqualTo(price,new BigDecimal("20"));verify(cb).desc(price);
        clearInvocations(cb);doReturn(Long.class).when(query).getResultType();
        ProductSpecifications.search(Map.of(),List.of(Map.of("key","rentalPrice","order","asc"))).toPredicate(root,query,cb);verify(cb,never()).asc(price);
        doReturn(Product.class).when(query).getResultType();ProductSpecifications.search(Map.of(),List.of(Map.of("key","rentalPrice"))).toPredicate(root,query,cb);verify(cb).asc(price);
    }
    @Test void productAvailabilityAndTagsUseExistsSubqueries() {
        var id=UUID.randomUUID();ProductSpecifications.search(Map.of("available","true","tags.id",id.toString())).toPredicate(root,query,cb);
        verify(cb,times(2)).exists(any(Subquery.class));
    }
    @Test void rentalPriceSortIsHandledByQueryAndNormalSortHasIdentityTieBreaker() {
        var q=PageQuery.defaultQuery();q.setSort(List.of(Map.of("key","rentalPrice","order","asc")));
        assertTrue(ProductSpecifications.toSort(q).isUnsorted());q.setSort(List.of(Map.of("key","name","order","desc")));
        assertEquals(List.of("name","id"),ProductSpecifications.toSort(q).stream().map(Sort.Order::getProperty).toList());
    }
    @Test void attributeAndCollectionSearchIgnoreUnknownBlankAndNullInputs() {
        Root<Attribute> attribute=mock(Root.class,RETURNS_DEEP_STUBS);doReturn(Attribute.class).when(attribute).getJavaType();
        Root<Collection> collection=mock(Root.class,RETURNS_DEEP_STUBS);doReturn(Collection.class).when(collection).getJavaType();
        AttributeSpecifications.search(Map.of("name","Capacity","slug","size")).toPredicate(attribute,query,cb);
        CollectionSpecifications.search(Map.of("name","Tents","slug","tents")).toPredicate(collection,query,cb);
        verify(cb).like(lower,"%capacity%",'!');verify(cb).like(lower,"%size%",'!');verify(cb,times(2)).like(lower,"%tents%",'!');
        var empty=new HashMap<String,String>();empty.put("name",null);empty.put("slug"," ");empty.put("unknown","x");
        assertNull(AttributeSpecifications.search(empty).toPredicate(attribute,query,cb));assertNull(CollectionSpecifications.search(empty).toPredicate(collection,query,cb));
        assertNull(AttributeSpecifications.search(null).toPredicate(attribute,query,cb));assertNull(CollectionSpecifications.search(null).toPredicate(collection,query,cb));
        assertNull(AttributeSpecifications.search(Map.of()).toPredicate(attribute,query,cb));assertNull(CollectionSpecifications.search(Map.of()).toPredicate(collection,query,cb));
    }
}
