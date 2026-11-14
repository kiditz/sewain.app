package app.sewain.finance;

import app.sewain.exception.ValidationException;
import app.sewain.settings.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FinanceServiceTest {
    final FinanceTransactionRepository repository=mock(FinanceTransactionRepository.class);
    final SettingFacade settings=mock(SettingFacade.class);
    final FinanceService service=new FinanceService(repository,Mappers.getMapper(FinanceMapper.class),settings);
    FinanceServiceTest() {
        when(settings.getValue(SettingKey.REGIONAL)).thenReturn(new ObjectMapper().readTree("{\"timezone\":\"Asia/Jakarta\",\"currency\":\"IDR\"}"));
        when(repository.sumAmount(any(),any(),any())).thenReturn(BigDecimal.ZERO);
        when(repository.sumCategory(any(),any(),any(),any())).thenReturn(BigDecimal.ZERO);
        when(repository.sumLifetimeCategory(any(),any())).thenReturn(BigDecimal.ZERO);
        when(repository.findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(any(),any(),any())).thenReturn(Page.empty());
        when(repository.findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanAndDirectionOrderByOccurredAtDesc(any(),any(),any(),any())).thenReturn(Page.empty());
    }
    @Test void summarySubtractsRefundsAndClampsNegativeRentalAndDepositTotals() {
        when(repository.sumAmount(eq(FinanceDirection.INCOME),any(),any())).thenReturn(new BigDecimal("100"));
        when(repository.sumCategory(eq(FinanceCategory.RENTAL_REFUND),any(),any(),any())).thenReturn(new BigDecimal("30"));
        when(repository.sumLifetimeCategory(FinanceCategory.DEPOSIT_REFUND,FinanceDirection.EXPENSE)).thenReturn(BigDecimal.TEN);
        var result=service.get(LocalDate.of(2026,1,1),LocalDate.of(2026,1,31),"ALL",null,null);
        assertEquals(0,result.summary().cashIn().compareTo(new BigDecimal("100")));
        assertEquals(0,result.summary().rentalIncome().signum()); assertEquals(0,result.summary().depositHeld().signum());
    }
    @Test void datesAreInclusiveLocallyAndPaginationIsBounded() {
        var date=LocalDate.of(2026,1,10); service.get(date,date,"income",-5,500);
        var page=org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanAndDirectionOrderByOccurredAtDesc(
                eq(date.atStartOfDay(ZoneId.of("Asia/Jakarta")).toOffsetDateTime()),
                eq(date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Jakarta")).toOffsetDateTime()),eq(FinanceDirection.INCOME),page.capture());
        assertEquals(0,page.getValue().getPageNumber()); assertEquals(100,page.getValue().getPageSize());
    }
    @Test void invertedAndExcessiveRangesAreRejected() {
        var date=LocalDate.of(2026,1,1);
        assertThrows(ValidationException.class,()->service.get(date,date.minusDays(1),null,null,null));
        assertThrows(ValidationException.class,()->service.get(date,date.plusDays(90),null,null,null));
        verify(repository,never()).sumAmount(any(),any(),any());
    }
    @Test void expenseUsesRegionalCurrencyAndLocalMidnight() {
        when(repository.save(any())).thenAnswer(c->c.getArgument(0));
        var result=service.recordExpense(new RecordExpenseCommand(ExpenseCategory.UTILITIES,"Electricity",new BigDecimal("125000"),
                LocalDate.of(2026,1,10),ExpensePaymentMethod.CASH,"January"));
        assertEquals(FinanceDirection.EXPENSE,result.direction()); assertEquals(FinanceCategory.OPERATING_EXPENSE,result.category());
        assertEquals("IDR",result.currency()); assertEquals(ZoneOffset.ofHours(7),result.occurredAt().getOffset());
        assertEquals("January",result.notes()); assertNotNull(result.sourceId());
    }
    @Test void defaultRangeAndMinimumPageSizeWorkWithoutDirectionFilter() {
        service.get(null,null,"",null,0);
        var page=org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(any(),any(),page.capture());
        assertEquals(1,page.getValue().getPageSize());
    }
}
