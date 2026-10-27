package app.sewain.shared;

import org.mapstruct.MappingTarget;

import java.util.List;

public interface GlobalMapper<T, R, C> {

	R toResponse(T entity);

	List<R> toResponses(Iterable<T> entity);
	List<R> toResponses(List<T> entity);

	T toEntity(C entity);
	List<T> toEntities(List<C> entity);

	void update(C command, @MappingTarget T entity);

}
