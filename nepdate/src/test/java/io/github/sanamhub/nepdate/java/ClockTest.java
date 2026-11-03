package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** The clock functions (ND14 to ND16, ND21 to ND23): every one takes a zone or a clock. */
class ClockTest {
  private static final ZoneId KATHMANDU = NepaliDate.KATHMANDU;

  private static Clock fixed(String instant, ZoneId zone) {
    return Clock.fixed(Instant.parse(instant), zone);
  }

  @Test
  void nepalIsFiveFortyFiveAheadOfUtc() {
    assertEquals(
        ZoneOffset.ofHoursMinutes(5, 45),
        KATHMANDU.getRules().getOffset(Instant.parse("2024-01-01T00:00:00Z")));
  }

  @Test
  void theZoneDecidesTheDate() {
    Clock utc = fixed("2024-07-30T18:30:00Z", ZoneOffset.UTC);
    Clock nepal = fixed("2024-07-30T18:30:00Z", KATHMANDU);
    assertEquals(NepaliDate.parse("2081-04-15"), NepaliDate.today(utc));
    assertEquals(NepaliDate.parse("2081-04-16"), NepaliDate.today(nepal));
    // The ZoneId overloads read the system clock, so they are checked for agreement, not for a
    // date: the fixed clocks above are the deterministic check.
    assertEquals(NepaliDate.of(java.time.LocalDate.now(KATHMANDU)), NepaliDate.today(KATHMANDU));
  }

  @Test
  void theRelativePredicatesUseTheClock() {
    Clock clock = fixed("2024-07-30T18:30:00Z", KATHMANDU);
    NepaliDate today = NepaliDate.parse("2081-04-16");
    assertEquals(true, today.isToday(clock));
    assertEquals(false, today.isYesterday(clock));
    assertEquals(false, today.isTomorrow(clock));
    assertEquals(true, today.plusDays(1).isTomorrow(clock));
    assertEquals(true, today.minusDays(1).isYesterday(clock));
    assertEquals(true, today.isToday(KATHMANDU) || !today.isToday(KATHMANDU));
  }

  @Test
  void theCurrentFactoriesUseTheClock() {
    Clock clock = fixed("2024-07-30T18:30:00Z", KATHMANDU);
    assertEquals(NepaliDateRange.forMonth(2081, 4), NepaliDateRange.currentMonth(clock));
    assertEquals(NepaliDateRange.forFiscalYear(2081), NepaliDateRange.currentFiscalYear(clock));
    assertEquals(NepaliDateRange.forYear(2081), NepaliDateRange.currentYear(clock));
    // The ZoneId overloads read the system clock; check that they agree with today in that zone.
    NepaliDate today = NepaliDate.today(KATHMANDU);
    assertEquals(
        NepaliDateRange.forMonth(today.year(), today.monthNumber()),
        NepaliDateRange.currentMonth(KATHMANDU));
    assertEquals(today.fiscalYear().range(), NepaliDateRange.currentFiscalYear(KATHMANDU));
    assertEquals(NepaliDateRange.forYear(today.year()), NepaliDateRange.currentYear(KATHMANDU));
  }

  @Test
  void aClockOutsideTheDataIsRefused() {
    Clock old = fixed("1830-01-01T00:00:00Z", ZoneOffset.UTC);
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.today(old));
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDate.parse("2081-04-15").isToday(old));
    Clock future = fixed("2200-01-01T00:00:00Z", ZoneOffset.UTC);
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.today(future));
  }

  @Test
  void theRangeHelpersTakeTheSameClock() {
    Clock clock = fixed("2024-07-30T18:30:00Z", KATHMANDU);
    NepaliDate today = NepaliDate.today(clock);
    assertEquals(true, NepaliDateRange.currentMonth(clock).contains(today));
    assertEquals(true, NepaliDateRange.currentFiscalYear(clock).contains(today));
    assertEquals(true, NepaliDateRange.currentYear(clock).contains(today));
  }
}
