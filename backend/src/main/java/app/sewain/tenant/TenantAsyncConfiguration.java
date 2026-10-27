package app.sewain.tenant;

import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

@Configuration
class TenantAsyncConfiguration {
	@Bean
	TaskDecorator tenantContextTaskDecorator() {
		return new TenantContextTaskDecorator();
	}

	@Bean
	ThreadPoolTaskExecutorCustomizer tenantExecutorCustomizer(
			TaskDecorator decorator
	) {
		return executor -> executor.setTaskDecorator(decorator);
	}

}
