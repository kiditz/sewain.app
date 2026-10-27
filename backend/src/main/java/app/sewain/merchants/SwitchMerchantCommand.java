package app.sewain.merchants;

import java.util.UUID;

record SwitchMerchantCommand(
		UUID userId,
		String slug,
		String username
) {
}
