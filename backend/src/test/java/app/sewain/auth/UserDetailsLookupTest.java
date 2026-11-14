package app.sewain.auth;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UserDetailsLookupTest {
 @Test void lookupUsesRepositoryIdentityAndRejectsUnknownUser(){var repository=mock(UserRepository.class);var service=new CustomUserDetailsService(repository);var user=new User();user.setEmail("owner@example.invalid");user.setPassword("hash");user.setEnabled(false);when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));var result=service.loadUserByUsername(user.getEmail());assertEquals(user.getEmail(),result.getUsername());assertEquals("hash",result.getPassword());assertFalse(result.isEnabled());assertThrows(UsernameNotFoundException.class,()->service.loadUserByUsername("unknown@example.invalid"));}
}
