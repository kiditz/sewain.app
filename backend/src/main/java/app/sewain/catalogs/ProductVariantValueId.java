package app.sewain.catalogs;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ProductVariantValueId implements Serializable {

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "attribute_id")
    private UUID attributeId;
}