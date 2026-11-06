package app.sewain.shared;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.util.*;

/** A small, shared policy for stored rich text and navigation URLs, including legacy reads. */
public final class ContentSecurity {
    private ContentSecurity() {}
    private static final Set<String> RICH_LABEL_BLOCKS=Set.of("BUTTON","POLICY_LINK","POPUP_LINK","CART_CHECKOUT","ADD_TO_CART","BUY_BUTTONS","CHECKOUT_SUBMIT","DEPOSIT_INFO","QUANTITY_SELECTOR","PRICE_SUMMARY");
    private static final Safelist RICH_TEXT=new Safelist()
        .addTags("p","h1","h2","h3","h4","h5","h6","strong","b","em","i","u","s","ol","ul","li","blockquote","pre","code","br","span","a")
        .addAttributes(":all","class").addAttributes("li","data-list")
        .addAttributes("a","href","title");
    public static String cleanHtml(String input) {
        if(input==null)return null;
        var document=Jsoup.parseBodyFragment(Jsoup.clean(input,"",RICH_TEXT,new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false)));
        for(var link:document.select("a[href]")){
            var safe=navigationUrl(link.attr("href"));
            if(safe.isEmpty())link.removeAttr("href");else link.attr("href",safe);
        }
        for(var element:document.select("[class]")){
            var classes=element.classNames().stream().filter(value->value.matches("ql-(?:align-(?:center|right|justify)|indent-[1-8]|direction-rtl|syntax|code-block|code-block-container|ui)")).toList();
            if(classes.isEmpty())element.removeAttr("class");else element.attr("class",String.join(" ",classes));
        }
        for(var item:document.select("[data-list]"))if(!Set.of("ordered","bullet").contains(item.attr("data-list")))item.removeAttr("data-list");
        document.outputSettings().prettyPrint(false);
        return document.body().html();
    }
    public static String navigationUrl(String input) {
        if(input==null)return "";
        var value=input.trim();
        if(value.isEmpty()||value.chars().anyMatch(c->c<=32||c==127)||value.indexOf('\\')>=0)return "";
        if(value.startsWith("/")&&!value.startsWith("//"))return value;
        if(value.startsWith("#")||value.startsWith("?"))return value;
        try{
            var uri=URI.create(value);
            if(uri.getScheme()!=null&&Set.of("http","https").contains(uri.getScheme().toLowerCase(Locale.ROOT))&&uri.getRawAuthority()!=null&&!uri.getRawAuthority().isBlank())return value;
        }catch(IllegalArgumentException ignored){}
        return "";
    }
    public static JsonNode cleanConfiguration(JsonNode input) {
        if(input==null)return null;
        var result=input.deepCopy();cleanNode(result,"",false);return result;
    }
    private static void cleanNode(JsonNode node,String blockType,boolean inSettings) {
        if(node.isObject()){
            var object=node.asObject();
            var type=object.path("type").asString(blockType);
            // Collect before replacing fields so Jackson iterators remain valid.
            var names=new ArrayList<String>();object.properties().forEach(entry->names.add(entry.getKey()));
            for(var key:names){
                var value=object.get(key);
                if(value.isString()){
                    if(inSettings&&(key.equals("text")||(key.equals("label")&&RICH_LABEL_BLOCKS.contains(type))))object.put(key,cleanHtml(value.asString()));
                    else if(isNavigationField(key))object.put(key,navigationUrl(value.asString()));
                }else cleanNode(value,type,inSettings||key.equals("settings"));
            }
        }else if(node.isArray())for(var child:node)cleanNode(child,blockType,inSettings);
    }
    private static boolean isNavigationField(String key) {
        if(key.matches("(?i).*(?:image|media|logo|favicon|poster|thumbnail)(?:url|src|source)?$"))return false;
        return key.matches("(?i).*(?:url|href|link|linkTo|destination|redirectTo|route|path)$");
    }
}
