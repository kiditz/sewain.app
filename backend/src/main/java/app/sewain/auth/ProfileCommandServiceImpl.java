package app.sewain.auth;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class ProfileCommandServiceImpl implements ProfileCommandService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UpdateProfileMapper updateProfileMapper;

	@Override
	@Transactional
	public void changePassword(UUID userId, ChangePasswordCommand command) {
		if (!command.newPassword().equals(command.confirmPassword())) {
			throw new ValidationException(ErrorCode.PASSWORD_CONFIRM_NOTMATCH);
		}
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
		if (!passwordEncoder.matches(
				command.currentPassword(),
				user.getPassword()
		)) {
			throw new ValidationException(ErrorCode.PASSWORD_CURRENT_INVALID);
		}

		if (passwordEncoder.matches(
				command.newPassword(),
				user.getPassword()
		)) {
			throw new ValidationException(ErrorCode.PASSWORD_SAME);
		}
		user.changePassword(passwordEncoder.encode(command.newPassword()));

	}

	@Override
	@Transactional
	@CacheEvict(
			cacheNames = "userDetails",
			key = "#command.email"
	)
	public UserResponse updateProfile(UUID userId, UpdateProfileCommand command) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
		updateProfileMapper.update(command, user);
		return updateProfileMapper.toResponse(user);
	}
}
