package app.sewain.catalogs;

import app.sewain.media.RemoteImageImporter;
import app.sewain.shared.DurationUnit;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
class ProductImportRowService {
    private final ProductRepository products;
    private final ProductCommandService commands;
    private final RemoteImageImporter images;
    private final Validator validator;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String create(List<Map<String,String>> rows) throws Exception {
        if (rows.isEmpty()) throw new IllegalArgumentException("Produk tidak memiliki varian");
        var first = rows.getFirst();
        String name = required(first,"name",255), slug = required(first,"slug",255);
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) throw new IllegalArgumentException("slug hanya huruf kecil, angka, dan tanda hubung");
        if (products.existsBySlug(slug)) return "SKIPPED";
        var description = first.getOrDefault("description", "");
        var statusText = first.getOrDefault("status", "DRAFT");
        ProductStatus status;
        try { status = ProductStatus.valueOf(statusText); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("status harus DRAFT, ACTIVE, atau UNLISTED"); }
        var variants = new ArrayList<ProductVariantCommand>();
        var imageSources = new ArrayList<List<String>>();
        int totalStock = 0;
        var skus = new HashSet<String>();
        var names = new HashSet<String>();
        // Validate every variant before downloading anything for this product.
        for (var row : rows) {
            if (!slug.equals(row.get("slug")) || !name.equals(row.get("name"))
                    || !description.equals(row.getOrDefault("description", ""))
                    || !statusText.equals(row.getOrDefault("status", "DRAFT")))
                throw new IllegalArgumentException("name, slug, description, dan status harus sama untuk semua varian produk");
            var sku = required(row,"sku",90);
            var variantName = rows.size() == 1 ? row.getOrDefault("variant_name", "Standar") : required(row,"variant_name",255);
            if (variantName.isBlank() || variantName.length()>255) throw new IllegalArgumentException("variant_name maksimal 255 karakter");
            if (!skus.add(sku)) throw new IllegalArgumentException("SKU varian harus unik dalam satu produk");
            if (!names.add(variantName.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Nama varian harus unik dalam satu produk");
            int stock;
            try { stock = Integer.parseInt(row.getOrDefault("stock", "0")); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("stock harus angka bulat"); }
            if (stock < 0 || stock > 1000) throw new IllegalArgumentException("stock harus 0–1000 per varian");
            totalStock += stock;
            if (totalStock > 1000) throw new IllegalArgumentException("Total stok semua varian maksimal 1000 per produk");
            var units = new ArrayList<InventoryUnitCommand>();
            for (int i=1;i<=stock;i++) units.add(new InventoryUnitCommand(null,sku+"-"+i,null,InventoryUnitStatus.AVAILABLE,null));
            var urls = row.getOrDefault("image_urls", "").isBlank() ? List.<String>of() : Arrays.stream(row.get("image_urls").split("\\|",-1)).map(String::trim).toList();
            if (urls.size() > 5 || urls.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Maksimal 5 URL gambar per varian, dipisahkan | tanpa URL kosong");
            imageSources.add(urls);
            var rental = new VariantRentalPlanCommand(DurationUnit.DAY,1,amount(row,"rental_fee",true),true);
            variants.add(new ProductVariantCommand(null,variantName,sku,List.of(),amount(row,"buy_price",false),amount(row,"deposit_amount",false),true,List.of(),List.of(rental),units));
        }
        var command = new ProductCommand(name,slug,null,description,status,null,List.of(),variants);
        var violations = validator.validate(command);
        if (!violations.isEmpty()) throw new IllegalArgumentException("Data produk tidak valid: " + violations.iterator().next().getPropertyPath());
        var downloaded = new HashMap<String,String>();
        String cover = null;
        for (int i=0;i<variants.size();i++) {
            var variant = variants.get(i);
            var media = new ArrayList<String>();
            for (var source : imageSources.get(i)) {
                var local = downloaded.get(source);
                if (local == null) { local = images.download(source); downloaded.put(source,local); }
                media.add(local);
                if (cover == null) cover = local;
            }
            variants.set(i,new ProductVariantCommand(null,variant.name(),variant.sku(),media,variant.buyPrice(),variant.depositAmount(),true,List.of(),variant.rentalPlans(),variant.inventoryUnits()));
        }
        commands.create(new ProductCommand(name,slug,cover,command.description(),status,null,List.of(),variants));
        return "SUCCEEDED";
    }
    private static String required(Map<String,String> row,String key,int max) {
        var value = row.getOrDefault(key,"");
        if (value.isBlank() || value.length()>max) throw new IllegalArgumentException(key+" wajib diisi, maksimal "+max+" karakter");
        return value;
    }
    private static BigDecimal amount(Map<String,String> row,String key,boolean required) {
        var value = row.getOrDefault(key,"");
        if (value.isBlank() && !required) return BigDecimal.ZERO;
        try {
            var number = new BigDecimal(value);
            if (number.signum()<0 || number.compareTo(new BigDecimal("9999999999.99"))>0 || number.scale()>2) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException error) { throw new IllegalArgumentException(key+" harus nominal positif, tanpa pemisah ribuan, maksimal 2 desimal"); }
    }
}
