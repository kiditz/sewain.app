package app.sewain.settings;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class SettingQueryServiceImpl implements SettingQueryService {
	private final SettingRepository repository;
	private final SettingMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public List<SettingResponse> findAll() {
		return repository.findAll().stream().map(mapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public SettingResponse findByKey(SettingKey key) {
		return repository.findByKey(key)
				.map(mapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SETTINGS_NOTFOUND));
	}
}
