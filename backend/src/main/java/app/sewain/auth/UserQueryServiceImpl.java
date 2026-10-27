package app.sewain.auth;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UserQueryServiceImpl implements ProfileQueryService {
	private final UserRepository userRepository;


	@Override
	@Transactional(readOnly = true)
	public UserResponse findProfile(String email) {
		var user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
		return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone());
	}
}
