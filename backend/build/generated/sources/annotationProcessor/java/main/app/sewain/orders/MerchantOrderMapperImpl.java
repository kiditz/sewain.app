package app.sewain.orders;

import app.sewain.catalogs.OrderCatalogService;
import app.sewain.customers.Customer;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-24T00:58:37+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class MerchantOrderMapperImpl implements MerchantOrderMapper {

    @Override
    public RentalOrder toEntity(MerchantOrderCommand command, MerchantOrderData data) {
        if ( command == null && data == null ) {
            return null;
        }

        RentalOrder rentalOrder = new RentalOrder();

        if ( command != null ) {
            rentalOrder.setCustomerName( command.customerName() );
            rentalOrder.setCustomerPhone( command.customerPhone() );
            rentalOrder.setFulfillmentType( command.fulfillmentType() );
            Map<String, Object> map1 = command.fulfillmentAddress();
            if ( map1 != null ) {
                rentalOrder.setFulfillmentAddress( new LinkedHashMap<String, Object>( map1 ) );
            }
            rentalOrder.setPickupAt( command.pickupAt() );
            rentalOrder.setDeliveryAt( command.deliveryAt() );
            rentalOrder.setReturnAt( command.returnAt() );
            rentalOrder.setPaymentMethod( command.paymentMethod() );
            rentalOrder.setNotes( command.notes() );
        }
        if ( data != null ) {
            rentalOrder.setOrderNumber( data.orderNumber() );
            rentalOrder.setStatus( data.status() );
            rentalOrder.setCustomerEmail( data.customerEmail() );
            rentalOrder.setBalanceDueAt( data.balanceDueAt() );
            Map<String, Object> map = data.paymentTermsSnapshot();
            if ( map != null ) {
                rentalOrder.setPaymentTermsSnapshot( new LinkedHashMap<String, Object>( map ) );
            }
            rentalOrder.setSubtotal( data.subtotal() );
            rentalOrder.setDepositTotal( data.depositTotal() );
            rentalOrder.setDepositWaived( data.depositWaived() );
            rentalOrder.setDepositWaiverReason( data.depositWaiverReason() );
            rentalOrder.setDeliveryFee( data.deliveryFee() );
            rentalOrder.setTotalAmount( data.totalAmount() );
            rentalOrder.setCurrency( data.currency() );
        }

        return rentalOrder;
    }

    @Override
    public OrderItem toEntity(MerchantOrderItemCommand command, RentalOrder order, OrderCatalogService.VariantPricing pricing, int durationValue, RentalDurationUnit durationUnit, BigDecimal lineTotal) {
        if ( command == null && order == null && pricing == null && durationUnit == null && lineTotal == null ) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        if ( command != null ) {
            orderItem.setRentalStartAt( command.rentalStartAt() );
            orderItem.setRentalEndAt( command.rentalEndAt() );
            orderItem.setQuantity( command.quantity() );
        }
        if ( pricing != null ) {
            orderItem.setProductId( pricing.productId() );
            orderItem.setVariantId( pricing.variantId() );
            orderItem.setProductName( pricing.productName() );
            orderItem.setVariantName( pricing.variantName() );
            orderItem.setSku( pricing.sku() );
            orderItem.setRentalUnitPrice( pricing.rentalPrice() );
            orderItem.setDepositUnitAmount( pricing.depositUnitAmount() );
            Map<String, Object> map = pricing.rentalPriceSnapshot();
            if ( map != null ) {
                orderItem.setRentalPriceSnapshot( new LinkedHashMap<String, Object>( map ) );
            }
        }
        orderItem.setOrder( order );
        orderItem.setRentalDurationValue( durationValue );
        orderItem.setRentalDurationUnit( durationUnit );
        orderItem.setLineTotal( lineTotal );

        return orderItem;
    }

    @Override
    public Payment toEntity(RentalOrder order, PaymentStage stage, PaymentType type, BigDecimal amount, PaymentMethod method) {
        if ( order == null && stage == null && type == null && amount == null && method == null ) {
            return null;
        }

        Payment payment = new Payment();

        if ( order != null ) {
            payment.setOrder( order );
            payment.setCurrency( order.getCurrency() );
        }
        payment.setPaymentStage( stage );
        payment.setPaymentType( type );
        payment.setAmount( amount );
        payment.setMethod( method );
        payment.setProvider( "MANUAL" );
        payment.setStatus( PaymentStatus.PENDING );

        return payment;
    }

    @Override
    public Payment toReceipt(RentalOrder order, PaymentStage stage, PaymentType type, BigDecimal amount, PaymentMethod method, OffsetDateTime paidAt, String reference) {
        if ( order == null && stage == null && type == null && amount == null && method == null && paidAt == null && reference == null ) {
            return null;
        }

        Payment payment = new Payment();

        if ( order != null ) {
            payment.setOrder( order );
            payment.setCurrency( order.getCurrency() );
        }
        payment.setPaymentStage( stage );
        payment.setPaymentType( type );
        payment.setAmount( amount );
        payment.setMethod( method );
        payment.setPaidAt( paidAt );
        payment.setProviderPaymentId( reference );
        payment.setProvider( "MANUAL" );
        payment.setStatus( PaymentStatus.SUCCEEDED );

        return payment;
    }

    @Override
    public PaymentRefund toSuccessfulRefund(Payment payment, BigDecimal amount, BigDecimal rentalAmount, BigDecimal depositAmount, String reason, OffsetDateTime processedAt) {
        if ( payment == null && amount == null && rentalAmount == null && depositAmount == null && reason == null && processedAt == null ) {
            return null;
        }

        PaymentRefund paymentRefund = new PaymentRefund();

        paymentRefund.setPayment( payment );
        paymentRefund.setAmount( amount );
        paymentRefund.setRentalAmount( rentalAmount );
        paymentRefund.setDepositAmount( depositAmount );
        paymentRefund.setReason( reason );
        paymentRefund.setProcessedAt( processedAt );
        paymentRefund.setStatus( PaymentRefundStatus.SUCCEEDED );

        return paymentRefund;
    }

    @Override
    public void confirmReceipt(RecordOrderPaymentCommand command, Payment payment, OffsetDateTime paidAt) {
        if ( command == null && paidAt == null ) {
            return;
        }

        if ( command != null ) {
            if ( command.method() != null ) {
                payment.setMethod( command.method() );
            }
            if ( command.amount() != null ) {
                payment.setAmount( command.amount() );
            }
            if ( command.reference() != null ) {
                payment.setProviderPaymentId( command.reference() );
            }
        }
        if ( paidAt != null ) {
            payment.setPaidAt( paidAt );
        }
        payment.setStatus( PaymentStatus.SUCCEEDED );
    }

    @Override
    public void updatePaymentStatus(PaymentStatus status, Payment payment) {
        if ( status == null ) {
            return;
        }

        if ( status != null ) {
            payment.setStatus( status );
        }
    }

    @Override
    public void linkCustomer(Customer customer, RentalOrder order) {
        if ( customer == null ) {
            return;
        }

        if ( customer != null ) {
            order.setCustomer( customer );
        }
    }

    @Override
    public RentalAllocation toAllocation(UUID unitId, OrderItem item, OffsetDateTime start, OffsetDateTime end, OffsetDateTime expiresAt) {
        if ( unitId == null && item == null && start == null && end == null && expiresAt == null ) {
            return null;
        }

        RentalAllocation rentalAllocation = new RentalAllocation();

        rentalAllocation.setInventoryUnitId( unitId );
        rentalAllocation.setOrderItem( item );
        rentalAllocation.setRentalStartAt( start );
        rentalAllocation.setRentalEndAt( end );
        rentalAllocation.setHoldExpiresAt( expiresAt );
        rentalAllocation.setStatus( RentalAllocationStatus.HELD );

        return rentalAllocation;
    }

    @Override
    public MerchantOrderResponse toResponse(RentalOrder entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String orderNumber = null;
        OrderStatus status = null;
        FulfillmentType fulfillmentType = null;
        OffsetDateTime pickupAt = null;
        OffsetDateTime deliveryAt = null;
        OffsetDateTime returnAt = null;
        PaymentMethod paymentMethod = null;
        String customerName = null;
        String customerEmail = null;
        BigDecimal subtotal = null;
        BigDecimal depositTotal = null;
        boolean depositWaived = false;
        String depositWaiverReason = null;
        BigDecimal deliveryFee = null;
        BigDecimal totalAmount = null;
        OffsetDateTime createdAt = null;
        BalanceDueAt balanceDueAt = null;
        OffsetDateTime returnedAt = null;
        BigDecimal lateFeeTotal = null;
        boolean lateFeeWaived = false;
        String lateFeeWaiverReason = null;

        id = entity.getId();
        orderNumber = entity.getOrderNumber();
        status = entity.getStatus();
        fulfillmentType = entity.getFulfillmentType();
        pickupAt = entity.getPickupAt();
        deliveryAt = entity.getDeliveryAt();
        returnAt = entity.getReturnAt();
        paymentMethod = entity.getPaymentMethod();
        customerName = entity.getCustomerName();
        customerEmail = entity.getCustomerEmail();
        subtotal = entity.getSubtotal();
        depositTotal = entity.getDepositTotal();
        depositWaived = entity.isDepositWaived();
        depositWaiverReason = entity.getDepositWaiverReason();
        deliveryFee = entity.getDeliveryFee();
        totalAmount = entity.getTotalAmount();
        createdAt = entity.getCreatedAt();
        balanceDueAt = entity.getBalanceDueAt();
        returnedAt = entity.getReturnedAt();
        lateFeeTotal = entity.getLateFeeTotal();
        lateFeeWaived = entity.isLateFeeWaived();
        lateFeeWaiverReason = entity.getLateFeeWaiverReason();

        List<MerchantOrderPaymentResponse> payments = null;
        BigDecimal totalPaidAmount = null;
        BigDecimal remainingAmount = null;
        BigDecimal remainingRentalAmount = null;
        BigDecimal remainingDepositAmount = null;
        BigDecimal initialAmountDue = null;
        MerchantOrderLateFeeResponse lateFee = null;
        List<MerchantOrderItemResponse> items = null;

        MerchantOrderResponse merchantOrderResponse = new MerchantOrderResponse( id, orderNumber, status, fulfillmentType, pickupAt, deliveryAt, returnAt, paymentMethod, customerName, customerEmail, subtotal, depositTotal, depositWaived, depositWaiverReason, deliveryFee, totalAmount, createdAt, items, payments, totalPaidAmount, remainingAmount, remainingRentalAmount, remainingDepositAmount, initialAmountDue, balanceDueAt, returnedAt, lateFeeTotal, lateFeeWaived, lateFeeWaiverReason, lateFee );

        return merchantOrderResponse;
    }

    @Override
    public MerchantOrderResponse toDetailResponse(RentalOrder entity, List<OrderItem> items, MerchantOrderPaymentSummary summary, MerchantOrderLateFeeResponse lateFee) {
        if ( entity == null && items == null && summary == null && lateFee == null ) {
            return null;
        }

        UUID id = null;
        String orderNumber = null;
        OrderStatus status = null;
        FulfillmentType fulfillmentType = null;
        OffsetDateTime pickupAt = null;
        OffsetDateTime deliveryAt = null;
        OffsetDateTime returnAt = null;
        PaymentMethod paymentMethod = null;
        String customerName = null;
        String customerEmail = null;
        BigDecimal subtotal = null;
        BigDecimal depositTotal = null;
        boolean depositWaived = false;
        String depositWaiverReason = null;
        BigDecimal deliveryFee = null;
        BigDecimal totalAmount = null;
        OffsetDateTime createdAt = null;
        BalanceDueAt balanceDueAt = null;
        OffsetDateTime returnedAt = null;
        BigDecimal lateFeeTotal = null;
        boolean lateFeeWaived = false;
        String lateFeeWaiverReason = null;
        if ( entity != null ) {
            id = entity.getId();
            orderNumber = entity.getOrderNumber();
            status = entity.getStatus();
            fulfillmentType = entity.getFulfillmentType();
            pickupAt = entity.getPickupAt();
            deliveryAt = entity.getDeliveryAt();
            returnAt = entity.getReturnAt();
            paymentMethod = entity.getPaymentMethod();
            customerName = entity.getCustomerName();
            customerEmail = entity.getCustomerEmail();
            subtotal = entity.getSubtotal();
            depositTotal = entity.getDepositTotal();
            depositWaived = entity.isDepositWaived();
            depositWaiverReason = entity.getDepositWaiverReason();
            deliveryFee = entity.getDeliveryFee();
            totalAmount = entity.getTotalAmount();
            createdAt = entity.getCreatedAt();
            balanceDueAt = entity.getBalanceDueAt();
            returnedAt = entity.getReturnedAt();
            lateFeeTotal = entity.getLateFeeTotal();
            lateFeeWaived = entity.isLateFeeWaived();
            lateFeeWaiverReason = entity.getLateFeeWaiverReason();
        }
        List<MerchantOrderPaymentResponse> payments = null;
        BigDecimal totalPaidAmount = null;
        BigDecimal remainingAmount = null;
        BigDecimal remainingRentalAmount = null;
        BigDecimal remainingDepositAmount = null;
        BigDecimal initialAmountDue = null;
        if ( summary != null ) {
            List<MerchantOrderPaymentResponse> list1 = summary.payments();
            if ( list1 != null ) {
                payments = new ArrayList<MerchantOrderPaymentResponse>( list1 );
            }
            totalPaidAmount = summary.totalPaidAmount();
            remainingAmount = summary.remainingAmount();
            remainingRentalAmount = summary.remainingRentalAmount();
            remainingDepositAmount = summary.remainingDepositAmount();
            initialAmountDue = summary.initialAmountDue();
        }
        List<MerchantOrderItemResponse> items1 = null;
        items1 = toItemResponses( items );
        MerchantOrderLateFeeResponse lateFee1 = null;
        lateFee1 = lateFee;

        MerchantOrderResponse merchantOrderResponse = new MerchantOrderResponse( id, orderNumber, status, fulfillmentType, pickupAt, deliveryAt, returnAt, paymentMethod, customerName, customerEmail, subtotal, depositTotal, depositWaived, depositWaiverReason, deliveryFee, totalAmount, createdAt, items1, payments, totalPaidAmount, remainingAmount, remainingRentalAmount, remainingDepositAmount, initialAmountDue, balanceDueAt, returnedAt, lateFeeTotal, lateFeeWaived, lateFeeWaiverReason, lateFee1 );

        return merchantOrderResponse;
    }

    @Override
    public MerchantOrderItemResponse toItemResponse(OrderItem entity) {
        if ( entity == null ) {
            return null;
        }

        UUID productId = null;
        UUID variantId = null;
        String productName = null;
        String variantName = null;
        String sku = null;
        OffsetDateTime rentalStartAt = null;
        OffsetDateTime rentalEndAt = null;
        int rentalDurationValue = 0;
        RentalDurationUnit rentalDurationUnit = null;
        int quantity = 0;
        BigDecimal rentalUnitPrice = null;
        BigDecimal depositUnitAmount = null;
        BigDecimal discountAmount = null;
        BigDecimal taxAmount = null;
        BigDecimal lineTotal = null;
        Map<String, Object> rentalPriceSnapshot = null;

        productId = entity.getProductId();
        variantId = entity.getVariantId();
        productName = entity.getProductName();
        variantName = entity.getVariantName();
        sku = entity.getSku();
        rentalStartAt = entity.getRentalStartAt();
        rentalEndAt = entity.getRentalEndAt();
        rentalDurationValue = entity.getRentalDurationValue();
        rentalDurationUnit = entity.getRentalDurationUnit();
        quantity = entity.getQuantity();
        rentalUnitPrice = entity.getRentalUnitPrice();
        depositUnitAmount = entity.getDepositUnitAmount();
        discountAmount = entity.getDiscountAmount();
        taxAmount = entity.getTaxAmount();
        lineTotal = entity.getLineTotal();
        Map<String, Object> map = entity.getRentalPriceSnapshot();
        if ( map != null ) {
            rentalPriceSnapshot = new LinkedHashMap<String, Object>( map );
        }

        MerchantOrderItemResponse merchantOrderItemResponse = new MerchantOrderItemResponse( productId, variantId, productName, variantName, sku, rentalStartAt, rentalEndAt, rentalDurationValue, rentalDurationUnit, quantity, rentalUnitPrice, depositUnitAmount, discountAmount, taxAmount, lineTotal, rentalPriceSnapshot );

        return merchantOrderItemResponse;
    }

    @Override
    public List<MerchantOrderItemResponse> toItemResponses(List<OrderItem> entities) {
        if ( entities == null ) {
            return null;
        }

        List<MerchantOrderItemResponse> list = new ArrayList<MerchantOrderItemResponse>( entities.size() );
        for ( OrderItem orderItem : entities ) {
            list.add( toItemResponse( orderItem ) );
        }

        return list;
    }

    @Override
    public MerchantOrderPaymentResponse toPaymentResponse(Payment payment, BigDecimal refundedAmount, BigDecimal rentalRefundedAmount, BigDecimal depositRefundedAmount) {
        if ( payment == null && refundedAmount == null && rentalRefundedAmount == null && depositRefundedAmount == null ) {
            return null;
        }

        PaymentStage stage = null;
        PaymentType type = null;
        String reference = null;
        UUID id = null;
        BigDecimal amount = null;
        String currency = null;
        PaymentStatus status = null;
        PaymentMethod method = null;
        OffsetDateTime paidAt = null;
        OffsetDateTime createdAt = null;
        if ( payment != null ) {
            stage = payment.getPaymentStage();
            type = payment.getPaymentType();
            reference = payment.getProviderPaymentId();
            id = payment.getId();
            amount = payment.getAmount();
            currency = payment.getCurrency();
            status = payment.getStatus();
            method = payment.getMethod();
            paidAt = payment.getPaidAt();
            createdAt = payment.getCreatedAt();
        }
        BigDecimal refundedAmount1 = null;
        refundedAmount1 = refundedAmount;
        BigDecimal rentalRefundedAmount1 = null;
        rentalRefundedAmount1 = rentalRefundedAmount;
        BigDecimal depositRefundedAmount1 = null;
        depositRefundedAmount1 = depositRefundedAmount;

        BigDecimal receivedAmount = payment.getStatus() == PaymentStatus.REFUNDED ? java.math.BigDecimal.ZERO : payment.getAmount().subtract(refundedAmount).max(java.math.BigDecimal.ZERO);
        BigDecimal rentalReceivedAmount = payment.getStatus() == PaymentStatus.REFUNDED ? java.math.BigDecimal.ZERO : payment.getRentalAmount().subtract(rentalRefundedAmount).max(java.math.BigDecimal.ZERO);
        BigDecimal depositReceivedAmount = payment.getStatus() == PaymentStatus.REFUNDED ? java.math.BigDecimal.ZERO : payment.getDepositAmount().subtract(depositRefundedAmount).max(java.math.BigDecimal.ZERO);

        MerchantOrderPaymentResponse merchantOrderPaymentResponse = new MerchantOrderPaymentResponse( id, stage, type, amount, refundedAmount1, receivedAmount, rentalReceivedAmount, depositReceivedAmount, rentalRefundedAmount1, depositRefundedAmount1, currency, status, method, reference, paidAt, createdAt );

        return merchantOrderPaymentResponse;
    }

    @Override
    public List<MerchantOrderResponse> toResponses(List<RentalOrder> entities) {
        if ( entities == null ) {
            return null;
        }

        List<MerchantOrderResponse> list = new ArrayList<MerchantOrderResponse>( entities.size() );
        for ( RentalOrder rentalOrder : entities ) {
            list.add( toResponse( rentalOrder ) );
        }

        return list;
    }
}
