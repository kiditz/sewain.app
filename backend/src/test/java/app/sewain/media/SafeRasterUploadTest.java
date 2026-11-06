package app.sewain.media;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.UrlResource;
import java.io.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class SafeRasterUploadTest {
 @TempDir Path directory;
 byte[] png(int width,int height)throws Exception{var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB),"png",output);return output.toByteArray();}
 @Test void rejectsSpoofedHtmlSvgCorruptAndOversizedRaster()throws Exception{
  for(var content:List.of("<html><script>x()</script></html>","<svg xmlns='http://www.w3.org/2000/svg' onload='x()'/>","not an image"))assertThrows(app.sewain.exception.ValidationException.class,()->SafeRasterUpload.normalize(new MockMultipartFile("file","image.png","image/png",content.getBytes()),false));
  assertThrows(app.sewain.exception.ValidationException.class,()->SafeRasterUpload.normalize(new MockMultipartFile("file","large.png","image/png",png(9000,1)),false));
 }
 @Test void derivesCanonicalPngAndWebpOutputFromDecodedBytes()throws Exception{
  var normalized=SafeRasterUpload.normalize(new MockMultipartFile("file","../../evil.html","text/html",png(2,2)),false);
  assertEquals("evil.png",normalized.getOriginalFilename());assertEquals("image/png",normalized.getContentType());assertEquals(2,ImageIO.read(normalized.getInputStream()).getWidth());
  try(var input=getClass().getResourceAsStream("/security/valid.webp")){
   var webp=new MockMultipartFile("file","picture.webp","image/webp",input.readAllBytes());var image=SafeRasterUpload.normalize(webp,false);assertEquals("image/png",image.getContentType());assertEquals(3,ImageIO.read(image.getInputStream()).getWidth());
   assertThrows(app.sewain.exception.ValidationException.class,()->SafeRasterUpload.normalize(webp,true));
  }
 }
 @Test void validatesTheEntireBatchBeforeWritingAndBlocksLegacyExecutableExtensions()throws Exception{
  var storage=mock(MediaStorage.class);var commands=new MediaCommandServiceImpl(mock(MediaRepository.class),storage,mock(MediaMapper.class));
  assertThrows(app.sewain.exception.ValidationException.class,()->commands.upload(List.of(new MockMultipartFile("file","valid.png","image/png",png(2,2)),new MockMultipartFile("file","bad.html","image/png","<html>x</html>".getBytes())),List.of()));verifyNoInteractions(storage);
  Files.writeString(directory.resolve("legacy.html"),"<html>x</html>");Files.writeString(directory.resolve("legacy.svg"),"<svg/>");var resolver=new MediaResourceResolver();var root=new UrlResource(directory.toUri());assertNull(resolver.getResource("legacy.html",root));assertNull(resolver.getResource("legacy.svg",root));
 }
}
