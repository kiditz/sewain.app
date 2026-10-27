package app.sewain.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface CartItemRepository extends JpaRepository<CartItem, UUID> {
	@Query("""
    SELECT
        ci.id AS id,
        ci.productId AS productId,
        p.name AS productName,
        p.slug AS productSlug,
        pv.mediaList AS mediaList,
        ci.variantId AS variantId,
        pv.name AS variantName,
        ci.quantity AS quantity,
        ci.rentalStartAt AS rentalStartAt,
        ci.rentalEndAt AS rentalEndAt,
        ci.rentalPlans AS rentalPlans,
        ci.rentalSubtotal AS rentalSubtotal,
        ci.depositSubtotal AS depositSubtotal,
        ci.totalAmount AS totalAmount,
        ci.createdAt AS createdAt,
        ci.updatedAt AS updatedAt
    FROM CartItem ci, Product p, ProductVariant pv
    WHERE ci.productId = p.id
      AND ci.variantId = pv.id
      AND ci.cart.id = :cartId
    ORDER BY ci.createdAt
    """)
	List<CartItemProjection> findProjectionsByCartId(
			@Param("cartId") UUID cartId
	);
}