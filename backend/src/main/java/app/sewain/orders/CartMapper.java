package app.sewain.orders;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
		componentModel = "spring",
		uses = CartItemMapper.class
)
interface CartMapper {


	CartResponse toResponse(Cart cart);
	
	@Mapping(target = "items", source = "items")
	CartResponse toResponse(Cart cart, List<CartItemResponse> items);

}