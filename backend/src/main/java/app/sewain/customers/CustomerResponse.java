package app.sewain.customers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

record CustomerResponse(
		UUID id,
		String fullName,
		String email,
		String phone,
		String notes,
		long orderCount,
		BigDecimal totalSpent,
		OffsetDateTime lastOrderAt,
		OffsetDateTime createdAt,
		boolean enabled,
        String source,
        boolean emailSubscribed,
        OffsetDateTime emailConsentAt
) {}
