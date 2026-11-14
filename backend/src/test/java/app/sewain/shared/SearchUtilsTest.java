package app.sewain.shared;

import app.sewain.exception.ValidationException;
import jakarta.persistence.Transient;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.math.*;
import java.time.*;
import java.util.*;
import static app.sewain.shared.SearchUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Exercises the public filter API rather than invoking private conversion helpers. */
@SuppressWarnings({"unchecked","rawtypes"})
class SearchUtilsTest {
    enum State { ACTIVE, DRAFT }
    static class Child { String name; }
    static class Row {
        String text; long lng; int integer; short small; byte tiny; double dbl; float flt;
        boolean flag; char character; BigDecimal decimal; BigInteger big; UUID uuid;
        LocalDate date; LocalTime time; LocalDateTime dateTime; Instant instant;
        OffsetDateTime offset; OffsetTime offsetTime; ZonedDateTime zoned; Date legacy;
        State state; Child unsupported; Child child; List<Child> children; List raw;
        @Transient String secret;
    }
    final CriteriaBuilder cb=mock(CriteriaBuilder.class);
    final Root<Row> root=mock(Root.class);
    final Path path=mock(Path.class);
    final CriteriaQuery query=mock(CriteriaQuery.class);
    SearchUtilsTest() { doReturn(Row.class).when(root).getJavaType(); when(root.get(anyString())).thenReturn(path); }
    void apply(String field,Operator operator,Object value) {
        from(Row.class,new Condition(field,operator,value),Map.of(field,FieldRule.searchable())).toPredicate(root,query,cb);
    }

    @Test void scalarConversionHandlesSupportedTypesAndPrimitiveWrappers() {
        var id=UUID.randomUUID();
        Object[][] cases={
                {"lng","12",12L},{"integer","12",12},{"small","12",(short)12},{"tiny","12",(byte)12},
                {"dbl","1.5",1.5d},{"flt","1.5",1.5f},{"flag","TRUE",true},{"character","X",'X'},
                {"decimal","1.25",new BigDecimal("1.25")},{"big","999999999999",new BigInteger("999999999999")},
                {"uuid",id.toString(),id},{"date","2026-01-01",LocalDate.of(2026,1,1)},
                {"time","12:30:00",LocalTime.of(12,30)},
                {"dateTime","2026-01-01T12:30:00",LocalDateTime.of(2026,1,1,12,30)},
                {"instant","2026-01-01T00:00:00Z",Instant.parse("2026-01-01T00:00:00Z")},
                {"offset","2026-01-01T12:00:00+07:00",OffsetDateTime.parse("2026-01-01T12:00:00+07:00")},
                {"offsetTime","12:00:00+07:00",OffsetTime.parse("12:00:00+07:00")},
                {"zoned","2026-01-01T12:00:00+07:00[Asia/Jakarta]",ZonedDateTime.parse("2026-01-01T12:00:00+07:00[Asia/Jakarta]")},
                {"legacy","2026-01-01T00:00:00Z",Date.from(Instant.parse("2026-01-01T00:00:00Z"))},
                {"state","ACTIVE",State.ACTIVE}};
        for(var test:cases) {
            clearInvocations(cb); apply((String)test[0],Operator.EQUAL,test[1]); verify(cb).equal(path,test[2]);
        }
        clearInvocations(cb); apply("integer",Operator.EQUAL,7); verify(cb).equal(path,7);
    }

    @Test void malformedScalarsAreRejectedInsteadOfSilentlyCoerced() {
        for(var test:List.of(new String[]{"flag","yes"},new String[]{"character","AB"},new String[]{"integer","1.5"},
                new String[]{"uuid","invalid"},new String[]{"state","unknown"},new String[]{"date","not-a-date"},new String[]{"unsupported","text"}))
            assertThrows(ValidationException.class,()->apply(test[0],Operator.EQUAL,test[1]));
    }

    @Test void likePatternsEscapeWildcardsAndNormalizeCase() {
        var expression=mock(Expression.class); when(path.as(String.class)).thenReturn(expression); when(cb.lower(expression)).thenReturn(expression);
        for(var entry:Map.of(Operator.LIKE,"%a!%!_!!%",Operator.CONTAINS,"%a!%!_!!%",Operator.STARTS_WITH,"a!%!_!!%",Operator.ENDS_WITH,"%a!%!_!!").entrySet()) {
            clearInvocations(cb); apply("text",entry.getKey(),"A%_!"); verify(cb).like(expression,entry.getValue(),'!');
        }
        assertThrows(IllegalArgumentException.class,()->apply("integer",Operator.CONTAINS,"1"));
    }

    @Test void inAcceptsCsvCollectionOrScalarAndRejectsArraysAndEmptyValues() {
        apply("integer",Operator.IN,"1, 2"); verify(path).in(List.of(1,2));
        apply("integer",Operator.NOT_IN,List.of("3","4")); verify(path).in(List.of(3,4));
        apply("integer",Operator.IN,5); verify(path).in(List.of(5));
        assertThrows(ValidationException.class,()->apply("integer",Operator.IN,new int[]{1,2}));
        assertThrows(ValidationException.class,()->apply("integer",Operator.IN,List.of()));
        assertThrows(ValidationException.class,()->apply("integer",Operator.IN,"1,"));
    }

    @Test void allComparisonOperatorsConvertNumbersAndRejectNonComparableFields() {
        apply("integer",Operator.GREATER_THAN,"10"); verify(cb).greaterThan(path,10);
        apply("integer",Operator.GREATER_THAN_OR_EQUAL,"10"); verify(cb).greaterThanOrEqualTo(path,10);
        apply("integer",Operator.LESS_THAN,"10"); verify(cb).lessThan(path,10);
        apply("integer",Operator.LESS_THAN_OR_EQUAL,"10"); verify(cb).lessThanOrEqualTo(path,10);
        assertThrows(IllegalArgumentException.class,()->apply("unsupported",Operator.GREATER_THAN,new Object()));
    }

    @Test void nullOperatorsDoNotRequireAValueAndNotEqualUsesConvertedValue() {
        apply("text",Operator.IS_NULL,null); verify(cb).isNull(path);
        apply("text",Operator.IS_NOT_NULL,null); verify(cb).isNotNull(path);
        apply("integer",Operator.NOT_EQUAL,"7"); verify(cb).notEqual(path,7);
        assertThrows(ValidationException.class,()->apply("integer",Operator.EQUAL,null));
    }

    @Test void unapprovedFieldsAndOperatorsAreRejected() {
        var rules=Map.of("text",FieldRule.of(false,Operator.EQUAL));
        for(var condition:List.of(new Condition("other",Operator.EQUAL,"x"),new Condition("text",Operator.IN,"x"),
                new Condition("",Operator.EQUAL,"x"),new Condition("text",null,"x")))
            assertThrows(ValidationException.class,()->from(Row.class,condition,rules).toPredicate(root,query,cb));
    }

    @Test void invalidAndTransientPathsCannotBeConfigured() {
        for(String field:List.of("missing","secret"))
            assertThrows(ValidationException.class,()->from(Row.class,new Condition("text",Operator.EQUAL,"x"),Map.of(field,FieldRule.searchable())));
        for(String field:List.of("integer.value","child..name"))
            assertThrows(IllegalArgumentException.class,()->from(Row.class,new Condition("text",Operator.EQUAL,"x"),Map.of(field,FieldRule.searchable())));
        assertThrows(IllegalArgumentException.class,()->from(Row.class,new Group(Logic.AND,null),Map.of()));
        assertThrows(IllegalArgumentException.class,()->from(Row.class,new Group(Logic.AND,null),Map.of("raw",FieldRule.searchable())));
    }

    @Test void sortHonorsDirectionAndBlocksUnapprovedOrCollectionPaths() {
        var rules=Map.of("text",FieldRule.of(true,Operator.EQUAL),"child.name",FieldRule.of(true,Operator.EQUAL),
                "children.name",FieldRule.of(true,Operator.EQUAL),"integer",FieldRule.searchable());
        var sort=toSort(Row.class,List.of(new SortField("text",null),new SortField("child.name",Sort.Direction.DESC)),rules);
        assertEquals(Sort.Direction.ASC,sort.getOrderFor("text").getDirection());
        assertEquals(Sort.Direction.DESC,sort.getOrderFor("child.name").getDirection());
        for(String field:List.of("missing","integer","children.name"))
            assertThrows(ValidationException.class,()->toSort(Row.class,List.of(new SortField(field,null)),rules));
        assertTrue(toSort(Row.class,null,rules).isUnsorted());
    }

    @Test void groupsSupportNestedAndOrAndEmptyNullFilters() {
        var filter=new Group(Logic.OR,List.of(new Group(Logic.AND,List.of(new Condition("integer",Operator.EQUAL,1)))));
        from(Row.class,filter,Map.of("integer",FieldRule.searchable())).toPredicate(root,query,cb);
        verify(cb).and(any(Predicate[].class)); verify(cb).or(any(Predicate[].class));
        from(Row.class,null,Map.of()).toPredicate(root,query,cb); verify(cb).conjunction();
        assertTrue(new Group(Logic.AND,null).filters().isEmpty());
        assertThrows(ValidationException.class,()->new Group(null,List.of()));
        assertThrows(ValidationException.class,()->new FieldRule(Set.of(),false));
        assertThrows(ValidationException.class,()->new SortField(" ",null));
    }

    @Test void collectionNavigationUsesLeftJoinAndDistinctToAvoidDuplicateEntities() {
        Join<Row,Child> join=mock(Join.class);
        doReturn(join).when(root).join("children",JoinType.LEFT);
        doReturn(Child.class).when(join).getJavaType(); when(join.get("name")).thenReturn(path);
        apply("children.name",Operator.EQUAL,"Tent");
        verify(root).join("children",JoinType.LEFT); verify(query).distinct(true); verify(cb).equal(path,"Tent");
    }

    @Test void legacySearchIgnoresUnapprovedAndBlankInputButKeepsNullChecks() {
        var values=new HashMap<String,String>(); values.put("text"," "); values.put("integer",null); values.put("missing","ignored");
        search(Row.class,values,Map.of("text",Operator.EQUAL,"integer",Operator.IS_NULL),true).toPredicate(root,query,cb);
        verify(cb).isNull(path); verify(cb).or(any(Predicate[].class)); verify(cb,never()).equal(any(Expression.class),any(Object.class));
        search(Row.class,Map.of(),Map.of()).toPredicate(root,query,cb); verify(cb).conjunction();
    }
}
