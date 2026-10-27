package app.sewain.merchants;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record MerchantUserId(
    UUID merchantId,
    UUID userId
) implements Serializable {
}