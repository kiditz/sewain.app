package app.sewain.catalogs;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.Set;
import app.sewain.media.RemoteImageImporter;
import jakarta.validation.Validator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ProductImportTest {
    private MockMultipartFile csv(String text) {
        return new MockMultipartFile("file","products.csv","text/csv",text.getBytes(StandardCharsets.UTF_8));
    }
    @Test void readsBomQuotedCommaAndMultilineDescription() throws Exception {
        var records = ProductImportService.parse(csv("\uFEFFname,slug,sku,rental_fee,description\r\n\"Tenda, Duo\",tenda-duo,TD-01,50000,\"Baris satu\nBaris dua\"\r\n"));
        assertEquals(1,records.size());
        assertEquals("Tenda, Duo",records.getFirst().get("name"));
        assertEquals("Baris satu\nBaris dua",records.getFirst().get("description"));
    }
    @Test void rejectsMissingHeadersMismatchedColumnsAndOversizedCatalog() {
        assertThrows(ResponseStatusException.class,()->ProductImportService.parse(csv("name,slug\nTenda,tenda\n")));
        assertThrows(ResponseStatusException.class,()->ProductImportService.parse(csv("name,slug,sku,rental_fee\nTenda,tenda,TD\n")));
        assertThrows(ResponseStatusException.class,()->ProductImportService.parse(csv("name,slug,sku,rental_fee\n"+"Tenda,tenda,TD,1000\n".repeat(1001))));
    }
    @Test void skipsExistingProductBeforeDownloadingImages() throws Exception {
        var products=mock(ProductRepository.class); var images=mock(RemoteImageImporter.class);
        var commands=mock(ProductCommandService.class);
        when(products.existsBySlug("tenda")).thenReturn(true);
        var rows=new ProductImportRowService(products,commands,images,mock(Validator.class));
        assertEquals("SKIPPED",rows.create(List.of(Map.of("name","Tenda","slug","tenda","sku","TD","image_urls","https://example.org/image.jpg"))));
        verifyNoInteractions(images,commands);
    }
    @Test void createsStockDailyRateAndLocallyStoredMedia() throws Exception {
        var images=mock(RemoteImageImporter.class); var commands=mock(ProductCommandService.class); var validator=mock(Validator.class);
        when(validator.validate(any(ProductCommand.class))).thenReturn(Set.of());
        when(images.download("https://example.org/tent.jpg")).thenReturn("http://localhost:8080/media/tenant/tent.png");
        var rows=new ProductImportRowService(mock(ProductRepository.class),commands,images,validator);
        rows.create(List.of(Map.of("name","Tenda","slug","tenda","sku","TD","stock","2","rental_fee","50000","image_urls","https://example.org/tent.jpg")));
        var capture=org.mockito.ArgumentCaptor.forClass(ProductCommand.class);
        verify(commands).create(capture.capture());
        var command=capture.getValue();
        assertEquals(ProductStatus.DRAFT,command.status());
        assertEquals("http://localhost:8080/media/tenant/tent.png",command.media());
        assertEquals(2,command.variants().getFirst().inventoryUnits().size());
        assertEquals(1,command.variants().getFirst().rentalPlans().getFirst().durationValue());
        assertEquals(new java.math.BigDecimal("50000"),command.variants().getFirst().rentalPlans().getFirst().rentalFee());
    }
    private Map<String,String> variant(String name,String sku,String fee,String image) {
        return Map.of("name","Tenda","slug","tenda","variant_name",name,"sku",sku,"rental_fee",fee,"image_urls",image);
    }
    @Test void groupsNonAdjacentVariantsAndKeepsOriginalRecordNumbers() {
        var groups=ProductImportService.group(List.of(Map.of("slug","tenda"),Map.of("slug","carrier"),Map.of("slug","tenda")));
        assertEquals(2,groups.size());
        assertEquals(List.of(2,4),groups.getFirst().recordNumbers());
        assertEquals(2,groups.getFirst().variants().size());
    }
    @Test void createsMultipleVariantsWithIndependentPricesAndSharedDownloadDeduplicated() throws Exception {
        var images=mock(RemoteImageImporter.class); var commands=mock(ProductCommandService.class); var validator=mock(Validator.class);
        when(validator.validate(any(ProductCommand.class))).thenReturn(Set.of());
        when(images.download("https://example.org/shared.jpg")).thenReturn("/media/local.png");
        var rows=new ProductImportRowService(mock(ProductRepository.class),commands,images,validator);
        rows.create(List.of(variant("2 Orang","TD-2","50000","https://example.org/shared.jpg"),variant("4 Orang","TD-4","75000","https://example.org/shared.jpg")));
        var capture=org.mockito.ArgumentCaptor.forClass(ProductCommand.class);
        verify(commands).create(capture.capture());
        var variants=capture.getValue().variants();
        assertEquals(List.of("2 Orang","4 Orang"),variants.stream().map(ProductVariantCommand::name).toList());
        assertEquals(new java.math.BigDecimal("75000"),variants.get(1).rentalPlans().getFirst().rentalFee());
        assertEquals(List.of("/media/local.png"),variants.get(1).mediaList());
        verify(images,times(1)).download(anyString());
    }
    @Test void rejectsDuplicateSkuMissingVariantNamesAndInconsistentProductMetadataBeforeDownloads() throws Exception {
        var images=mock(RemoteImageImporter.class); var commands=mock(ProductCommandService.class);
        var rows=new ProductImportRowService(mock(ProductRepository.class),commands,images,mock(Validator.class));
        var first=variant("2 Orang","TD-2","50000","");
        assertThrows(IllegalArgumentException.class,()->rows.create(List.of(first,variant("4 Orang","TD-2","75000",""))));
        assertThrows(IllegalArgumentException.class,()->rows.create(List.of(first,variant("","TD-4","75000",""))));
        var inconsistent=new java.util.HashMap<>(variant("4 Orang","TD-4","75000",""));
        inconsistent.put("name","Different product");
        assertThrows(IllegalArgumentException.class,()->rows.create(List.of(first,inconsistent)));
        verifyNoInteractions(images,commands);
    }
    @Test void checkedDownloadFailureOnLaterVariantRollsBackWholeProduct() throws Exception {
        var images=mock(RemoteImageImporter.class); var commands=mock(ProductCommandService.class); var validator=mock(Validator.class);
        when(validator.validate(any(ProductCommand.class))).thenReturn(Set.of());
        when(images.download("https://example.org/first.jpg")).thenReturn("/media/first.png");
        when(images.download("https://example.org/second.jpg")).thenThrow(new java.io.IOException("download failed"));
        var target=new ProductImportRowService(mock(ProductRepository.class),commands,images,validator);
        var manager=mock(org.springframework.transaction.PlatformTransactionManager.class);
        var transaction=mock(org.springframework.transaction.TransactionStatus.class);
        when(manager.getTransaction(any())).thenReturn(transaction);
        var proxyFactory=new org.springframework.aop.framework.ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        var interceptor = new org.springframework.transaction.interceptor.TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
        proxyFactory.addAdvice(interceptor);
        var proxy=(ProductImportRowService)proxyFactory.getProxy();
        assertThrows(java.io.IOException.class,()->proxy.create(List.of(variant("2 Orang","TD-2","50000","https://example.org/first.jpg"),variant("4 Orang","TD-4","75000","https://example.org/second.jpg"))));
        verify(manager).rollback(transaction);
        verify(manager,never()).commit(any());
        verifyNoInteractions(commands);
    }
}
