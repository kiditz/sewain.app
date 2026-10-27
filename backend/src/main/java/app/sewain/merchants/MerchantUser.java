package app.sewain.merchants;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "merchant_users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantUser {

    @EmbeddedId
    private MerchantUserId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MerchantRole role;
}