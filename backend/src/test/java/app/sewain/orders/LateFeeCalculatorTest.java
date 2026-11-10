package app.sewain.orders;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LateFeeCalculatorTest {
    private final OffsetDateTime due=OffsetDateTime.parse("2026-01-05T12:00:00Z");
    private OrderItem item(RentalDurationUnit unit,int duration,int quantity,String price) {
        var item=new OrderItem(); item.setRentalDurationUnit(unit); item.setRentalDurationValue(duration);
        item.setQuantity(quantity); item.setRentalUnitPrice(new BigDecimal(price)); return item;
    }
    @Test void exactGraceBoundaryIsFreeButOneMinuteBeyondBillsWholeDelay() {
        var policy=new LateFeeCalculator.Policy(true,120);
        var items=List.of(item(RentalDurationUnit.DAY,1,2,"50000"));
        assertEquals(0,LateFeeCalculator.calculate(policy,due,due.plusMinutes(120),items).amount().signum());
        var late=LateFeeCalculator.calculate(policy,due,due.plusMinutes(121),items);
        assertEquals(121,late.lateMinutes()); assertEquals(1,late.billedUnits());
        assertEquals(new BigDecimal("100000.00"),late.amount());
    }
    @Test void afterGraceBillsAllDelayNotOnlyMinutesExceedingGrace() {
        var quote=LateFeeCalculator.calculate(new LateFeeCalculator.Policy(true,120),due,due.plusDays(1).plusMinutes(1),List.of(item(RentalDurationUnit.DAY,1,1,"50")));
        assertEquals(2,quote.billedUnits()); assertEquals(new BigDecimal("100.00"),quote.amount());
    }
    @Test void exactDayIsOneUnitAndTwentyFiveHoursRoundsUpToTwo() {
        var policy=new LateFeeCalculator.Policy(true,0); var items=List.of(item(RentalDurationUnit.DAY,1,1,"50"));
        assertEquals(1,LateFeeCalculator.calculate(policy,due,due.plusDays(1),items).billedUnits());
        assertEquals(2,LateFeeCalculator.calculate(policy,due,due.plusHours(25),items).billedUnits());
    }
    @Test void proratesPackagePricesByDurationAndQuantityBeforeRoundingTotal() {
        var items=List.of(item(RentalDurationUnit.DAY,3,2,"100"),item(RentalDurationUnit.WEEK,1,1,"70"),item(RentalDurationUnit.MONTH,1,1,"300"));
        var quote=LateFeeCalculator.calculate(new LateFeeCalculator.Policy(true,0),due,due.plusMinutes(1),items);
        assertEquals(new BigDecimal("86.67"),quote.unitAmount());
        assertEquals(new BigDecimal("86.67"),quote.amount());
    }
    @Test void disabledEarlyOrMissingReturnNeverCharges() {
        var items=List.of(item(RentalDurationUnit.DAY,1,1,"50"));
        assertEquals(0,LateFeeCalculator.calculate(LateFeeCalculator.Policy.disabled(),due,due.plusDays(3),items).amount().signum());
        assertEquals(0,LateFeeCalculator.calculate(new LateFeeCalculator.Policy(true,0),due,due.minusHours(1),items).lateMinutes());
        assertEquals(0,LateFeeCalculator.calculate(new LateFeeCalculator.Policy(true,0),due,null,items).amount().signum());
    }
    @Test void timestampsWithDifferentOffsetsCompareAsInstants() {
        var quote=LateFeeCalculator.calculate(new LateFeeCalculator.Policy(true,0),due,due.withOffsetSameInstant(java.time.ZoneOffset.ofHours(7)),List.of(item(RentalDurationUnit.DAY,1,1,"50")));
        assertEquals(0,quote.lateMinutes()); assertEquals(0,quote.billedUnits());
    }
    @Test void readsFractionalGraceHoursAndSafelyHandlesLegacySettings() {
        assertEquals(90,LateFeeCalculator.Policy.fromRentalConfig(Map.of("lateReturnEnabled",true,"lateReturnGraceHours","1.5")).graceMinutes());
        assertEquals(0,LateFeeCalculator.Policy.fromRentalConfig(Map.of("lateReturnEnabled",true,"lateReturnGraceHours","invalid")).graceMinutes());
        assertEquals(0,LateFeeCalculator.Policy.fromRentalConfig(Map.of("lateReturnEnabled",true,"lateReturnGraceHours",-1)).graceMinutes());
        assertFalse(LateFeeCalculator.Policy.fromRentalConfig(null).enabled());
        assertFalse(LateFeeCalculator.Policy.isConfigured(Map.of()));
        assertTrue(LateFeeCalculator.Policy.isConfigured(Map.of("lateReturnEnabled",false)));
    }
}
