package app.sewain.merchants;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
class BusinessInformation {

    @Column(name = "business_name")
    private String name;

    @Column(name = "business_address", columnDefinition = "text")
    private String address;

    @Column(name = "business_email")
    private String email;

    @Column(name = "business_phone")
    private String phone;

    @Column(name = "tax_id")
    private String taxId;
}