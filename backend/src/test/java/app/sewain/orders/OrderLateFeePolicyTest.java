package app.sewain.orders;
import app.sewain.settings.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderLateFeePolicyTest {
 final SettingFacade settings=mock(SettingFacade.class);final MerchantOrderLateFeeService service=new MerchantOrderLateFeeService(settings,new ObjectMapper());
 final OffsetDateTime due=OffsetDateTime.parse("2026-01-01T00:00:00Z");
 OrderItem item(){var item=new OrderItem();item.setRentalStartAt(due.minusDays(1));item.setRentalEndAt(due);item.setQuantity(1);item.setRentalDurationValue(1);item.setRentalDurationUnit(RentalDurationUnit.DAY);item.setRentalUnitPrice(new BigDecimal("100"));return item;}
 @Test void configuredSnapshotOverridesCurrentStoreSettings(){var order=new RentalOrder();order.setReturnAt(due);order.getPaymentTermsSnapshot().put("rental",Map.of("lateReturnEnabled",true,"lateReturnGraceHours",1));var quote=service.calculate(order,List.of(item()),due.plusDays(1));assertTrue(quote.enabled());assertTrue(quote.amount().signum()>0);verifyNoInteractions(settings);}
 @Test void legacyOrderFallsBackToCurrentSettingsAndUsesLatestItemDueDate(){when(settings.getValue(SettingKey.RENTAL)).thenReturn(new ObjectMapper().readTree("{\"lateReturnEnabled\":true,\"lateReturnGraceHours\":1}"));var order=new RentalOrder();var quote=service.calculate(order,List.of(item()),due.plusDays(1));assertTrue(quote.enabled());assertTrue(quote.lateMinutes()>0);}
 @Test void unavailableSettingsDisableFeesInsteadOfBreakingLegacyOrder(){when(settings.getValue(SettingKey.RENTAL)).thenThrow(new IllegalStateException("Missing"));assertFalse(service.calculate(new RentalOrder(),List.of(),due).enabled());}
 @Test void inactiveOrderHasNoLiveQuoteAndReturnedOrderUsesStoredAssessment(){var order=new RentalOrder();order.getPaymentTermsSnapshot().put("rental",Map.of("lateReturnEnabled",false));order.setStatus(OrderStatus.CONFIRMED);assertNull(service.response(order,List.of()));order.setReturnedAt(due);order.setLateFeeAssessed(BigDecimal.TEN);assertNotNull(service.response(order,List.of()));order.setLateFeeUnit("DAY");assertNotNull(service.response(order,List.of()));}
 @Test void activeOrderWithoutReturnProducesLiveQuote(){var order=new RentalOrder();order.setStatus(OrderStatus.ACTIVE_RENTAL);order.getPaymentTermsSnapshot().put("rental",Map.of("lateReturnEnabled",true));order.setReturnAt(due);assertNotNull(service.response(order,List.of(item())));}
}
