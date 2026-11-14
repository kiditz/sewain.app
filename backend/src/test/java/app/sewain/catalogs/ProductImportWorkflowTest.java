package app.sewain.catalogs;

import app.sewain.merchants.TenantAccessService;
import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.*;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.sql.ResultSet;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SuppressWarnings({"unchecked","rawtypes"})
class ProductImportWorkflowTest {
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final JobRepository jobs=mock(JobRepository.class);
    final JobOperator operator=mock(JobOperator.class);
    final ProductImportConfiguration.ImportWorker worker=mock(ProductImportConfiguration.ImportWorker.class);
    final ProductImportRowService rows=mock(ProductImportRowService.class);
    final SimpMessagingTemplate socket=mock(SimpMessagingTemplate.class);
    final TenantAccessService access=mock(TenantAccessService.class);
    final ProductImportService service=new ProductImportService(jdbc,jobs,operator,worker,rows,socket,access);
    final UUID user=UUID.randomUUID();
    final AtomicReference<String> state=new AtomicReference<>("QUEUED");
    final ResultSet rs=mock(ResultSet.class);
    @BeforeEach void setup()throws Exception {
        TenantContext.set("shop");when(rs.getString("tenant")).thenReturn("shop");when(rs.getString("status")).thenAnswer(c->state.get());
        when(jdbc.query(contains("SELECT * FROM public.product_imports"),any(RowMapper.class),any(UUID.class),eq(user)))
                .thenAnswer(c->List.of(((RowMapper<?>)c.getArgument(1)).mapRow(rs,0)));
        when(jdbc.update(startsWith("UPDATE public.product_imports SET status=?"),any(Object[].class)))
                .thenAnswer(c->{state.set(c.getArgument(1));return 1;});
        doAnswer(c->{((Runnable)c.getArgument(0)).run();return null;}).when(worker).execute(any());
        // Exercise the real import tasklet. Batch metadata/transactions remain covered separately.
        when(operator.start(any(Job.class),any())).thenAnswer(c->{
            SimpleJob job=c.getArgument(0);var tasklet=((TaskletStep)job.getStep("importProductRows")).getTasklet();
            var contribution=mock(StepContribution.class);
            while(tasklet.execute(contribution,null)==RepeatStatus.CONTINUABLE) { }
            var execution=mock(JobExecution.class);when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);return execution;
        });
        when(rows.create(anyList())).thenReturn("SUCCEEDED");
    }
    @AfterEach void clear(){TenantContext.clear();}
    MockMultipartFile csv(String rows){return new MockMultipartFile("file","products.csv","text/csv",("name,slug,sku,rental_fee\n"+rows).getBytes(StandardCharsets.UTF_8));}
    @Test void workerProcessesGroupedVariantsAndClearsTenantAfterCompletion()throws Exception {
        var result=service.start(csv("Tent,tent,T1,100\nTent,tent,T2,100\nStove,stove,S1,50\n"),user);
        assertEquals("COMPLETED",result.status());assertNull(TenantContext.get());
        var groups=org.mockito.ArgumentCaptor.forClass(List.class);verify(rows,times(2)).create(groups.capture());
        assertEquals(2,groups.getAllValues().getFirst().size());assertEquals(1,groups.getAllValues().get(1).size());
        verify(jdbc,times(3)).update(startsWith("INSERT INTO public.product_import_rows"),any(Object[].class));
    }
    @Test void skippedAndInvalidProductsDoNotStopLaterGroupsAndErrorsAreSanitized()throws Exception {
        when(rows.create(anyList())).thenReturn("SKIPPED").thenThrow(new IllegalArgumentException("x".repeat(300)))
                .thenThrow(new java.io.IOException("https://host?secret=password")).thenReturn("SUCCEEDED");
        assertEquals("COMPLETED",service.start(csv("A,a,A,10\nB,b,B,10\nC,c,C,10\nD,d,D,10\n"),user).status());
        // Check the row writes specifically without relying on notification timing.
        verify(jdbc).update(startsWith("INSERT INTO public.product_import_rows"),any(UUID.class),eq(3),eq("FAILED"),eq("x".repeat(255)));
        verify(jdbc).update(startsWith("INSERT INTO public.product_import_rows"),any(UUID.class),eq(4),eq("FAILED"),argThat((String text)->!text.contains("secret")));
        verify(rows,times(4)).create(anyList());
    }
    @Test void jobFailureAndBrokenNotificationsStillFinishAndClearTenant()throws Exception {
        when(operator.start(any(Job.class),any())).thenThrow(new IllegalStateException("Job unavailable"));
        doThrow(new IllegalStateException("Browser offline")).when(socket).convertAndSendToUser(anyString(),anyString(),any(Object.class));
        assertEquals("FAILED",service.start(csv("Tent,tent,T1,100\n"),user).status());assertNull(TenantContext.get());
    }
    @Test void unsuccessfulBatchExecutionMarksImportFailed()throws Exception {
        var execution=mock(JobExecution.class);when(execution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(operator.start(any(Job.class),any())).thenReturn(execution);
        assertEquals("FAILED",service.start(csv("Tent,tent,T1,100\n"),user).status());
    }
    @Test void duplicateActiveImportAndFullWorkerQueueReturnConflictAndRateLimit()throws Exception {
        when(jdbc.update(startsWith("INSERT INTO public.product_imports"),any(Object[].class))).thenThrow(new DuplicateKeyException("busy"));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.start(csv("Tent,tent,T1,100\n"),user)).getStatusCode().value());
        when(jdbc.update(startsWith("INSERT INTO public.product_imports"),any(Object[].class))).thenReturn(1);
        doThrow(new TaskRejectedException("busy")).when(worker).execute(any());
        assertEquals(429,assertThrows(ResponseStatusException.class,()->service.start(csv("Tent,tent,T1,100\n"),user)).getStatusCode().value());assertEquals("FAILED",state.get());
    }
    @Test void snapshotRejectsCrossTenantAndUnknownImports()throws Exception {
        TenantContext.set("other");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.snapshot(UUID.randomUUID(),user));
        TenantContext.set("shop");when(jdbc.query(contains("SELECT * FROM public.product_imports"),any(RowMapper.class),any(UUID.class),eq(user))).thenReturn(List.of());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.snapshot(UUID.randomUUID(),user)).getStatusCode().value());
    }
    @Test void latestReturnsNullOrOwnedImportAndRecoveryMarksInterruptedJobs()throws Exception {
        assertNull(service.latest(user));var id=UUID.randomUUID();
        when(jdbc.queryForList(contains("ORDER BY created_at DESC"),eq(UUID.class),eq("shop"),eq(user))).thenReturn(List.of(id));
        assertEquals(id,service.latest(user).id());verify(access).validateAccess(user,"shop");service.recoverInterrupted();
        verify(jdbc).update(contains("status IN ('QUEUED','RUNNING')"));
    }
}
