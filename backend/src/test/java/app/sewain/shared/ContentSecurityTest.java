package app.sewain.shared;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
class ContentSecurityTest {
 @Test void preservesSupportedRichTextAndRejectsExecutableMarkup(){
  var clean=ContentSecurity.cleanHtml("<p class='ql-align-center arbitrary'><strong>Hello</strong><img src=x onerror=alert(1)><svg><script>alert(1)</script></svg><a href='jav&#x61;script:alert(1)' onclick='x()'>Link</a></p>");
  assertTrue(clean.contains("<strong>Hello</strong>"));assertTrue(clean.contains("ql-align-center"));assertFalse(clean.contains("onerror"));assertFalse(clean.contains("onclick"));assertFalse(clean.contains("<svg"));assertFalse(clean.contains("<script"));assertFalse(clean.contains("javascript"));assertFalse(clean.contains("arbitrary"));
 }
 @Test void validatesNavigationAndCleansNestedLegacyConfigurationWithoutMutatingInput(){
  for(var value:new String[]{"javascript:alert(1)","JaVaScRiPt:x", "java\nscript:x", "//evil.test", "/\\evil.test","data:text/html,x"})assertEquals("",ContentSecurity.navigationUrl(value));
  for(var value:new String[]{"/products/camera","https://shop.test/path","#details","?page=2"})assertEquals(value,ContentSecurity.navigationUrl(value));
  var input=JsonMapper.builder().build().readTree("{\"title\":\"A & B\",\"blocks\":[{\"type\":\"TEXT\",\"settings\":{\"text\":\"<p onclick='x()'>OK</p>\",\"url\":\"javascript:x\",\"src\":\"/image.png\"}}]}");
  var clean=ContentSecurity.cleanConfiguration(input);assertTrue(input.toString().contains("onclick"));assertEquals("A & B",clean.path("title").asString());assertFalse(clean.toString().contains("onclick"));assertEquals("",clean.path("blocks").get(0).path("settings").path("url").asString());assertEquals("/image.png",clean.path("blocks").get(0).path("settings").path("src").asString());
 }
}
