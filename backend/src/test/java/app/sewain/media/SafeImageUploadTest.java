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
class SafeImageUploadTest {
 @TempDir Path directory;
 byte[] png(int width,int height)throws Exception{var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB),"png",output);return output.toByteArray();}
 @Test void rejectsSpoofedHtmlCorruptAndOversizedRaster()throws Exception{
  for(var content:List.of("<html><script>x()</script></html>","not an image"))assertThrows(app.sewain.exception.ValidationException.class,()->SafeImageUpload.normalize(new MockMultipartFile("file","image.png","image/png",content.getBytes()),false));
  assertThrows(app.sewain.exception.ValidationException.class,()->SafeImageUpload.normalize(new MockMultipartFile("file","large.png","image/png",png(9000,1)),false));
 }
 @Test void preservesStaticSvgAndServesValidatedSvg()throws Exception{
  var svg="<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'><defs><linearGradient id='paint'><stop offset='0%' stop-color='#fff'/></linearGradient></defs><path d='M0 0L24 24' fill='url(#paint)' transform='translate(1 2) scale(0.5)'/></svg>";
  var normalized=SafeImageUpload.normalize(new MockMultipartFile("file","../../icon.svg","image/svg+xml",svg.getBytes()),false);
  assertEquals("icon.svg",normalized.getOriginalFilename());assertEquals("image/svg+xml",normalized.getContentType());
  assertTrue(new String(normalized.getBytes()).contains("<path"));
  assertThrows(app.sewain.exception.ValidationException.class,()->SafeImageUpload.normalize(new MockMultipartFile("file","icon.svg","image/svg+xml",svg.getBytes()),true));
  Files.write(directory.resolve("icon.svg"),normalized.getBytes());
  var resource=new MediaResourceResolver().getResource("icon.svg",new UrlResource(directory.toUri()));
  assertNotNull(resource);assertEquals("icon.svg",resource.getFilename());assertTrue(resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("<path"));
  assertEquals(Files.getLastModifiedTime(directory.resolve("icon.svg")).toMillis(),resource.lastModified());
 }
 @Test void preservesExportedLogoStylesAndSvg()throws Exception{
  try(var input=getClass().getResourceAsStream("/security/exported-logo.svg")){
   var normalized=SafeImageUpload.normalize(new MockMultipartFile("file","logo.svg","image/svg+xml",input.readAllBytes()),false);
   assertEquals("logo.svg",normalized.getOriginalFilename());
   assertEquals("image/svg+xml",normalized.getContentType());
   var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
   factory.setNamespaceAware(true);
   var document=factory.newDocumentBuilder().parse(normalized.getInputStream());
   var polygons=document.getElementsByTagNameNS("http://www.w3.org/2000/svg","polygon");
   assertEquals("st0",((org.w3c.dom.Element)polygons.item(0)).getAttribute("class"));
   assertEquals("st1",((org.w3c.dom.Element)polygons.item(1)).getAttribute("class"));
   var css=document.getElementsByTagNameNS("http://www.w3.org/2000/svg","style").item(0).getTextContent();
   assertTrue(css.contains("#323F45"));assertTrue(css.contains("#1C262B"));assertTrue(css.contains("fill-rule:evenodd"));
  }
 }
 @Test void sanitizesScriptsEventsEmbeddedHtmlAndExternalLinks()throws Exception{
  var svg="<svg xmlns='http://www.w3.org/2000/svg' onload='alert(1)'><script>alert(1)</script><foreignObject><div xmlns='http://www.w3.org/1999/xhtml'>bad</div></foreignObject><use href='https://example.com/x.svg#x'/><path d='M0 0L1 1' onclick='alert(1)'/></svg>";
  var normalized=SafeImageUpload.normalize(new MockMultipartFile("file","image.svg","image/svg+xml",svg.getBytes()),false);
  var clean=new String(normalized.getBytes(),java.nio.charset.StandardCharsets.UTF_8);
  assertFalse(clean.contains("<script"));assertFalse(clean.contains("onload"));assertFalse(clean.contains("onclick"));
  assertFalse(clean.contains("foreignObject"));assertFalse(clean.contains("https://example.com"));assertTrue(clean.contains("<path"));
  Files.writeString(directory.resolve("legacy.svg"),svg);
  var resource=new MediaResourceResolver().getResource("legacy.svg",new UrlResource(directory.toUri()));
  assertNotNull(resource);assertFalse(resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("<script"));
 }
 @Test void sandboxesBothMediaDeliveryRoutes()throws Exception{
  var registry=mock(org.springframework.web.servlet.config.annotation.InterceptorRegistry.class);
  var registration=mock(org.springframework.web.servlet.config.annotation.InterceptorRegistration.class);
  var interceptor=org.mockito.ArgumentCaptor.forClass(org.springframework.web.servlet.HandlerInterceptor.class);
  when(registry.addInterceptor(interceptor.capture())).thenReturn(registration);
  new MediaWebConfiguration(null).addInterceptors(registry);
  verify(registration).addPathPatterns("/media/**","/api/media/*/file");
  var response=new org.springframework.mock.web.MockHttpServletResponse();
  assertTrue(interceptor.getValue().preHandle(new org.springframework.mock.web.MockHttpServletRequest(),response,new Object()));
  var policy=response.getHeader("Content-Security-Policy");
  assertNotNull(policy);assertTrue(policy.contains("sandbox;"));assertTrue(policy.contains("default-src 'none'"));
  assertTrue(policy.contains("style-src 'unsafe-inline'"));assertFalse(policy.contains("allow-scripts"));
  assertEquals("nosniff",response.getHeader("X-Content-Type-Options"));
 }
 @Test void derivesCanonicalPngAndWebpOutputFromDecodedBytes()throws Exception{
  var normalized=SafeImageUpload.normalize(new MockMultipartFile("file","../../evil.html","text/html",png(2,2)),false);
  assertEquals("evil.png",normalized.getOriginalFilename());assertEquals("image/png",normalized.getContentType());assertEquals(2,ImageIO.read(normalized.getInputStream()).getWidth());
  try(var input=getClass().getResourceAsStream("/security/valid.webp")){
   var webp=new MockMultipartFile("file","picture.webp","image/webp",input.readAllBytes());var image=SafeImageUpload.normalize(webp,false);assertEquals("image/png",image.getContentType());assertEquals(3,ImageIO.read(image.getInputStream()).getWidth());
   assertThrows(app.sewain.exception.ValidationException.class,()->SafeImageUpload.normalize(webp,true));
  }
 }
 @Test void validatesTheEntireBatchBeforeWritingAndBlocksLegacyExecutableExtensions()throws Exception{
  var storage=mock(MediaStorage.class);var commands=new MediaCommandServiceImpl(mock(MediaRepository.class),storage,mock(MediaMapper.class));
  assertThrows(app.sewain.exception.ValidationException.class,()->commands.upload(List.of(new MockMultipartFile("file","valid.png","image/png",png(2,2)),new MockMultipartFile("file","bad.html","image/png","<html>x</html>".getBytes())),List.of()));verifyNoInteractions(storage);
  Files.writeString(directory.resolve("legacy.html"),"<html>x</html>");Files.writeString(directory.resolve("legacy.svg"),"<svg/>");var resolver=new MediaResourceResolver();var root=new UrlResource(directory.toUri());assertNull(resolver.getResource("legacy.html",root));assertNull(resolver.getResource("legacy.svg",root));
 }
}
