package app.sewain.tenant;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;

/** Real persistence and business services, without HTTP servers or external event consumers. */
@TestConfiguration(proxyBeanMethods = false)
@EnableTransactionManagement(proxyTargetClass = true)
@EnableJpaRepositories(basePackages = {"app.sewain.catalogs", "app.sewain.orders", "app.sewain.settings"})
@Import({HibernateTenantConfig.class, TenantConnectionProvider.class, TenantIdentifierResolver.class})
@ComponentScan(basePackages = "app.sewain", useDefaultFilters = false, includeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "app\\.sewain\\..*MapperImpl"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "app\\.sewain\\.(settings|countries)\\.JsonNodeMapper"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "app\\.sewain\\.settings\\.(SettingFacade|SettingQueryServiceImpl)"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "app\\.sewain\\.catalogs\\.(OrderCatalogService|InventoryUnitLockRepository|ProductCommandServiceImpl)"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "app\\.sewain\\.orders\\.MerchantOrder(CreationService|PaymentService|LifecycleService|QueryServiceImpl|LateFeeService)")
})
public class JpaIntegrationConfiguration {
    @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, HibernatePropertiesCustomizer tenantProperties) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("app.sewain");
        var adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQLDialect");
        factory.setJpaVendorAdapter(adapter);
        var properties = new HashMap<String, Object>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.jdbc.time_zone", "UTC");
        properties.put("hibernate.type.json_format_mapper", "jackson3");
        tenantProperties.customize(properties);
        factory.setJpaPropertyMap(properties);
        return factory;
    }

    @Bean @Primary JpaTransactionManager transactionManager(EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
}
