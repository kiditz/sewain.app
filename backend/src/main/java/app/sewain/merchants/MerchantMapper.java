package app.sewain.merchants;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
interface MerchantMapper extends GlobalMapper<Merchant, MerchantResponse, MerchantCommand> {
}
