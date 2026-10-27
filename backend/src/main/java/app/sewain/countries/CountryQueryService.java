package app.sewain.countries;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CountryQueryService {

	private final CountryRepository countryRepository;
	private final CountryMapper countryMapper;

	public List<CountryResponse> findAll() {
		return countryMapper.toResponses(countryRepository.findAllByOrderByNameAsc());
	}
}
