package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Every supported day, through the conversion and the invariants (ALGORITHM sections 4 to 7). */
class ExhaustiveTest {
  @Test
  void everySerialRoundTrips() {
    NepaliDate previous = null;
    int previousDayOfYear = 0;
    for (int serial = 0; serial <= CalendarTable.MAX_SERIAL; serial++) {
      NepaliDate date = NepaliDate.ofSerial(serial);
      LocalDate local = date.toLocalDate();

      assertEquals(local.getYear(), date.gregorianYear(), "AD year at serial " + serial);
      assertEquals(local.getMonthValue(), date.gregorianMonth(), "AD month at serial " + serial);
      assertEquals(local.getDayOfMonth(), date.gregorianDay(), "AD day at serial " + serial);
      assertEquals(
          local.getDayOfWeek(), date.weekday().toDayOfWeek(), "weekday at serial " + serial);
      assertEquals(serial, (int) date.daysSince(NepaliDate.MIN), "serial of " + date);
      assertEquals(serial, (int) date.toEpochDay() - CalendarTable.EPOCH_UNIX_DAYS);
      assertEquals(date, NepaliDate.of(date.year(), date.monthNumber(), date.day()));
      assertEquals(date, NepaliDate.of(local));
      assertEquals(date.toIsoString(), NepaliDate.parse(date.toIsoString()).toIsoString());
      assertEquals(date.toIsoString(), NepaliDate.parseLenient(date.toIsoString()).toIsoString());
      assertEquals(date.toYmd(), date.year() * 10000 + date.monthNumber() * 100 + date.day());

      assertTrue(date.monthLength() >= 29 && date.monthLength() <= 32, "month length of " + date);
      assertTrue(date.day() >= 1 && date.day() <= date.monthLength(), "day of " + date);
      assertTrue(
          date.dayOfYear() >= 1 && date.dayOfYear() <= date.yearLength(), "day of year of " + date);
      assertEquals(date.monthLength(), date.lastDayOfMonth().day());
      assertEquals(1, date.firstDayOfMonth().day());
      assertTrue(date.firstDayOfMonth().compareTo(date) <= 0);
      assertTrue(date.lastDayOfMonth().compareTo(date) >= 0);

      if (previous != null) {
        assertEquals(previous.plusDays(1), date);
        assertEquals(1, (int) date.daysSince(previous));
        int expectedDayOfYear =
            date.monthNumber() == 1 && date.day() == 1 ? 1 : previousDayOfYear + 1;
        assertEquals(expectedDayOfYear, date.dayOfYear(), "day of year at " + date);
      }
      previous = date;
      previousDayOfYear = date.dayOfYear();
    }
    assertEquals(CalendarTable.MAX_SERIAL + 1, 109212);
  }

  @Test
  void everyMonthAndYearLengthAddsUp() {
    for (int year = CalendarTable.MIN_YEAR; year <= CalendarTable.MAX_YEAR; year++) {
      int sum = 0;
      for (int month = 1; month <= 12; month++) {
        int length = Calendar.monthLength(year, month);
        assertTrue(length >= 29 && length <= 32);
        sum += length;
        assertEquals(length, NepaliDate.of(year, month, 1).monthLength());
        assertEquals(length, NepaliDate.of(year, month, length).day());
        final int checkedYear = year;
        final int checkedMonth = month;
        final int tooLong = length + 1;
        Vectors.assertThrows(
            NepDateError.INVALID_DAY, () -> NepaliDate.of(checkedYear, checkedMonth, tooLong));
      }
      assertEquals(sum, Calendar.yearLength(year), "year length of " + year);
      assertEquals(sum, NepaliDate.of(year, 1, 1).yearLength());
    }
  }

  @Test
  void theSerialEstimateFindsEveryMonthStart() {
    for (int year = CalendarTable.MIN_YEAR; year <= CalendarTable.MAX_YEAR; year++) {
      for (int month = 1; month <= 12; month++) {
        NepaliDate first = NepaliDate.of(year, month, 1);
        assertEquals(year, first.year());
        assertEquals(month, first.monthNumber());
        assertEquals(1, first.day());
      }
    }
  }
}
