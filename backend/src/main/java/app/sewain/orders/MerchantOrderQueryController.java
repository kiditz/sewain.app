package app.sewain.orders;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
class MerchantOrderQueryController {
    private final MerchantOrderQueryService service;

    @GetMapping
    PageResponse<MerchantOrderResponse> search(@RequestParam Map<String, String> params) { return service.search(PageQuery.from(params)); }

    @GetMapping("/activities")
    java.util.List<MerchantOrderActivityResponse> recentActivities() { return service.recentActivities(); }

    @GetMapping("/calendar")
    java.util.List<MerchantOrderCalendarEntry> calendar(@RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to) { return service.calendar(from, to); }

    @GetMapping("/{id}")
    MerchantOrderResponse get(@PathVariable UUID id) { return service.get(id); }
}
