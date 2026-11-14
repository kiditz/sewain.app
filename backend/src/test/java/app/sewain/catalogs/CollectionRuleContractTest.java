package app.sewain.catalogs;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Unit contracts for query construction; database result semantics have integration coverage. */
@SuppressWarnings({"unchecked","rawtypes"})
class CollectionRuleContractTest {
    final Root<Product> root=mock(Root.class,RETURNS_DEEP_STUBS);
    final CriteriaQuery<Product> query=mock(CriteriaQuery.class);
    final CriteriaBuilder cb=mock(CriteriaBuilder.class);
    final Expression<String> lowered=mock(Expression.class);
    final Predicate positive=mock(Predicate.class);
    CollectionRuleContractTest() {
        when(cb.lower(any(Expression.class))).thenReturn(lowered);
        when(cb.or(any(Predicate[].class))).thenReturn(positive);
    }
    Collection collection(CollectionRuleAttribute attribute,CollectionRuleOperator operator,String... values) {
        var c=new Collection();c.setMatchCondition(CollectionMatchCondition.ALL);
        var rule=new CollectionRule();rule.setAttribute(attribute);rule.setOperator(operator);for(var value:values)rule.addValue(value);
        c.getRules().add(rule);return c;
    }
    void apply(Collection c){CollectionRuleSpecifications.matching(c).toPredicate(root,query,cb);}
    @Test void titleComparisonNormalizesCaseAndEscapesEveryLikeOperator() {
        apply(collection(CollectionRuleAttribute.TITLE,CollectionRuleOperator.EQUALS," TENT "));verify(cb).equal(lowered,"tent");
        var operators=new CollectionRuleOperator[]{CollectionRuleOperator.CONTAINS,CollectionRuleOperator.STARTS_WITH,CollectionRuleOperator.ENDS_WITH};
        var patterns=new String[]{"%a!%!_!!%","a!%!_!!%","%a!%!_!!"};
        for(int i=0;i<operators.length;i++) {
            clearInvocations(cb);apply(collection(CollectionRuleAttribute.TITLE,operators[i]," A%_! "));verify(cb).like(lowered,patterns[i],'!');
        }
    }
    @Test void negativeRulesNegateTheWholeValueGroup() {
        apply(collection(CollectionRuleAttribute.TITLE,CollectionRuleOperator.NOT_EQUALS,"Tent","Stove"));
        verify(cb).equal(lowered,"tent");verify(cb).equal(lowered,"stove");verify(cb).not(positive);
        clearInvocations(cb);apply(collection(CollectionRuleAttribute.TITLE,CollectionRuleOperator.NOT_CONTAINS,"Bag"));
        verify(cb).like(lowered,"%bag%",'!');verify(cb).not(positive);
    }
    @Test void typeUsesLeftJoinAndCoalescesMissingTypeToEmptyString() {
        when(cb.coalesce(any(Expression.class),eq(""))).thenReturn(lowered);
        apply(collection(CollectionRuleAttribute.TYPE,CollectionRuleOperator.EQUALS,"Camping"));
        verify(root).join("type",JoinType.LEFT);verify(cb).coalesce(any(Expression.class),eq(""));verify(cb).equal(lowered,"camping");
    }
    @Test void multiValuedAttributesUseCorrelatedExistsInsteadOfDuplicatingRootRows() {
        for(var attribute:new CollectionRuleAttribute[]{CollectionRuleAttribute.TAG,CollectionRuleAttribute.VARIANT_TITLE,CollectionRuleAttribute.ATTRIBUTE_VALUE}) {
            Subquery<Integer> sub=mock(Subquery.class);when(query.subquery(Integer.class)).thenReturn(sub);
            when(sub.correlate(root)).thenReturn(root);when(sub.select(any())).thenReturn(sub);when(sub.where(any(Predicate.class))).thenReturn(sub);
            when(cb.exists(sub)).thenReturn(positive);
            apply(collection(attribute,CollectionRuleOperator.EQUALS,"Camping"));
            verify(sub).correlate(root);verify(sub).select(any());verify(sub).where(positive);verify(cb).exists(sub);
        }
    }
    @Test void allAndAnyDetermineOuterGroupingAndSnapshotSurvivesEntityMutation() {
        var c=collection(CollectionRuleAttribute.TITLE,CollectionRuleOperator.EQUALS,"Tent");
        var spec=CollectionRuleSpecifications.matching(c);c.getRules().clear();spec.toPredicate(root,query,cb);
        verify(cb).equal(lowered,"tent");verify(cb).and(any(Predicate[].class));
        clearInvocations(cb);c=collection(CollectionRuleAttribute.TITLE,CollectionRuleOperator.EQUALS,"Stove");c.setMatchCondition(CollectionMatchCondition.ANY);
        apply(c);verify(cb,times(2)).or(any(Predicate[].class));verify(cb,never()).and(any(Predicate[].class));
    }
    @Test void noUsableRulesMatchNoProducts() {
        apply(new Collection());verify(cb).disjunction();
        clearInvocations(cb);apply(collection(CollectionRuleAttribute.TITLE,CollectionRuleOperator.EQUALS));verify(cb).disjunction();
    }
}
