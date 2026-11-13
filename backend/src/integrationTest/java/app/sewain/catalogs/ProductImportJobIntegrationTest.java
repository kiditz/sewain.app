package app.sewain.catalogs;

import app.sewain.media.RemoteImageImporter;
import app.sewain.merchants.TenantAccessService;
import app.sewain.testsupport.JpaIntegrationSupport;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductImportJobIntegrationTest extends JpaIntegrationSupport {
    @Override protected void customizeContext(AnnotationConfigApplicationContext context) {
        context.register(BatchTestConfiguration.class);
    }

    @Test
    void importsCsvAsOneProductWithMultipleVariantsPlansAndPhysicalInventory() throws Exception {
        var result = runCsv("""
                name,slug,sku,rental_fee,stock,variant_name,status
                Alpine Tent,alpine-tent,TENT-2P,100000,2,2 Person,ACTIVE
                Alpine Tent,alpine-tent,TENT-4P,150000,3,4 Person,ACTIVE
                """);

        assertEquals("COMPLETED", result.status());
        assertEquals(1, result.total());
        assertEquals(1, result.succeeded());
        assertEquals(1, count("products"));
        assertEquals(2, count("product_variants"));
        assertEquals(2, count("variant_rental_plans"));
        assertEquals(5, count("inventory_units"));
        assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM public.product_import_rows WHERE import_id=? AND status='SUCCEEDED'",
                Long.class, result.id()));
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM public.batch_job_execution"
                + " WHERE job_execution_id=(SELECT max(job_execution_id) FROM public.batch_job_execution)", String.class));
        verifyNoInteractions(context.getBean(RemoteImageImporter.class));
    }

    @Test
    void invalidProductDoesNotPreventLaterProductsAndExistingSlugIsSkipped() throws Exception {
        catalogFixture("Already exists", "AVAILABLE");
        var existingSlug = jdbc.queryForObject("SELECT slug FROM " + tenant + ".products", String.class);
        var result = runCsv("name,slug,sku,rental_fee,stock\n"
                + "Existing," + existingSlug + ",EXISTING,100000,1\n"
                + "Invalid,invalid,INVALID,-1,1\n"
                + "New Tent,new-tent,NEW,120000,1\n");

        assertEquals("COMPLETED", result.status());
        assertEquals(3, result.processed());
        assertEquals(1, result.succeeded());
        assertEquals(1, result.failed());
        assertEquals(1, result.skipped());
        assertEquals(2, count("products"));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM public.product_import_rows WHERE import_id=? AND status='FAILED'",
                Long.class, result.id()));
    }

    @Test
    void imageFailureIsReportedWithoutLeakingUrlAndLaterProductStillImports() throws Exception {
        var secretUrl = "https://example.invalid/image.png?token=private";
        when(context.getBean(RemoteImageImporter.class).download(secretUrl))
                .thenThrow(new java.io.IOException("Download failed: " + secretUrl));

        var result = runCsv("name,slug,sku,rental_fee,image_urls\n"
                + "Broken,broken,BROKEN,100000," + secretUrl + "\n"
                + "Valid,valid,VALID,100000,\n");

        assertEquals("COMPLETED", result.status());
        assertEquals(1, result.failed());
        assertEquals(1, result.succeeded());
        assertEquals(1, count("products"));
        assertEquals("valid", jdbc.queryForObject("SELECT slug FROM " + tenant + ".products", String.class));
        assertEquals(1, result.errors().size());
        assertFalse(result.errors().getFirst().message().contains("private"));
        assertFalse(result.errors().getFirst().message().contains("https://"));
    }

    @Test
    void onlyOneActiveImportPerTenantAndStartupMarksInterruptedImportsFailed() throws Exception {
        var id = UUID.randomUUID();
        var user = UUID.randomUUID();
        jdbc.update("INSERT INTO public.product_imports(id,tenant,user_id,status,total) VALUES (?,?,?,'RUNNING',1)",
                id, tenant, user);
        var service = context.getBean(ProductImportService.class);
        var error = assertThrows(ResponseStatusException.class, () -> service.start(csv("""
                name,slug,sku,rental_fee
                Tent,tent,TENT,100000
                """), user));
        assertEquals(409, error.getStatusCode().value());

        service.recoverInterrupted();

        assertEquals("FAILED", jdbc.queryForObject("SELECT status FROM public.product_imports WHERE id=?", String.class, id));
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM public.product_imports WHERE id=?", Long.class, id));
        assertEquals(0, count("products"));
    }

    private ProductImportService.Status runCsv(String contents) throws Exception {
        var finished = new CountDownLatch(1);
        var terminal = new AtomicReference<ProductImportService.Status>();
        var socket = context.getBean(SimpMessagingTemplate.class);
        doAnswer(invocation -> {
            var status = (ProductImportService.Status) invocation.getArgument(2);
            if (status.status().equals("COMPLETED") || status.status().equals("FAILED")) {
                terminal.set(status);
                finished.countDown();
            }
            return null;
        }).when(socket).convertAndSendToUser(anyString(), eq("/queue/product-imports"), any(Object.class));
        var service = context.getBean(ProductImportService.class);
        var started = service.start(csv(contents), UUID.randomUUID());
        assertTrue(finished.await(30, TimeUnit.SECONDS), "Batch job must finish and publish a terminal status");
        assertEquals(started.id(), terminal.get().id());
        return terminal.get();
    }

    private MockMultipartFile csv(String contents) {
        return new MockMultipartFile("file", "products.csv", "text/csv", contents.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableBatchProcessing(transactionManagerRef = "importBatchTransactionManager")
    @EnableJdbcJobRepository(transactionManagerRef = "importBatchTransactionManager", tablePrefix = "public.BATCH_")
    @Import({ProductImportService.class, ProductImportRowService.class})
    static class BatchTestConfiguration {
        @Bean JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }
        @Bean JdbcTransactionManager importBatchTransactionManager(DataSource source) { return new JdbcTransactionManager(source); }
        @Bean ProductImportConfiguration.ImportWorker productImportWorker() { return new ProductImportConfiguration.ImportWorker(); }
        @Bean SimpMessagingTemplate socket() { return mock(SimpMessagingTemplate.class); }
        @Bean TenantAccessService tenantAccessService() { return mock(TenantAccessService.class); }
        @Bean RemoteImageImporter remoteImageImporter() { return mock(RemoteImageImporter.class); }
        @Bean(destroyMethod = "close") ValidatorFactory validatorFactory() { return Validation.buildDefaultValidatorFactory(); }
        @Bean Validator validator(ValidatorFactory factory) { return factory.getValidator(); }
    }
}
