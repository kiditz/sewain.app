package app.sewain.media;

import app.sewain.shared.SearchUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

final class MediaSpecifications {

	private static final Map<String, SearchUtils.Operator> SEARCH_FIELDS = Map.of("filename", SearchUtils.Operator.LIKE);

	private MediaSpecifications() {
	}

	public static Specification<Media> search(Map<String, String> searchValues) {
		return SearchUtils.search(Media.class, searchValues, SEARCH_FIELDS);
	}
}