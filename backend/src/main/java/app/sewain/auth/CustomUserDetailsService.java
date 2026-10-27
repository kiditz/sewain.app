package app.sewain.auth;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;


	@Override
	@Cacheable(
			cacheNames = "userDetails",
			key = "#username"
	)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.info("QUERY USER: {}", username);
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException(ErrorCode.USER_NOT_FOUND));
		return new CustomUserDetails(user);

	}
}