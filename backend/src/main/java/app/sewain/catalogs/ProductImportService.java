package app.sewain.catalogs;

import app.sewain.merchants.TenantAccessService;
import app.sewain.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
class ProductImportService {
    private final JdbcTemplate jdbc;
    private final JobRepository jobRepository;
    private final JobOperator jobOperator;
    private final ProductImportConfiguration.ImportWorker productImportWorker;
    private final ProductImportRowService rows;
    private final SimpMessagingTemplate socket;
    private final TenantAccessService access;
    record ProductRows(List<Map<String,String>> variants, List<Integer> recordNumbers) {}
    static List<ProductRows> group(List<Map<String,String>> records) {
        var groups = new LinkedHashMap<String,ProductRows>();
        for (int i=0;i<records.size();i++) {
            var row = records.get(i);
            var slug = row.getOrDefault("slug", "");
            var key = slug.isBlank() ? "invalid:"+i : slug;
            var group = groups.computeIfAbsent(key, ignored -> new ProductRows(new ArrayList<>(),new ArrayList<>()));
            group.variants().add(row);
            group.recordNumbers().add(i+2);
        }
        return new ArrayList<>(groups.values());
    }
    record RowResult(int row, String status, String message) {}
    record Status(UUID id, String status, int total, int processed, int succeeded, int skipped, int failed, String message, long version, List<RowResult> errors) {}

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterrupted() {
        jdbc.update("UPDATE public.product_imports SET status='FAILED', message='Import terhenti saat backend restart. Upload ulang CSV; slug yang sudah ada akan dilewati.',version=version+1 WHERE status IN ('QUEUED','RUNNING')");
    }

    Status start(MultipartFile file, UUID user) throws IOException {
        var tenant = TenantContext.get();
        if (tenant == null || tenant.equals("public")) throw new AccessDeniedException("Pilih toko terlebih dahulu");
        if (file.isEmpty() || file.getSize()>5*1024*1024) throw bad("CSV harus diisi, maksimal 5 MB");
        var records = group(parse(file));
        var id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO public.product_imports(id,tenant,user_id,status,total) VALUES (?,?,?,'QUEUED',?)",id,tenant,user,records.size());
        } catch (org.springframework.dao.DuplicateKeyException busy) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Masih ada import aktif untuk toko ini"); }
        try { productImportWorker.execute(() -> run(id,tenant,user,records)); }
        catch (org.springframework.core.task.TaskRejectedException busy) {
            finish(id,"FAILED","Antrean penuh. Coba upload lagi nanti.");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Antrean import penuh");
        }
        return snapshot(id,user);
    }

    static List<Map<String,String>> parse(MultipartFile file) throws IOException {
        var bytes = file.getBytes();
        var text = new String(bytes,StandardCharsets.UTF_8).replaceFirst("^\\uFEFF", "");
        var format = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).get();
        try (var parser = format.parse(new StringReader(text))) {
            var headers = parser.getHeaderNames();
            if (new HashSet<>(headers).size()!=headers.size() || !new HashSet<>(headers).containsAll(List.of("name","slug","sku","rental_fee"))) throw bad("Header wajib: name,slug,sku,rental_fee");
            var allowed = Set.of("name","slug","sku","rental_fee","stock","buy_price","deposit_amount","status","description","image_urls","variant_name");
            if (!allowed.containsAll(headers)) throw bad("Ada kolom CSV yang tidak dikenal. Gunakan template import.");
            var result = new ArrayList<Map<String,String>>();
            for (var record : parser) {
                if (!record.isConsistent()) throw bad("Jumlah kolom tidak sesuai pada record "+record.getRecordNumber());
                if (result.size()>=1000) throw bad("Maksimal 1000 baris varian per import");
                var row = new HashMap<>(record.toMap());
                row.entrySet().removeIf(entry -> entry.getValue().isBlank() && !Set.of("name","slug","sku","rental_fee").contains(entry.getKey()));
                result.add(row);
            }
            if (result.isEmpty()) throw bad("CSV tidak memiliki produk");
            return result;
        } catch (IOException | UncheckedIOException | IllegalArgumentException error) { throw bad("CSV tidak valid: periksa pemisah koma dan tanda kutip"); }
    }

    private void run(UUID id,String tenant,UUID user,List<ProductRows> records) {
        TenantContext.set(tenant);
        try {
            jdbc.update("UPDATE public.product_imports SET status='RUNNING',version=version+1 WHERE id=?",id);
            publish(id,user);
            var iterator = records.iterator();
            var step = new StepBuilder("importProductRows",jobRepository).tasklet((contribution,context) -> {
                if (!iterator.hasNext()) return RepeatStatus.FINISHED;
                var product = iterator.next();
                String state, message = null;
                try { state = rows.create(product.variants()); }
                catch (Exception error) {
                    state = "FAILED";
                    // Never expose HTTP URLs/credentials or SQL diagnostics in the report.
                    message = error instanceof IllegalArgumentException ? error.getMessage() : "Produk/gambar gagal disimpan. Periksa data, SKU unik, dan URL gambar publik.";
                    if (message == null) message = "Data tidak valid";
                    message = message.substring(0,Math.min(255,message.length()));
                    log.warn("Product import {} records {} failed ({})",id,product.recordNumbers(),error.getClass().getSimpleName());
                }
                for (var row : product.recordNumbers())
                    jdbc.update("INSERT INTO public.product_import_rows(import_id,row_number,status,message) VALUES (?,?,?,?)",id,row,state,message);
                jdbc.update("UPDATE public.product_imports SET processed=processed+1,succeeded=succeeded+?,skipped=skipped+?,failed=failed+?,version=version+1 WHERE id=?",state.equals("SUCCEEDED")?1:0,state.equals("SKIPPED")?1:0,state.equals("FAILED")?1:0,id);
                contribution.incrementReadCount();
                if (state.equals("SUCCEEDED")) contribution.incrementWriteCount(1);
                publish(id,user);
                return iterator.hasNext()?RepeatStatus.CONTINUABLE:RepeatStatus.FINISHED;
            },new ResourcelessTransactionManager()).build();
            var job = new JobBuilder("productCsvImport",jobRepository).preventRestart().start(step).build();
            var execution = jobOperator.start(job,new JobParametersBuilder().addString("importId",id.toString()).toJobParameters());
            if (execution.getStatus().isUnsuccessful()) finish(id,"FAILED","Import terhenti. Upload ulang; slug yang sudah ada akan dilewati.");
            else finish(id,"COMPLETED",null);
        } catch (Exception error) {
            log.error("Product import {} could not complete",id,error);
            finish(id,"FAILED","Import terhenti. Upload ulang; slug yang sudah ada akan dilewati.");
        } finally {
            try { publish(id,user); } finally { TenantContext.clear(); }
        }
    }
    private void finish(UUID id,String status,String message) { jdbc.update("UPDATE public.product_imports SET status=?,message=?,version=version+1 WHERE id=?",status,message,id); }
    private void publish(UUID id,UUID user) {
        try { socket.convertAndSendToUser(user.toString(),"/queue/product-imports",snapshot(id,user)); }
        catch (Exception error) { log.debug("Import notification unavailable for {}",id); }
    }
    Status latest(UUID user) {
        var ids = jdbc.queryForList("SELECT id FROM public.product_imports WHERE tenant=? AND user_id=? ORDER BY created_at DESC LIMIT 1",UUID.class,TenantContext.get(),user);
        return ids.isEmpty()?null:snapshot(ids.getFirst(),user);
    }
    Status snapshot(UUID id,UUID user) {
        var results = jdbc.query("SELECT * FROM public.product_imports WHERE id=? AND user_id=?",(rs,n) -> {
            var tenant = rs.getString("tenant");
            var current = TenantContext.get();
            if (current != null && !current.equals("public") && !current.equals(tenant)) throw new AccessDeniedException("Import toko lain");
            access.validateAccess(user,tenant);
            return new Status(id,rs.getString("status"),rs.getInt("total"),rs.getInt("processed"),rs.getInt("succeeded"),rs.getInt("skipped"),rs.getInt("failed"),rs.getString("message"),rs.getLong("version"),List.of());
        },id,user);
        if (results.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Import tidak ditemukan");
        var s = results.getFirst();
        var errors = jdbc.query("SELECT row_number,status,message FROM public.product_import_rows WHERE import_id=? AND status='FAILED' ORDER BY row_number LIMIT 100",(rs,n)->new RowResult(rs.getInt(1),rs.getString(2),rs.getString(3)),id);
        return new Status(s.id(),s.status(),s.total(),s.processed(),s.succeeded(),s.skipped(),s.failed(),s.message(),s.version(),errors);
    }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}
