package app.sewain.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RemoteImageDownloadTest {
    final MediaCommandService media=mock(MediaCommandService.class);
    final RestClient.Builder builder=mock(RestClient.Builder.class);
    final RestClient.Builder http=RestClient.builder();
    final MockRestServiceServer server=MockRestServiceServer.bindTo(http).build();
    final RemoteImageImporter importer=new RemoteImageImporter(media,builder);
    @TempDir Path temp;
    RemoteImageDownloadTest(){when(builder.clone()).thenReturn(builder);when(builder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(builder);when(builder.build()).thenReturn(http.build());}
    @Test void downloadUploadsBoundedBytesAndMultipartFileProvidesDefensiveCopy()throws Exception {
        server.expect(requestTo("https://8.8.8.8/image.png")).andRespond(withSuccess(new byte[]{1,2,3},MediaType.IMAGE_PNG));
        var response=mock(MediaResponse.class);when(response.url()).thenReturn("/media/saved.png");when(media.upload(anyList(),anyList())).thenReturn(List.of(response));
        assertEquals("/media/saved.png",importer.download("https://8.8.8.8/image.png"));
        var capture=org.mockito.ArgumentCaptor.forClass(List.class);verify(media).upload(capture.capture(),eq(List.of()));
        var file=(MultipartFile)capture.getValue().getFirst();assertEquals("file",file.getName());assertEquals("import-image",file.getOriginalFilename());
        assertEquals("application/octet-stream",file.getContentType());assertEquals(3,file.getSize());assertFalse(file.isEmpty());
        var bytes=file.getBytes();bytes[0]=99;assertEquals(1,file.getBytes()[0]);assertArrayEquals(new byte[]{1,2,3},file.getInputStream().readAllBytes());
        var destination=temp.resolve("download.bin");file.transferTo(destination.toFile());assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(destination));server.verify();
    }
    @Test void nonSuccessHttpStatusAndOversizedBodyNeverReachMediaUpload() {
        server.expect(requestTo("https://8.8.8.8/missing")).andRespond(withResourceNotFound());
        assertThrows(IOException.class,()->importer.download("https://8.8.8.8/missing"));verifyNoInteractions(media);server.verify();
    }
    @Test void bodySizeLimitIsEnforcedBeforeUpload() {
        server.expect(requestTo("https://8.8.8.8/large")).andRespond(withSuccess(new byte[5*1024*1024+1],MediaType.IMAGE_PNG));
        assertThrows(IOException.class,()->importer.download("https://8.8.8.8/large"));verifyNoInteractions(media);server.verify();
    }
    @Test void malformedSchemeCredentialsHostAndPortAreRejectedWithoutTransport() {
        for(String url:List.of("not a uri","file:///image.png","https:///missing","https://user:password@8.8.8.8/x","https://8.8.8.8:444/x"))
            assertThrows(IOException.class,()->importer.download(url));
        verifyNoInteractions(builder,media);
    }
}
