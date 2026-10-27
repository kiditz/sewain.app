package app.sewain.catalogs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T12:27:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class CollectionRuleMapperImpl implements CollectionRuleMapper {

    @Override
    public CollectionRuleResponse toResponse(CollectionRule rule) {
        if ( rule == null ) {
            return null;
        }

        UUID id = null;
        CollectionRuleAttribute attribute = null;
        CollectionRuleOperator operator = null;
        List<String> values = null;

        id = rule.getId();
        attribute = rule.getAttribute();
        operator = rule.getOperator();
        values = collectionRuleValueListToStringList( rule.getValues() );

        CollectionRuleResponse collectionRuleResponse = new CollectionRuleResponse( id, attribute, operator, values );

        return collectionRuleResponse;
    }

    protected List<String> collectionRuleValueListToStringList(List<CollectionRuleValue> list) {
        if ( list == null ) {
            return null;
        }

        List<String> list1 = new ArrayList<String>( list.size() );
        for ( CollectionRuleValue collectionRuleValue : list ) {
            list1.add( toValue( collectionRuleValue ) );
        }

        return list1;
    }
}
