package app.sewain.catalogs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T12:27:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class CollectionMapperImpl implements CollectionMapper {

    @Autowired
    private CollectionRuleMapper collectionRuleMapper;

    @Override
    public List<CollectionsResponse> toResponses(Iterable<Collection> entity) {
        if ( entity == null ) {
            return null;
        }

        List<CollectionsResponse> list = new ArrayList<CollectionsResponse>();
        for ( Collection collection : entity ) {
            list.add( toResponse( collection ) );
        }

        return list;
    }

    @Override
    public List<CollectionsResponse> toResponses(List<Collection> entity) {
        if ( entity == null ) {
            return null;
        }

        List<CollectionsResponse> list = new ArrayList<CollectionsResponse>( entity.size() );
        for ( Collection collection : entity ) {
            list.add( toResponse( collection ) );
        }

        return list;
    }

    @Override
    public List<Collection> toEntities(List<CollectionCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Collection> list = new ArrayList<Collection>( entity.size() );
        for ( CollectionCommand collectionCommand : entity ) {
            list.add( toEntity( collectionCommand ) );
        }

        return list;
    }

    @Override
    public Collection toEntity(CollectionCommand command) {
        if ( command == null ) {
            return null;
        }

        Collection collection = new Collection();

        collection.setName( command.name() );
        collection.setSlug( command.slug() );
        collection.setImageUrl( command.imageUrl() );
        collection.setDescription( command.description() );
        collection.setType( command.type() );
        collection.setMatchCondition( command.matchCondition() );

        return collection;
    }

    @Override
    public void update(CollectionCommand command, Collection entity) {
        if ( command == null ) {
            return;
        }

        if ( command.name() != null ) {
            entity.setName( command.name() );
        }
        if ( command.slug() != null ) {
            entity.setSlug( command.slug() );
        }
        if ( command.imageUrl() != null ) {
            entity.setImageUrl( command.imageUrl() );
        }
        if ( command.description() != null ) {
            entity.setDescription( command.description() );
        }
        if ( command.type() != null ) {
            entity.setType( command.type() );
        }
        if ( command.matchCondition() != null ) {
            entity.setMatchCondition( command.matchCondition() );
        }
    }

    @Override
    public CollectionsResponse toResponse(Collection entity) {
        if ( entity == null ) {
            return null;
        }

        UUID parentId = null;
        UUID id = null;
        String name = null;
        String slug = null;
        String imageUrl = null;
        String description = null;
        CollectionType type = null;
        CollectionMatchCondition matchCondition = null;
        Long productCount = null;

        parentId = entityParentId( entity );
        id = entity.getId();
        name = entity.getName();
        slug = entity.getSlug();
        imageUrl = entity.getImageUrl();
        description = entity.getDescription();
        type = entity.getType();
        matchCondition = entity.getMatchCondition();
        productCount = entity.getProductCount();

        CollectionsResponse collectionsResponse = new CollectionsResponse( id, name, slug, imageUrl, parentId, description, type, matchCondition, productCount );

        return collectionsResponse;
    }

    @Override
    public CollectionDetailResponse toDetail(Collection entity) {
        if ( entity == null ) {
            return null;
        }

        UUID parentId = null;
        List<UUID> productIds = null;
        UUID id = null;
        String name = null;
        String slug = null;
        String imageUrl = null;
        String description = null;
        CollectionType type = null;
        CollectionMatchCondition matchCondition = null;
        List<CollectionRuleResponse> rules = null;

        parentId = entityParentId( entity );
        productIds = collectionItemListToUUIDList( entity.getItems() );
        id = entity.getId();
        name = entity.getName();
        slug = entity.getSlug();
        imageUrl = entity.getImageUrl();
        description = entity.getDescription();
        type = entity.getType();
        matchCondition = entity.getMatchCondition();
        rules = collectionRuleListToCollectionRuleResponseList( entity.getRules() );

        CollectionDetailResponse collectionDetailResponse = new CollectionDetailResponse( id, name, slug, imageUrl, parentId, description, type, matchCondition, rules, productIds );

        return collectionDetailResponse;
    }

    private UUID entityParentId(Collection collection) {
        Collection parent = collection.getParent();
        if ( parent == null ) {
            return null;
        }
        return parent.getId();
    }

    protected List<UUID> collectionItemListToUUIDList(List<CollectionItem> list) {
        if ( list == null ) {
            return null;
        }

        List<UUID> list1 = new ArrayList<UUID>( list.size() );
        for ( CollectionItem collectionItem : list ) {
            list1.add( toProductId( collectionItem ) );
        }

        return list1;
    }

    protected List<CollectionRuleResponse> collectionRuleListToCollectionRuleResponseList(List<CollectionRule> list) {
        if ( list == null ) {
            return null;
        }

        List<CollectionRuleResponse> list1 = new ArrayList<CollectionRuleResponse>( list.size() );
        for ( CollectionRule collectionRule : list ) {
            list1.add( collectionRuleMapper.toResponse( collectionRule ) );
        }

        return list1;
    }
}
