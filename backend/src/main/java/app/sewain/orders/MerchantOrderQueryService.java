package app.sewain.orders;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface MerchantOrderQueryService {
    PageResponse<MerchantOrderResponse> search(PageQuery query);
    List<MerchantOrderActivityResponse> recentActivities();
    List<MerchantOrderCalendarEntry> calendar(OffsetDateTime fromAt, OffsetDateTime toAt);
    MerchantOrderResponse get(UUID id);
}
