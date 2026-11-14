package app.sewain.auth;

import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileCommandServiceTest {
    final UserRepository users=mock(UserRepository.class);
    final PasswordEncoder encoder=mock(PasswordEncoder.class);
    final ProfileCommandServiceImpl service=new ProfileCommandServiceImpl(users,encoder,Mappers.getMapper(UpdateProfileMapper.class));
    final UUID id=UUID.randomUUID();
    User existing() { var user=new User(); user.setId(id); user.setPassword("old-hash"); when(users.findById(id)).thenReturn(Optional.of(user)); return user; }
    @Test void matchingCurrentPasswordAllowsNewEncodedPassword() {
        var user=existing(); when(encoder.matches("old-password","old-hash")).thenReturn(true);
        when(encoder.encode("new-password")).thenReturn("new-hash");
        service.changePassword(id,new ChangePasswordCommand("old-password","new-password","new-password"));
        assertEquals("new-hash",user.getPassword());
    }
    @Test void confirmationMismatchStopsBeforeLoadingAccount() {
        assertThrows(ValidationException.class,()->service.changePassword(id,new ChangePasswordCommand("old","new-password","different")));
        verifyNoInteractions(users,encoder);
    }
    @Test void incorrectCurrentPasswordAndReusedPasswordAreRejected() {
        var user=existing();
        assertThrows(ValidationException.class,()->service.changePassword(id,new ChangePasswordCommand("wrong","new-password","new-password")));
        when(encoder.matches("old-password","old-hash")).thenReturn(true);
        when(encoder.matches("same-password","old-hash")).thenReturn(true);
        assertThrows(ValidationException.class,()->service.changePassword(id,new ChangePasswordCommand("old-password","same-password","same-password")));
        assertEquals("old-hash",user.getPassword()); verify(encoder,never()).encode(org.mockito.ArgumentMatchers.anyString());
    }
    @Test void missingAccountCannotChangePasswordOrProfile() {
        assertThrows(ResourceNotFoundException.class,()->service.changePassword(id,new ChangePasswordCommand("old","new-password","new-password")));
        assertThrows(ResourceNotFoundException.class,()->service.updateProfile(id,new UpdateProfileCommand("Name","name@example.invalid","08123")));
    }
    @Test void profileUpdatePreservesIdentityAndPassword() {
        var user=existing(); var response=service.updateProfile(id,new UpdateProfileCommand("New Name","new@example.invalid","08123"));
        assertEquals(id,response.id()); assertEquals("New Name",response.fullName()); assertEquals("new@example.invalid",response.email());
        assertEquals("08123",response.phone()); assertEquals("old-hash",user.getPassword());
    }
}
