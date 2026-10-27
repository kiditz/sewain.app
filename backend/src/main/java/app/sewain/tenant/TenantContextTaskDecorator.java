package app.sewain.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;

@Slf4j
final class TenantContextTaskDecorator implements TaskDecorator {

	@Override
	public Runnable decorate(Runnable task) {
		String capturedTenant = TenantContext.get();

		log.info("Decorating task with tenant: {}", capturedTenant);

		return () -> {
			String previousTenant = TenantContext.get();

			try {
				if (capturedTenant == null) {
					TenantContext.clear();
				} else {
					TenantContext.set(capturedTenant);
				}

				log.info("Task started with tenant: {}", TenantContext.get());

				task.run();
			} finally {
				if (previousTenant == null) {
					TenantContext.clear();
				} else {
					TenantContext.set(previousTenant);
				}
			}
		};
	}
}