package app.sewain.customers;
import app.sewain.shared.PageQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SuppressWarnings({"unchecked","rawtypes"})
class CustomerSearchContractTest {
 final Root<Customer> root=mock(Root.class,RETURNS_DEEP_STUBS);final CriteriaQuery query=mock(CriteriaQuery.class);final CriteriaBuilder cb=mock(CriteriaBuilder.class);final Expression<String> lower=mock(Expression.class);
 CustomerSearchContractTest(){doReturn(Customer.class).when(root).getJavaType();when(cb.lower(any(Expression.class))).thenReturn(lower);}
 @Test void keywordSearchesThreeIdentityFieldsAndStatusAliasesBecomeBooleans(){CustomerSpecifications.search(Map.of("keyword","Owner","enabled","ACTIVE")).toPredicate(root,query,cb);verify(cb,times(3)).like(lower,"%owner%",'!');verify(cb).equal(root.get("enabled"),true);clearInvocations(cb);CustomerSpecifications.search(Map.of("enabled","INACTIVE")).toPredicate(root,query,cb);verify(cb).equal(root.get("enabled"),false);clearInvocations(cb);CustomerSpecifications.search(Map.of("enabled","true")).toPredicate(root,query,cb);verify(cb).equal(root.get("enabled"),true);}
 @Test void emptyAndBlankFiltersAreUnrestricted(){for(var values:Arrays.asList(null,Map.<String,String>of(),Map.of("keyword"," ","enabled"," "),Map.of("unknown","x")))assertNull(CustomerSpecifications.search(values).toPredicate(root,query,cb));verifyNoInteractions(cb);}
 @Test void sortDefaultsToNewestAndSupportsApprovedIdentityFields(){var q=PageQuery.defaultQuery();assertNotNull(CustomerSpecifications.toSort(q).getOrderFor("createdAt"));q.setSort(List.of());assertNotNull(CustomerSpecifications.toSort(q).getOrderFor("createdAt"));q.setSort(List.of(Map.of("key","email","order","asc")));assertEquals(Sort.Direction.ASC,CustomerSpecifications.toSort(q).getOrderFor("email").getDirection());}
}
