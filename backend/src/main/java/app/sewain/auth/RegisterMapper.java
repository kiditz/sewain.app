package app.sewain.auth;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
interface RegisterMapper extends GlobalMapper<User, UserResponse, RegisterRequest> {
}
