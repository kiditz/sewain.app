package app.sewain.orders;

import app.sewain.exception.ConflictException;
import app.sewain.shared.MerchantOrderActivityEvent;
import app.sewain.tenant.TenantContext;
import app.sewain.testsupport.JpaIntegrationSupport;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class OrderReservationIntegrationTest extends JpaIntegrationSupport {
    @Override
    protected void customizeContext(AnnotationConfigApplicationContext context) {
        context.registerBean(CreationProbe.class,
                () -> new CreationProbe(context.getBean(EntityManagerFactory.class)));
    }

    @Test
    void overlappingRequestsForLastUnitAllowExactlyOneCommittedOrder() throws Exception {
        var fixture = catalogFixture("Last tent", "AVAILABLE");
        var command = command(List.of(item(fixture, 1)));
        var service = context.getBean(MerchantOrderCreationService.class);
        var probe = context.getBean(CreationProbe.class);
        probe.blockFirst.set(true);
        var executor = Executors.newSingleThreadExecutor();
        var winner = executor.submit(() -> {
            TenantContext.set(tenant);
            try { return service.create(command); }
            finally { TenantContext.clear(); }
        });
        try {
            assertTrue(probe.writesFlushed.await(15, TimeUnit.SECONDS), "First request must hold its inventory lock");
            try {
                assertThrows(ConflictException.class, () -> service.create(command));
            } finally {
                probe.allowCommit.countDown();
            }
            var response = winner.get(15, TimeUnit.SECONDS);
            assertNotNull(response.id());
            assertEquals(1, count("orders"));
            assertEquals(1, count("order_items"));
            assertEquals(1, count("payments"));
            assertEquals(1, count("rental_allocations"));
            assertEquals("HELD", jdbc.queryForObject("SELECT status FROM " + tenant + ".rental_allocations", String.class));
            // A later request also fails after the winning allocation has committed.
            assertThrows(ConflictException.class, () -> service.create(command));
            assertEquals(1, count("orders"));
        } finally {
            probe.allowCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS), "Reservation worker must terminate");
        }
    }

    @Test
    void unavailableSecondItemRollsBackOrderFirstItemAllocationAndPayment() {
        var available = catalogFixture("Available tent", "AVAILABLE");
        var unavailable = catalogFixture("Broken tent", "MAINTENANCE");
        var service = context.getBean(MerchantOrderCreationService.class);

        assertThrows(ConflictException.class, () -> service.create(
                command(List.of(item(available, 1), item(unavailable, 1)))));

        assertNoOrderWrites();
        assertEquals(2, count("products"), "Existing catalog must not be rolled back");
        // The first item's lock/allocation must be reusable after the failed order.
        assertNotNull(service.create(command(List.of(item(available, 1)))).id());
        assertEquals(1, count("rental_allocations"));
    }

    @Test
    void failureAfterFlushingPaymentRollsBackAllOrderWritesAndReleasesStock() {
        var fixture = catalogFixture("Tent", "AVAILABLE");
        var probe = context.getBean(CreationProbe.class);
        probe.fail.set(true);
        var service = context.getBean(MerchantOrderCreationService.class);
        var command = command(List.of(item(fixture, 1)));

        assertThrows(SimulatedFailure.class, () -> service.create(command));

        assertTrue(probe.sawAllWrites.get(), "Failure must occur after real order/item/allocation/payment inserts");
        assertNoOrderWrites();
        probe.fail.set(false);
        assertNotNull(service.create(command).id());
        assertEquals(1, count("orders"));
        assertEquals(1, count("payments"));
    }

    @Test
    void initialPaymentPersistsCashAllocationAndConfirmsHeldInventory() {
        var fixture = catalogFixture("Paid tent", "AVAILABLE");
        var order = context.getBean(MerchantOrderCreationService.class).create(command(List.of(item(fixture, 1))));

        var detail = context.getBean(MerchantOrderPaymentService.class).recordPayment(order.id(),
                new RecordOrderPaymentCommand(PaymentStage.INITIAL, new BigDecimal("250000"), PaymentMethod.CASH, "RECEIPT-1"));

        assertEquals(OrderStatus.CONFIRMED, detail.status());
        assertEquals(0, new BigDecimal("250000").compareTo(detail.totalPaidAmount()));
        assertEquals("SUCCEEDED", jdbc.queryForObject("SELECT status FROM " + tenant + ".payments", String.class));
        assertEquals(0, new BigDecimal("200000").compareTo(jdbc.queryForObject(
                "SELECT rental_amount FROM " + tenant + ".payments", BigDecimal.class)));
        assertEquals(0, new BigDecimal("50000").compareTo(jdbc.queryForObject(
                "SELECT deposit_amount FROM " + tenant + ".payments", BigDecimal.class)));
        assertEquals("CONFIRMED", jdbc.queryForObject("SELECT status FROM " + tenant + ".rental_allocations", String.class));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM " + tenant
                + ".rental_allocations WHERE hold_expires_at IS NOT NULL", Long.class));
    }

    @Test
    void paymentFailureRollsBackReceiptOrderStatusAndAllocationConfirmation() {
        var fixture = catalogFixture("Tent", "AVAILABLE");
        var order = context.getBean(MerchantOrderCreationService.class).create(command(List.of(item(fixture, 1))));
        var probe = context.getBean(CreationProbe.class);
        probe.failPayment.set(true);

        assertThrows(SimulatedFailure.class, () -> context.getBean(MerchantOrderPaymentService.class).recordPayment(order.id(),
                new RecordOrderPaymentCommand(PaymentStage.INITIAL, new BigDecimal("250000"), PaymentMethod.CASH, "RECEIPT-FAIL")));

        assertEquals("PENDING_PAYMENT", jdbc.queryForObject("SELECT status FROM " + tenant + ".orders", String.class));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM " + tenant + ".payments", String.class));
        assertEquals("HELD", jdbc.queryForObject("SELECT status FROM " + tenant + ".rental_allocations", String.class));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM " + tenant + ".payments WHERE paid_at IS NOT NULL", Long.class));
        assertEquals(0, jdbc.queryForObject("SELECT rental_amount FROM " + tenant + ".payments", BigDecimal.class).signum());
    }

    @Test
    void refundFailureRollsBackBothRefundAndPaymentStatusThenRetryCommitsOnce() {
        var fixture = catalogFixture("Refund tent", "AVAILABLE");
        var order = context.getBean(MerchantOrderCreationService.class).create(command(List.of(item(fixture, 1))));
        var payments = context.getBean(MerchantOrderPaymentService.class);
        payments.recordPayment(order.id(), new RecordOrderPaymentCommand(
                PaymentStage.INITIAL, new BigDecimal("250000"), PaymentMethod.CASH, "RECEIPT-REFUND"));
        var paymentId = jdbc.queryForObject("SELECT id FROM " + tenant + ".payments", java.util.UUID.class);
        var probe = context.getBean(CreationProbe.class);
        probe.failRefund.set(true);

        assertThrows(SimulatedFailure.class, () -> payments.recordRefund(order.id(), paymentId,
                new RecordPaymentRefundCommand(new BigDecimal("250000"), "Test refund")));

        assertEquals(0, count("payment_refunds"));
        assertEquals("SUCCEEDED", jdbc.queryForObject("SELECT status FROM " + tenant + ".payments", String.class));
        probe.failRefund.set(false);
        var refunded = payments.recordRefund(order.id(), paymentId,
                new RecordPaymentRefundCommand(new BigDecimal("250000"), "Test refund"));
        assertEquals(0, refunded.totalPaidAmount().signum());
        assertEquals(1, count("payment_refunds"));
        assertEquals("REFUNDED", jdbc.queryForObject("SELECT status FROM " + tenant + ".payments", String.class));
        assertThrows(ConflictException.class, () -> payments.recordRefund(order.id(), paymentId,
                new RecordPaymentRefundCommand(BigDecimal.ONE, "Duplicate refund")));
        assertEquals(1, count("payment_refunds"));
    }

    @Test
    void bufferHoursBlockAdjacentStartUntilBufferEnds() {
        var fixture = catalogFixture("Buffered tent", "AVAILABLE");
        jdbc.update("UPDATE " + tenant + ".settings SET setting_value = jsonb_set(setting_value, '{bufferHours}', '2')"
                + " WHERE setting_key = 'RENTAL'");
        var service = context.getBean(MerchantOrderCreationService.class);
        service.create(command(List.of(item(fixture, 1))));

        assertThrows(ConflictException.class, () -> service.create(command(List.of(
                new MerchantOrderItemCommand(fixture.productId(), fixture.variantId(), 1, end, end.plusDays(1))))));

        service.create(command(List.of(new MerchantOrderItemCommand(
                fixture.productId(), fixture.variantId(), 1, end.plusHours(2), end.plusDays(1).plusHours(2)))));
        assertEquals(2, count("orders"));
        assertEquals(2, count("rental_allocations"));
    }

    @Test
    void adjacentRentalCanReuseUnitButOverlappingRentalCannot() {
        var fixture = catalogFixture("Tent", "AVAILABLE");
        var service = context.getBean(MerchantOrderCreationService.class);
        service.create(command(List.of(item(fixture, 1))));

        assertThrows(ConflictException.class, () -> service.create(command(List.of(
                new MerchantOrderItemCommand(fixture.productId(), fixture.variantId(), 1, start.plusHours(1), end)))));

        var adjacent = new MerchantOrderItemCommand(fixture.productId(), fixture.variantId(), 1, end, end.plusDays(1));
        service.create(command(List.of(adjacent)));
        assertEquals(2, count("orders"));
        assertEquals(2, count("rental_allocations"));
    }

    private MerchantOrderItemCommand item(CatalogFixture fixture, int quantity) {
        return new MerchantOrderItemCommand(fixture.productId(), fixture.variantId(), quantity, start, end);
    }

    private MerchantOrderCommand command(List<MerchantOrderItemCommand> items) {
        // All fixtures use a deterministic future opening time and permit every weekday.
        jdbc.update("UPDATE " + tenant + ".settings SET setting_value = jsonb_set(setting_value, '{operatingDays}',"
                + " '[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"]')"
                + " WHERE setting_key = 'RENTAL'");
        var pickup = items.getFirst().rentalStartAt();
        var returned = items.stream().map(MerchantOrderItemCommand::rentalEndAt)
                .max(java.util.Comparator.naturalOrder()).orElseThrow();
        return new MerchantOrderCommand("Integration Customer", "customer@example.invalid", null,
                FulfillmentType.PICKUP, null, pickup, null, returned,
                PaymentMethod.CASH, InitialPaymentChoice.FULL_PAYMENT, null,
                BigDecimal.ZERO, false, null, null, items);
    }

    private void assertNoOrderWrites() {
        for (var table : List.of("orders", "order_items", "rental_allocations", "payments"))
            assertEquals(0, count(table), table + " must be empty after rollback");
    }

    static final class SimulatedFailure extends RuntimeException { }

    /** A test-only synchronous consumer; production service transaction boundaries stay intact. */
    static final class CreationProbe {
        private final EntityManagerFactory factory;
        final AtomicBoolean fail = new AtomicBoolean();
        final AtomicBoolean failPayment = new AtomicBoolean();
        final AtomicBoolean failRefund = new AtomicBoolean();
        final AtomicBoolean blockFirst = new AtomicBoolean();
        final AtomicBoolean sawAllWrites = new AtomicBoolean();
        final CountDownLatch writesFlushed = new CountDownLatch(1);
        final CountDownLatch allowCommit = new CountDownLatch(1);

        CreationProbe(EntityManagerFactory factory) { this.factory = factory; }

        @EventListener
        public void created(MerchantOrderActivityEvent event) throws InterruptedException {
            if (("PAYMENT_RECORDED".equals(event.eventType()) && failPayment.get())
                    || ("PAYMENT_REFUNDED".equals(event.eventType()) && failRefund.get())) {
                var manager = EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
                assertNotNull(manager);
                manager.flush();
                if (failPayment.get()) {
                    assertEquals("CONFIRMED", manager.createNativeQuery("SELECT status FROM orders").getSingleResult());
                    assertEquals("CONFIRMED", manager.createNativeQuery("SELECT status FROM rental_allocations").getSingleResult());
                } else {
                    assertEquals("REFUNDED", manager.createNativeQuery("SELECT status FROM payments").getSingleResult());
                    assertEquals(1L, ((Number) manager.createNativeQuery("SELECT count(*) FROM payment_refunds").getSingleResult()).longValue());
                }
                throw new SimulatedFailure();
            }
            if (!"ORDER_CREATED".equals(event.eventType())) return;
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            var manager = EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
            assertNotNull(manager);
            manager.flush();
            for (var table : List.of("orders", "order_items", "rental_allocations", "payments")) {
                var total = (Number) manager.createNativeQuery("SELECT count(*) FROM " + table).getSingleResult();
                assertTrue(total.longValue() > 0, table + " must be flushed before failure/commit");
            }
            sawAllWrites.set(true);
            if (fail.get()) throw new SimulatedFailure();
            if (blockFirst.compareAndSet(true, false)) {
                writesFlushed.countDown();
                assertTrue(allowCommit.await(30, TimeUnit.SECONDS), "Timed out waiting to release winning transaction");
            }
        }
    }
}
