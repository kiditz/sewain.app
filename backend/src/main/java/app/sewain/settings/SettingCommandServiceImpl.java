package app.sewain.settings;

import app.sewain.exception.ConflictException;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service

@RequiredArgsConstructor
class SettingCommandServiceImpl implements SettingCommandService {
	private final SettingRepository repository;
	private final SettingMapper mapper;

	@Override
	@Transactional
	public SettingResponse create(SettingCommand command) {
		if (repository.existsByKey(command.key())) {
			throw new ConflictException(ErrorCode.SETTING_EXISTS);
		}

		var setting = mapper.toEntity(command);
		return mapper.toResponse(repository.save(setting));
	}

	@Override
	@Transactional
	public SettingResponse update(UUID id, SettingCommand command) {
		var setting = findById(id);
		mapper.update(command, setting);
		return mapper.toResponse(setting);
	}

	@Override
	public void delete(UUID id) {
		var setting = findById(id);
		repository.delete(setting);
	}

	private Setting findById(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SETTINGS_NOTFOUND));
	}

}
