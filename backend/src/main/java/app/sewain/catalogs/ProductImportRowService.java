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
    public String create(Map<String,String> row) throws Exception {
        String name = required(row,"name",255), slug = required(row,"slug",255), sku = required(row,"sku",90);
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) throw new IllegalArgumentException("slug hanya huruf kecil, angka, dan tanda hubung");
        if (products.existsBySlug(slug)) return "SKIPPED";
        int stock;
        try { stock = Integer.parseInt(row.getOrDefault("stock", "0")); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("stock harus angka bulat"); }
        if (stock < 0 || stock > 1000) throw new IllegalArgumentException("stock harus 0–1000");
        var units = new ArrayList<InventoryUnitCommand>();
        for (int i=1;i<=stock;i++) units.add(new InventoryUnitCommand(null,sku+"-"+i,null,InventoryUnitStatus.AVAILABLE,null));
        var urls = row.getOrDefault("image_urls", "").isBlank() ? List.<String>of() : Arrays.stream(row.get("image_urls").split("\\|",-1)).map(String::trim).toList();
        if (urls.size() > 5 || urls.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Maksimal 5 URL gambar, dipisahkan | tanpa URL kosong");
        var status = ProductStatus.valueOf(row.getOrDefault("status","DRAFT"));
        var rental = new VariantRentalPlanCommand(DurationUnit.DAY,1,amount(row,"rental_fee",true),true);
        var variant = new ProductVariantCommand(null,"Standar",sku,List.of(),amount(row,"buy_price",false),amount(row,"deposit_amount",false),true,List.of(),List.of(rental),units);
        var command = new ProductCommand(name,slug,null,row.getOrDefault("description",""),status,null,List.of(),List.of(variant));
        var violations = validator.validate(command);
        if (!violations.isEmpty()) throw new IllegalArgumentException("Data produk tidak valid: " + violations.iterator().next().getPropertyPath());
        var media = new ArrayList<String>();
        for (var url : urls) media.add(images.download(url));
        variant = new ProductVariantCommand(null,"Standar",sku,media,variant.buyPrice(),variant.depositAmount(),true,List.of(),List.of(rental),units);
        commands.create(new ProductCommand(name,slug,media.isEmpty()?null:media.getFirst(),command.description(),status,null,List.of(),List.of(variant)));
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
