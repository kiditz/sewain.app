package app.sewain.orders;

import app.sewain.exception.ValidationException;
import app.sewain.shared.PageQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SuppressWarnings({"unchecked","rawtypes"})
class OrderSearchContractTest {
    final Root<RentalOrder> root=mock(Root.class,RETURNS_DEEP_STUBS);
    final CriteriaQuery query=mock(CriteriaQuery.class);
    final CriteriaBuilder cb=mock(CriteriaBuilder.class);
    final Expression<String> lower=mock(Expression.class);
    OrderSearchContractTest(){doReturn(RentalOrder.class).when(root).getJavaType();when(cb.lower(any(Expression.class))).thenReturn(lower);}
    void apply(Map<String,String> filters){MerchantOrderSpecifications.search(filters).toPredicate(root,query,cb);}
    @Test void keywordMatchesNumberNameOrEmailAndCombinesWithStatus() {
        apply(Map.of("keyword"," ORDER_%! ","status","CONFIRMED"));
        verify(root).get("orderNumber");verify(root).get("customerName");verify(root).get("customerEmail");
        verify(cb,times(3)).like(lower,"% order!_!%!! %",'!');
        verify(cb).equal(root.get("status"),OrderStatus.CONFIRMED);verify(cb).or(any(Predicate[].class));verify(cb).and(any(Predicate[].class));
    }
    @Test void emptyAndUnapprovedValuesDoNotConstructPredicates() {
        var filters=new HashMap<String,String>();filters.put("keyword"," ");filters.put("status",null);filters.put("customerName","");filters.put("secret","value");
        assertNull(MerchantOrderSpecifications.search(filters).toPredicate(root,query,cb));
        assertNull(MerchantOrderSpecifications.search(null).toPredicate(root,query,cb));
        assertNull(MerchantOrderSpecifications.search(Map.of()).toPredicate(root,query,cb));verifyNoInteractions(cb);
    }
    @Test void defaultSortUsesCreationDateAndExplicitSortUsesApprovedFields() {
        var q=PageQuery.defaultQuery();assertEquals(Sort.Direction.DESC,MerchantOrderSpecifications.toSort(q).getOrderFor("createdAt").getDirection());
        q.setSort(null);assertNotNull(MerchantOrderSpecifications.toSort(q).getOrderFor("createdAt"));
        q.setSort(List.of());assertNotNull(MerchantOrderSpecifications.toSort(q).getOrderFor("createdAt"));
        q.setSort(List.of(Map.of("key","totalAmount","order","asc")));assertEquals(Sort.Direction.ASC,MerchantOrderSpecifications.toSort(q).getOrderFor("totalAmount").getDirection());
        q.setSort(List.of(Map.of("key","customerEmail","order","asc")));assertThrows(ValidationException.class,()->MerchantOrderSpecifications.toSort(q));
    }
}
