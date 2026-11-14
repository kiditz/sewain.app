package app.sewain.orders;
import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class OrderQueryEndpointsTest {
 final RentalOrderRepository orders=mock(RentalOrderRepository.class);final MerchantOrderActivityRepository activities=mock(MerchantOrderActivityRepository.class);
 final MerchantOrderQueryServiceImpl service=new MerchantOrderQueryServiceImpl(orders,Mappers.getMapper(MerchantOrderMapper.class),mock(OrderItemRepository.class),mock(PaymentRepository.class),mock(PaymentRefundRepository.class),mock(MerchantOrderLateFeeService.class),activities);
 @Test void calendarValidatesBoundsBeforeCallingRepository(){var start=OffsetDateTime.parse("2026-01-01T00:00:00Z");for(var bounds:List.of(new OffsetDateTime[]{null,start},new OffsetDateTime[]{start,null},new OffsetDateTime[]{start,start},new OffsetDateTime[]{start,start.minusDays(1)},new OffsetDateTime[]{start,start.plusDays(94)}))assertThrows(ValidationException.class,()->service.calendar(bounds[0],bounds[1]));service.calendar(start,start.plusDays(7));verify(orders).findCalendarEntries(start,start.plusDays(7),FulfillmentType.PICKUP,OrderStatus.CANCELLED,OrderStatus.EXPIRED,RentalAllocationStatus.RELEASED);}
 @Test void paginatedSearchAndActivitiesReturnMappedRepositoryData(){var order=new RentalOrder();order.setId(UUID.randomUUID());when(orders.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));assertEquals(order.getId(),service.search(null).items().getFirst().id());var activity=new MerchantOrderActivity();activity.setId(UUID.randomUUID());activity.setEventType("PAYMENT_RECORDED");when(activities.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of(activity));assertEquals(activity.getId(),service.recentActivities().getFirst().id());assertThrows(ResourceNotFoundException.class,()->service.get(UUID.randomUUID()));}
}
