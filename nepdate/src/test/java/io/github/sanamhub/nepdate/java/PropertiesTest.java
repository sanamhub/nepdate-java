package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Period;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

/** Properties that hold for every supported date, not only for the vector rows. */
class PropertiesTest {
  private static final int LAST = CalendarTable.MAX_SERIAL;

  @Property
  void everySerialRoundTripsThroughItsFields(@ForAll @IntRange(min = 0, max = LAST) int serial) {
    NepaliDate date = NepaliDate.ofSerial(serial);
    assertEquals(date, NepaliDate.of(date.year(), date.monthNumber(), date.day()));
    assertEquals(date, NepaliDate.of(date.toLocalDate()));
    assertEquals(date, NepaliDate.ofYmd(date.toYmd()));
    assertEquals(date.toIsoString(), NepaliDate.parse(date.toIsoString()).toIsoString());
    assertEquals(serial, (int) date.daysSince(NepaliDate.MIN));
  }

  @Property
  void steppingForwardAndBackReturnsTheSameDay(
      @ForAll @IntRange(min = 0, max = LAST) int serial,
      @ForAll @IntRange(min = -2000, max = 2000) int days) {
    NepaliDate date = NepaliDate.ofSerial(serial);
    long target = serial + (long) days;
    if (target < 0 || target > LAST) {
      Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> date.plusDays(days));
    } else {
      assertEquals(date, date.plusDays(days).minusDays(days));
      assertEquals((int) target, (int) date.plusDays(days).daysSince(NepaliDate.MIN));
    }
  }

  @Property
  void monthArithmeticKeepsTheDayInsideTheTargetMonth(
      @ForAll @IntRange(min = 0, max = LAST) int serial,
      @ForAll @IntRange(min = -2400, max = 2400) int months) {
    NepaliDate date = NepaliDate.ofSerial(serial);
    int targetMonths =
        (date.year() - CalendarTable.MIN_YEAR) * 12 + date.monthNumber() - 1 + months;
    if (targetMonths < 0 || targetMonths >= CalendarTable.MONTHS) {
      Vectors.assertThrows(
          NepDateError.OUT_OF_RANGE, () -> date.plusMonths(months, MonthOverflow.CLAMP));
      return;
    }
    NepaliDate clamped = date.plusMonths(months, MonthOverflow.CLAMP);
    assertTrue(clamped.day() >= 1 && clamped.day() <= clamped.monthLength());
    assertEquals(
        Math.min(date.day(), clamped.monthLength()),
        clamped.day(),
        "clamping uses the smaller of the two days");
    try {
      NepaliDate spilled = date.plusMonths(months, MonthOverflow.SPILL);
      assertTrue(spilled.day() >= 1 && spilled.day() <= spilled.monthLength());
      assertTrue(spilled.compareTo(clamped) >= 0);
    } catch (NepDateException spill) {
      // Only the last supported month can spill past the end of the data.
      assertEquals(NepDateError.OUT_OF_RANGE, spill.error());
      assertEquals(CalendarTable.MONTHS - 1, targetMonths);
    }
  }

  @Property
  void splitsCoverTheRangeWithoutGaps(
      @ForAll @IntRange(min = 0, max = LAST) int first,
      @ForAll @IntRange(min = 0, max = LAST) int second) {
    NepaliDateRange range =
        NepaliDateRange.of(
            NepaliDate.ofSerial(Math.min(first, second)),
            NepaliDate.ofSerial(Math.max(first, second)));
    for (List<NepaliDateRange> pieces :
        List.of(range.splitByMonth(), range.splitByFiscalQuarter())) {
      assertEquals(range.start(), pieces.get(0).start());
      assertEquals(range.end(), pieces.get(pieces.size() - 1).end());
      int covered = 0;
      for (int i = 0; i < pieces.size(); i++) {
        covered += pieces.get(i).length();
        if (i > 0) {
          assertEquals(pieces.get(i - 1).end().plusDays(1), pieces.get(i).start());
        }
      }
      assertEquals(range.length(), covered);
    }
  }

  @Property
  void dayDifferencesAreAntisymmetric(
      @ForAll @IntRange(min = 0, max = LAST) int first,
      @ForAll @IntRange(min = 0, max = LAST) int second) {
    NepaliDate a = NepaliDate.ofSerial(first);
    NepaliDate b = NepaliDate.ofSerial(second);
    assertEquals(a.daysUntil(b), -b.daysUntil(a));
    assertEquals(a.daysSince(b), -b.daysSince(a));
    assertEquals(a.daysUntil(b), b.daysSince(a));
    assertEquals(second - first, a.daysUntil(b));
  }

  @Property
  void theCalendarBreakdownReconstructsTheLaterDate(
      @ForAll @IntRange(min = 0, max = LAST) int first,
      @ForAll @IntRange(min = 0, max = LAST) int second) {
    NepaliDate a = NepaliDate.ofSerial(first);
    NepaliDate b = NepaliDate.ofSerial(second);
    Period period = a.until(b);
    assertTrue(Math.abs(period.getDays()) <= 32);
    assertTrue(Math.abs(period.getMonths()) <= 11);
    // The breakdown is anchored at the earlier date, the direction ALGORITHM section 8a uses.
    NepaliDate earlier = a.compareTo(b) <= 0 ? a : b;
    NepaliDate later = a.compareTo(b) <= 0 ? b : a;
    Period forward = earlier.until(later);
    int months = forward.getYears() * 12 + forward.getMonths();
    assertEquals(
        later, earlier.plusMonths(months, MonthOverflow.CLAMP).plusDays(forward.getDays()));
    assertEquals(period.negated(), b.until(a));
  }

  @Property
  void formattingAndParsingRoundTrip(@ForAll @IntRange(min = 0, max = LAST) int serial) {
    NepaliDate date = NepaliDate.ofSerial(serial);
    for (DateOrder order : DateOrder.values()) {
      for (Separator separator : Separator.values()) {
        String text = date.toShortString(order, separator, true);
        if (order == DateOrder.YMD) {
          assertEquals(date, NepaliDate.parse(text));
        }
      }
    }
    assertEquals(date, NepaliDate.parse(date.format("yyyy-MM-dd")));
    assertEquals(date, NepaliDate.parseLenient(date.toLongString()));
    char[] buffer = new char[32];
    assertTrue(date.tryFormat(buffer, "yyyy-MM-dd"));
    assertEquals(date.toIsoString(), new String(buffer, 0, 10));
  }
}
