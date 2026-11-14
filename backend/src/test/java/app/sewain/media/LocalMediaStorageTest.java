package app.sewain.media;
import app.sewain.exception.MediaStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class LocalMediaStorageTest {
 @TempDir Path root;
 LocalMediaStorage storage(){return new LocalMediaStorage(new MediaProperties(root.toString(),"https://media.example.invalid"));}
 MockMultipartFile file(String value){return new MockMultipartFile("file","test.png","image/png",value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 @Test void storeReadUrlAndRepeatedDeleteUseSameObjectKey()throws Exception{var s=storage();s.store(file("original"),"tenant/image.png");assertEquals("original",s.read("tenant/image.png").getContentAsString(java.nio.charset.StandardCharsets.UTF_8));assertEquals("https://media.example.invalid/tenant/image.png",s.getUrl("tenant/image.png"));s.delete("tenant/image.png");s.delete("tenant/image.png");assertFalse(Files.exists(root.resolve("tenant/image.png")));}
 @Test void replacementRollbackRestoresOriginalAndCommitRemovesBackup()throws Exception{var s=storage();s.store(file("original"),"image.png");var replacement=s.replace(file("new"),"image.png");assertEquals("new",Files.readString(root.resolve("image.png")));replacement.complete(false);assertEquals("original",Files.readString(root.resolve("image.png")));s.replace(file("committed"),"image.png").complete(true);assertEquals("committed",Files.readString(root.resolve("image.png")));try(var files=Files.list(root)){assertEquals(1,files.count());}}
 @Test void readAndReplacementRejectTraversal(){assertThrows(MediaStorageException.class,()->storage().read("../outside"));assertThrows(MediaStorageException.class,()->storage().replace(file("x"),"../outside"));}
 @Test void failedTransferCleansStagingAndPreservesOriginal()throws Exception{var s=storage();s.store(file("original"),"image.png");var broken=mock(MultipartFile.class);doThrow(new IOException("failed")).when(broken).transferTo(any(Path.class));assertThrows(MediaStorageException.class,()->s.replace(broken,"image.png"));assertEquals("original",Files.readString(root.resolve("image.png")));try(var files=Files.list(root)){assertEquals(1,files.count());}}
 @Test void missingOriginalCannotBeReplaced(){assertThrows(MediaStorageException.class,()->storage().replace(file("new"),"missing.png"));}
 @Test void storeAndDeleteWrapIoFailures()throws Exception{var broken=mock(MultipartFile.class);doThrow(new IOException("disk error")).when(broken).transferTo(any(Path.class));assertThrows(MediaStorageException.class,()->storage().store(broken,"image.png"));Files.createDirectory(root.resolve("directory"));Files.writeString(root.resolve("directory/child"),"data");assertThrows(MediaStorageException.class,()->storage().delete("directory"));}
}
