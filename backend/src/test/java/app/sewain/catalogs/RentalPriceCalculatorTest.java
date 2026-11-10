package app.sewain.catalogs;

import app.sewain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RentalPriceCalculatorTest {
    private final OffsetDateTime start=OffsetDateTime.parse("2026-01-01T10:00:00+07:00");
    private final ZoneId zone=ZoneId.of("Asia/Jakarta");
    private RentalPriceCalculator.RentalPlan plan(String unit,int days,String price) {
        return new RentalPriceCalculator.RentalPlan(UUID.randomUUID(),unit,days,new BigDecimal(price));
    }
    @Test void findsCheapestCombinationRatherThanLargestPackageFirst() {
        var daily=plan("DAY",1,"40"); var three=plan("DAY",3,"85"); var four=plan("DAY",4,"120");
        var result=RentalPriceCalculator.calculate(start,start.plusDays(6),zone,List.of(daily,three,four));
        assertEquals(0,new BigDecimal("170").compareTo(result.totalPrice()));
        assertEquals(6,result.coveredDurationDays());
        assertEquals(List.of(new RentalPriceCalculator.PlanSelection(three.id(),2,3,three.rentalFee())),result.selections());
    }
    @Test void allowsOverCoverageWhenOnlyThreeDayPackagesExist() {
        var result=RentalPriceCalculator.calculate(start,start.plusDays(5),zone,List.of(plan("DAY",3,"100")));
        assertEquals(5,result.requestedDurationDays());
        assertEquals(6,result.coveredDurationDays());
        assertEquals(0,new BigDecimal("200").compareTo(result.totalPrice()));
    }
    @Test void usesLongerSinglePackageWhenCheaperThanDailyRental() {
        var weekly=plan("WEEK",1,"100");
        var result=RentalPriceCalculator.calculate(start,start.plusDays(2),zone,List.of(plan("DAY",1,"60"),weekly));
        assertEquals(7,result.coveredDurationDays());
        assertEquals(weekly.id(),result.selections().getFirst().rentalPlanId());
        assertEquals(0,new BigDecimal("100").compareTo(result.totalPrice()));
    }
    @Test void prefersLessCoverageWhenPricesTie() {
        var result=RentalPriceCalculator.calculate(start,start.plusDays(2),zone,List.of(plan("DAY",1,"50"),plan("DAY",3,"100")));
        assertEquals(2,result.coveredDurationDays());
    }
    @Test void normalizesWeekAndMonthPlansAndIgnoresHourlyNegativeAndZeroDurationPlans() {
        var plans=List.of(plan("WEEK",1,"70"),plan("MONTH",1,"250"),plan("HOUR",1,"0"),plan("DAY",1,"-1"),plan("DAY",0,"0"));
        var result=RentalPriceCalculator.calculate(start,start.plusDays(30),zone,plans);
        assertEquals(30,result.coveredDurationDays());
        assertEquals(0,new BigDecimal("250").compareTo(result.totalPrice()));
    }
    @Test void acceptsFreePlansWithoutTreatingZeroAsMissingPrice() {
        var result=RentalPriceCalculator.calculate(start,start.plusDays(3),zone,List.of(plan("DAY",1,"0")));
        assertEquals(0,result.totalPrice().signum());
        assertEquals(3,result.coveredDurationDays());
    }
    @Test void usesMerchantCalendarDatesRatherThanUtcDates() {
        var result=RentalPriceCalculator.calculate(OffsetDateTime.parse("2026-01-01T16:00:00Z"),OffsetDateTime.parse("2026-01-02T18:00:00Z"),zone,List.of(plan("DAY",1,"100")));
        assertEquals(2,result.requestedDurationDays());
    }
    @Test void daylightSavingShortDayStillCountsAsOneRentalDay() {
        var result=RentalPriceCalculator.calculate(OffsetDateTime.parse("2026-03-08T00:00:00-05:00"),OffsetDateTime.parse("2026-03-09T00:00:00-04:00"),ZoneId.of("America/New_York"),List.of(plan("DAY",1,"100")));
        assertEquals(1,result.requestedDurationDays());
    }
    @Test void rejectsSameDayReversedExcessiveOrMissingDates() {
        var plans=List.of(plan("DAY",1,"100"));
        assertThrows(ValidationException.class,()->RentalPriceCalculator.calculate(start,start.plusHours(1),zone,plans));
        assertThrows(ValidationException.class,()->RentalPriceCalculator.calculate(start,start.minusDays(1),zone,plans));
        assertThrows(ValidationException.class,()->RentalPriceCalculator.calculate(start,start.plusDays(3651),zone,plans));
        assertThrows(ValidationException.class,()->RentalPriceCalculator.calculate(null,start,zone,plans));
        assertThrows(ValidationException.class,()->RentalPriceCalculator.calculate(start,start.plusDays(1),null,plans));
    }
    @Test void rejectsCatalogWithNoUsablePlans() {
        assertThrows(ValidationException.class,()->RentalPriceCalculator.calculate(start,start.plusDays(1),zone,List.of()));
        assertThrows(ValidationException.class,()->RentalPriceCalculator.calculate(start,start.plusDays(1),zone,List.of(plan("HOUR",1,"10"))));
    }
}
