package app.sewain.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.List;

interface RentalOrderRepository extends JpaRepository<RentalOrder, UUID>, JpaSpecificationExecutor<RentalOrder> {
	@Query("""
			select new app.sewain.orders.MerchantOrderCalendarEntry(
				o.id, o.orderNumber, o.customerName, o.status, o.fulfillmentType,
				case when o.fulfillmentType = :pickup then o.pickupAt else o.deliveryAt end,
				o.returnAt, i.productName, i.variantName, 1, a.inventoryUnitId,
				a.rentalStartAt, a.rentalEndAt, a.status)
			from RentalAllocation a join a.orderItem i join i.order o
			where a.status <> :released and o.status <> :cancelled and o.status <> :expired
			and ((a.rentalStartAt < :toAt and a.rentalEndAt > :fromAt)
				or (case when o.fulfillmentType = :pickup then o.pickupAt else o.deliveryAt end >= :fromAt
				and case when o.fulfillmentType = :pickup then o.pickupAt else o.deliveryAt end < :toAt)
				or (o.returnAt >= :fromAt and o.returnAt < :toAt))
			order by a.rentalStartAt, o.orderNumber
			""")
	List<MerchantOrderCalendarEntry> findCalendarEntries(@Param("fromAt") OffsetDateTime fromAt,
			@Param("toAt") OffsetDateTime toAt, @Param("pickup") FulfillmentType pickup,
			@Param("cancelled") OrderStatus cancelled, @Param("expired") OrderStatus expired,
			@Param("released") RentalAllocationStatus released);
}
