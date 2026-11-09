package app.sewain.catalogs;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
        assertEquals("SKIPPED",rows.create(Map.of("name","Tenda","slug","tenda","sku","TD","image_urls","https://example.org/image.jpg")));
        verifyNoInteractions(images,commands);
    }
    @Test void createsStockDailyRateAndLocallyStoredMedia() throws Exception {
        var images=mock(RemoteImageImporter.class); var commands=mock(ProductCommandService.class); var validator=mock(Validator.class);
        when(validator.validate(any(ProductCommand.class))).thenReturn(Set.of());
        when(images.download("https://example.org/tent.jpg")).thenReturn("http://localhost:8080/media/tenant/tent.png");
        var rows=new ProductImportRowService(mock(ProductRepository.class),commands,images,validator);
        rows.create(Map.of("name","Tenda","slug","tenda","sku","TD","stock","2","rental_fee","50000","image_urls","https://example.org/tent.jpg"));
        var capture=org.mockito.ArgumentCaptor.forClass(ProductCommand.class);
        verify(commands).create(capture.capture());
        var command=capture.getValue();
        assertEquals(ProductStatus.DRAFT,command.status());
        assertEquals("http://localhost:8080/media/tenant/tent.png",command.media());
        assertEquals(2,command.variants().getFirst().inventoryUnits().size());
        assertEquals(1,command.variants().getFirst().rentalPlans().getFirst().durationValue());
        assertEquals(new java.math.BigDecimal("50000"),command.variants().getFirst().rentalPlans().getFirst().rentalFee());
    }
}
