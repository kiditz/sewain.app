package app.sewain.catalogs;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SuppressWarnings({"unchecked","rawtypes"})
class InventorySearchContractTest {
 final Root<InventoryUnit> root=mock(Root.class,RETURNS_DEEP_STUBS);final CriteriaQuery query=mock(CriteriaQuery.class);final CriteriaBuilder cb=mock(CriteriaBuilder.class);final Expression<String> lower=mock(Expression.class);
 InventorySearchContractTest(){doReturn(InventoryUnit.class).when(root).getJavaType();when(cb.lower(any(Expression.class))).thenReturn(lower);}
 @Test void statusAndTextUseAndWhileTextAlternativesUseOr(){InventorySpecifications.search(Map.of("status","AVAILABLE","skuBarcode","TENT","serialNumber","SERIAL")).toPredicate(root,query,cb);verify(cb).equal(root.get("status"),InventoryUnitStatus.AVAILABLE);verify(cb).like(lower,"%tent%",'!');verify(cb).like(lower,"%serial%",'!');verify(cb).or(any(Predicate[].class));verify(cb).and(any(Predicate[].class));}
 @Test void statusOnlyAndTextOnlyRemainValid(){InventorySpecifications.search(Map.of("status","MAINTENANCE")).toPredicate(root,query,cb);verify(cb).equal(root.get("status"),InventoryUnitStatus.MAINTENANCE);clearInvocations(cb);InventorySpecifications.search(Map.of("skuBarcode","x")).toPredicate(root,query,cb);verify(cb).like(lower,"%x%",'!');}
 @Test void nullBlankUnknownAndSortOnlyFiltersAreIgnored(){var filters=new HashMap<String,String>();filters.put("skuBarcode",null);filters.put("serialNumber"," ");filters.put("secret","x");filters.put("createdAt","not-a-date");filters.put("updatedAt","not-a-date");assertNull(InventorySpecifications.search(filters).toPredicate(root,query,cb));assertNull(InventorySpecifications.search(null).toPredicate(root,query,cb));assertNull(InventorySpecifications.search(Map.of()).toPredicate(root,query,cb));verifyNoInteractions(cb);}
}
