package app.sewain.catalogs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-27T20:08:38+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class TagMapperImpl implements TagMapper {

    @Override
    public List<TagResponse> toResponses(Iterable<Tag> entity) {
        if ( entity == null ) {
            return null;
        }

        List<TagResponse> list = new ArrayList<TagResponse>();
        for ( Tag tag : entity ) {
            list.add( toResponse( tag ) );
        }

        return list;
    }

    @Override
    public List<TagResponse> toResponses(List<Tag> entity) {
        if ( entity == null ) {
            return null;
        }

        List<TagResponse> list = new ArrayList<TagResponse>( entity.size() );
        for ( Tag tag : entity ) {
            list.add( toResponse( tag ) );
        }

        return list;
    }

    @Override
    public List<Tag> toEntities(List<TagCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Tag> list = new ArrayList<Tag>( entity.size() );
        for ( TagCommand tagCommand : entity ) {
            list.add( toEntity( tagCommand ) );
        }

        return list;
    }

    @Override
    public TagResponse toResponse(Tag entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        String slug = null;

        id = entity.getId();
        name = entity.getName();
        slug = entity.getSlug();

        TagResponse tagResponse = new TagResponse( id, name, slug );

        return tagResponse;
    }

    @Override
    public Tag toEntity(TagCommand command) {
        if ( command == null ) {
            return null;
        }

        Tag tag = new Tag();

        tag.setName( command.name() );

        return tag;
    }

    @Override
    public void update(TagCommand command, Tag entity) {
        if ( command == null ) {
            return;
        }

        if ( command.name() != null ) {
            entity.setName( command.name() );
        }
    }
}
