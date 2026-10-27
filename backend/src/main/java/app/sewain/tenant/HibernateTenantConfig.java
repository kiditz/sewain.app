package app.sewain.tenant;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class HibernateTenantConfig {

	@Bean
	HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
			TenantConnectionProvider connectionProvider,
			TenantIdentifierResolver tenantResolver
	) {
		return properties -> {
			properties.put(
					MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER,
					connectionProvider
			);

			properties.put(
					MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
					tenantResolver
			);
		};
	}
}
