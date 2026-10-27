package app.sewain.customers;

import jakarta.validation.constraints.NotNull;

record CustomerStatusCommand(@NotNull Boolean enabled) {}
