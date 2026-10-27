package app.sewain.customers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CustomerCommand(
	@NotBlank @Size(max = 150) String fullName,
	@NotBlank @Email @Size(max = 255) String email,
	@Size(max = 30) String phone,
	@Size(max = 5000) String notes
) {}
