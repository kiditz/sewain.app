package app.sewain.customers;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchant/customers")
@RequiredArgsConstructor
class CustomerController {
	private final CustomerService service;

	@GetMapping
	PageResponse<CustomerResponse> search(@RequestParam Map<String, String> params) {
		return service.search(PageQuery.from(params));
	}

	@GetMapping("/summary")
	CustomerStatisticsResponse statistics() {
		return service.statistics();
	}

	@GetMapping("/{id}")
	CustomerResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	CustomerResponse create(@Valid @RequestBody CustomerCommand command) {
		return service.create(command);
	}

	@PatchMapping("/{id}/status")
	CustomerResponse setEnabled(@PathVariable UUID id, @Valid @RequestBody CustomerStatusCommand command) {
		return service.setEnabled(id, command);
	}
}
