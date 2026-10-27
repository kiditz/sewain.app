package app.sewain.finance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/merchant/finance")
@RequiredArgsConstructor
class FinanceController {
	private final FinanceService service;

	@GetMapping
	FinanceDashboardResponse get(@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(required = false, defaultValue = "ALL") String direction,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "20") Integer size) {
		return service.get(from, to, direction, page, size);
	}

	@PostMapping("/expenses")
	@ResponseStatus(HttpStatus.CREATED)
	FinanceTransactionResponse recordExpense(@Valid @RequestBody RecordExpenseCommand command) {
		return service.recordExpense(command);
	}
}
