package app.sewain.orders;

import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = RentalPlansMapper.class)
interface CartItemMapper {

	@Mapping(
			target = "rentalPlans",
			source = "rentalPlans",
			qualifiedByName = "mapRentalPlans"
	)
	CartItemResponse toResponse(CartItemProjection projection);

	List<CartItemResponse> toResponses(
			List<CartItemProjection> projections
	);

	CartItemResponse toResponse(CartItem item);



	@Mapping(target = "id", ignore = true)
	@Mapping(target = "cart", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	CartItem toEntity(CartItemCommand command);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "cart", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(
			CartItemCommand command,
			@MappingTarget CartItem item
	);
}