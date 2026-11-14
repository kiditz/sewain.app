package app.sewain.reports;

import app.sewain.exception.ValidationException;
import app.sewain.settings.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.*;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MerchantReportServiceTest {
    final NamedParameterJdbcTemplate jdbc=mock(NamedParameterJdbcTemplate.class);
    final SettingFacade settings=mock(SettingFacade.class);
    final MerchantReportService service=new MerchantReportService(jdbc,settings);
    MerchantReportServiceTest() {
        when(settings.getValue(SettingKey.REGIONAL)).thenReturn(new ObjectMapper().readTree("{\"timezone\":\"Asia/Jakarta\"}"));
        when(jdbc.queryForMap(anyString(),any(SqlParameterSource.class))).thenReturn(Map.of());
    }
    @Test void emptyPeriodsProduceCompleteZeroFilledCalendarAndSevenTrendBuckets() {
        for (int days:List.of(7,30,90)) {
            var report=service.get(days);
            assertEquals(days,report.dailyReports().size()); assertEquals(7,report.revenueTrend().size());
            assertEquals(0,report.summary().totalOrders()); assertEquals(0,report.summary().cancellationRate().signum());
            assertEquals(0,report.summary().ordersChangePercent().signum());
            assertTrue(report.dailyReports().stream().allMatch(p->p.orders()==0 && p.revenue().signum()==0));
            for(int i=1;i<days;i++) assertEquals(report.dailyReports().get(i-1).date().plusDays(1),report.dailyReports().get(i).date());
        }
    }
    @Test void summaryCalculatesGrowthRatesAndCancellationPercentagePoints() {
        when(jdbc.queryForMap(anyString(),any(SqlParameterSource.class))).thenReturn(Map.of(
                "order_count",10L,"previous_order_count",5,"cancelled_count",2,"previous_cancelled_count",2,
                "rental_revenue",new BigDecimal("150"),"previous_rental_revenue",new BigDecimal("100"),
                "average_order_value","15","previous_average_order_value","20"));
        var summary=service.get(30).summary();
        amount("100",summary.ordersChangePercent()); amount("50",summary.revenueChangePercent());
        amount("-25",summary.averageOrderValueChangePercent()); amount("20",summary.cancellationRate());
        amount("-20",summary.cancellationRateChangePoints());
    }
    @Test void newBusinessWithNoPreviousRevenueHasUndefinedGrowthRatherThanDivisionByZero() {
        when(jdbc.queryForMap(anyString(),any(SqlParameterSource.class))).thenReturn(Map.of("order_count",1,"rental_revenue",BigDecimal.TEN));
        var summary=service.get(7).summary();
        assertNull(summary.ordersChangePercent()); assertNull(summary.revenueChangePercent());
        amount("0",summary.averageOrderValueChangePercent());
    }
    @Test void invalidPeriodNeverQueriesDatabase() {
        for(int days:List.of(0,1,8,29,91,-1)) assertThrows(ValidationException.class,()->service.get(days));
        verifyNoInteractions(jdbc,settings);
    }
    @Test void queryBoundsUseRegionalMidnightAndExclusiveNextDay() {
        service.get(7);
        var params=org.mockito.ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).queryForMap(anyString(),params.capture());
        var from=(java.time.OffsetDateTime)params.getValue().getValue("fromAt");
        var to=(java.time.OffsetDateTime)params.getValue().getValue("toAt");
        assertEquals(java.time.ZoneOffset.ofHours(7),from.getOffset());
        assertEquals(java.time.LocalTime.MIDNIGHT,from.toLocalTime()); assertEquals(from.plusDays(7),to);
    }
    void amount(String expected,BigDecimal actual) { assertEquals(0,new BigDecimal(expected).compareTo(actual)); }
    @Test void databaseRowsPopulateDailyTrendStatusAndTopProductResponses() throws Exception {
        var rs=mock(java.sql.ResultSet.class);var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta"));var product=UUID.randomUUID();
        when(rs.getObject("report_date",java.time.LocalDate.class)).thenReturn(today);
        when(rs.getLong("order_count")).thenReturn(2L);when(rs.getLong("completed")).thenReturn(1L);when(rs.getLong("cancelled")).thenReturn(1L);
        when(rs.getBigDecimal("net_revenue")).thenReturn(new BigDecimal("100"));when(rs.getString("status")).thenReturn("COMPLETED");when(rs.getLong("count")).thenReturn(2L);
        when(rs.getObject("product_id",UUID.class)).thenReturn(product);when(rs.getString("name")).thenReturn("Tent");when(rs.getString("category")).thenReturn("Camping");
        when(rs.getLong("rentals")).thenReturn(3L);when(rs.getBigDecimal("revenue")).thenReturn(new BigDecimal("200"));when(rs.getBigDecimal("utilization")).thenReturn(new BigDecimal("50"));
        when(jdbc.query(anyString(),any(SqlParameterSource.class),any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(c->List.of(((org.springframework.jdbc.core.RowMapper<?>)c.getArgument(2)).mapRow(rs,0)));
        var report=service.get(7);var daily=report.dailyReports().getLast();assertEquals(today,daily.date());assertEquals(2,daily.orders());assertEquals(1,daily.completed());
        amount("100",daily.revenue());assertEquals("COMPLETED",report.orderStatuses().getFirst().status());
        assertEquals(product,report.topProducts().getFirst().id());assertEquals("Camping",report.topProducts().getFirst().category());
        amount("100",report.revenueTrend().stream().map(MerchantReportTrendPoint::revenue).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
}
