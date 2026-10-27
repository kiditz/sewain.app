package app.sewain.auth;

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
class UpdateProfileMapperImpl implements UpdateProfileMapper {

    @Override
    public UserResponse toResponse(User entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String email = null;
        String fullName = null;
        String phone = null;

        id = entity.getId();
        email = entity.getEmail();
        fullName = entity.getFullName();
        phone = entity.getPhone();

        UserResponse userResponse = new UserResponse( id, email, fullName, phone );

        return userResponse;
    }

    @Override
    public List<UserResponse> toResponses(Iterable<User> entity) {
        if ( entity == null ) {
            return null;
        }

        List<UserResponse> list = new ArrayList<UserResponse>();
        for ( User user : entity ) {
            list.add( toResponse( user ) );
        }

        return list;
    }

    @Override
    public List<UserResponse> toResponses(List<User> entity) {
        if ( entity == null ) {
            return null;
        }

        List<UserResponse> list = new ArrayList<UserResponse>( entity.size() );
        for ( User user : entity ) {
            list.add( toResponse( user ) );
        }

        return list;
    }

    @Override
    public User toEntity(UpdateProfileCommand entity) {
        if ( entity == null ) {
            return null;
        }

        User user = new User();

        user.setEmail( entity.email() );
        user.setPhone( entity.phone() );
        user.setFullName( entity.fullName() );

        return user;
    }

    @Override
    public List<User> toEntities(List<UpdateProfileCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<User> list = new ArrayList<User>( entity.size() );
        for ( UpdateProfileCommand updateProfileCommand : entity ) {
            list.add( toEntity( updateProfileCommand ) );
        }

        return list;
    }

    @Override
    public void update(UpdateProfileCommand command, User entity) {
        if ( command == null ) {
            return;
        }

        if ( command.email() != null ) {
            entity.setEmail( command.email() );
        }
        if ( command.phone() != null ) {
            entity.setPhone( command.phone() );
        }
        if ( command.fullName() != null ) {
            entity.setFullName( command.fullName() );
        }
    }
}
