package app.sewain.reports;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/reports")
@RequiredArgsConstructor
class MerchantReportController {
	private final MerchantReportService service;

	@GetMapping
	MerchantReportResponse get(@RequestParam(defaultValue = "30") int days) {
		return service.get(days);
	}
}
