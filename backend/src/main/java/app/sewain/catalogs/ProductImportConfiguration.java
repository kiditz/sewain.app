package app.sewain.catalogs;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing(transactionManagerRef = "importBatchTransactionManager")
@EnableJdbcJobRepository(transactionManagerRef = "importBatchTransactionManager", tablePrefix = "public.BATCH_")
class ProductImportConfiguration {
    @Bean @org.springframework.context.annotation.Primary
    org.springframework.orm.jpa.JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) {
        return new org.springframework.orm.jpa.JpaTransactionManager(factory);
    }
    @Bean JdbcTransactionManager importBatchTransactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
    // Keep this worker separate from the application's @Async executor and tenant decorator.
    @Bean ImportWorker productImportWorker() { return new ImportWorker(); }
    static final class ImportWorker {
        private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        ImportWorker() {
            executor.setCorePoolSize(1); executor.setMaxPoolSize(1); executor.setQueueCapacity(10);
            executor.setThreadNamePrefix("product-import-");
            executor.setWaitForTasksToCompleteOnShutdown(true); executor.setAwaitTerminationSeconds(30);
            executor.initialize();
        }
        void execute(Runnable task) { executor.execute(task); }
        @jakarta.annotation.PreDestroy void close() { executor.shutdown(); }
    }
}
