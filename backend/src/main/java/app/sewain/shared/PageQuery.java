package app.sewain.shared;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PageQuery {
	private Integer page;
	private Integer size;
	private List<Map<String, String>> sort = List.of(
			Map.of("key", "name", "order", "asc")
	);
	private Map<String, String> search = new HashMap<>();
	public static PageQuery from(
			Map<String, String> params
	) {
		var query = new PageQuery();
		if (params == null || params.isEmpty()) return query;
		if (params.get("page") != null) query.setPage(Integer.valueOf(params.get("page")));
		if (params.get("size") != null) query.setSize(Integer.valueOf(params.get("size")));
		if (params.get("sortBy") != null && !params.get("sortBy").isBlank()) {
			query.setSort(List.of(Map.of(
					"key", params.get("sortBy"),
					"order", params.getOrDefault("direction", "asc")
			)));
		}
		query.setSearch(params.entrySet().stream()
				.filter(entry -> !List.of("page", "size", "sortBy", "direction").contains(entry.getKey()))
				.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

		return query;
	}

	public static PageQuery defaultQuery() {
		return new PageQuery();
	}

	public int pageIndex() {
		return page == null || page < 0 ? 0 : page;
	}

	public Integer getSize() {
		return size == null || size < 1 ? 10 : Math.min(size, 100);
	}
}
