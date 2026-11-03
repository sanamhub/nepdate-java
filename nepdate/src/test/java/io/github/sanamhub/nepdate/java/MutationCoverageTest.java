package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The deterministic cases the mutation gate needs beyond the vector tests.
 *
 * <p>PIT excludes the per-row vector classes (ADR-0010), so the boundaries they reach in passing
 * are asserted here directly: a condition at 0 or 1, a page-end month in a date, the custom-pattern
 * parser, and the hash of each value type.
 */
class MutationCoverageTest {
  private static final NepaliDate DATE = NepaliDate.of(2081, 4, 15);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2024-07-30T00:00:00Z"), NepaliDate.KATHMANDU);

  @Test
  void theHashesAreTheDocumentedValues() {
    assertEquals((int) DATE.daysSince(NepaliDate.MIN), DATE.hashCode());
    NepaliDateRange range = NepaliDateRange.of(DATE, DATE.plusDays(5));
    assertEquals(31 * range.start().hashCode() + range.end().hashCode(), range.hashCode());
    assertEquals(DATE.fiscalYear().startYear(), DATE.fiscalYear().hashCode());
    assertNotEquals(DATE.hashCode(), DATE.plusDays(1).hashCode());
  }

  @Test
  void todayAndItsNeighboursAnswerFalseForOtherDays() {
    NepaliDate today = NepaliDate.today(CLOCK);
    assertTrue(today.isToday(CLOCK));
    assertFalse(today.plusDays(1).isToday(CLOCK));
    assertFalse(today.minusDays(1).isToday(CLOCK));
    assertFalse(NepaliDate.MIN.isToday(java.time.ZoneOffset.UTC));
    assertFalse(NepaliDate.MAX.isYesterday(java.time.ZoneOffset.UTC));
    assertFalse(NepaliDate.MIN.isTomorrow(java.time.ZoneOffset.UTC));

    assertTrue(today.minusDays(1).isYesterday(CLOCK));
    assertFalse(today.isYesterday(CLOCK));
    assertFalse(today.plusDays(1).isYesterday(CLOCK));

    assertTrue(today.plusDays(1).isTomorrow(CLOCK));
    assertFalse(today.isTomorrow(CLOCK));
    assertFalse(today.minusDays(1).isTomorrow(CLOCK));
  }

  @Test
  void gregorianDatesAtTheEdgesAndLeapDays() {
    assertEquals(NepaliDate.MIN, NepaliDate.ofGregorian(1844, 4, 11));
    assertEquals(NepaliDate.MAX, NepaliDate.ofGregorian(2143, 4, 15));

    assertEquals(NepDateError.OUT_OF_RANGE, ofGregorianError(1844, 4, 10));
    assertEquals(NepDateError.OUT_OF_RANGE, ofGregorianError(2143, 4, 16));
    assertEquals(NepDateError.OUT_OF_RANGE, ofGregorianError(1844, 2, 29));

    assertEquals(NepDateError.INVALID_GREGORIAN, ofGregorianError(1844, 0, 1));
    assertEquals(NepDateError.INVALID_GREGORIAN, ofGregorianError(1844, 13, 1));
    assertEquals(NepDateError.INVALID_GREGORIAN, ofGregorianError(1844, 4, 0));
    assertEquals(NepDateError.INVALID_GREGORIAN, ofGregorianError(1844, 4, 31));
    assertEquals(NepDateError.INVALID_GREGORIAN, ofGregorianError(1900, 2, 29));
    assertEquals(NepDateError.INVALID_GREGORIAN, ofGregorianError(2100, 2, 29));

    for (int[] row :
        new int[][] {
          {1844, 4, 11},
          {1900, 2, 28},
          {2000, 2, 29},
          {2024, 1, 1},
          {2024, 12, 31},
          {2100, 2, 28},
          {2143, 4, 15},
        }) {
      NepaliDate date = NepaliDate.ofGregorian(row[0], row[1], row[2]);
      assertEquals(row[0], date.gregorianYear());
      assertEquals(row[1], date.gregorianMonth());
      assertEquals(row[2], date.gregorianDay());
      assertEquals(LocalDate.of(row[0], row[1], row[2]), date.toLocalDate());
    }

    // A whole leap year and a whole common year, so every month boundary of civil_from_days runs.
    for (int[] year : new int[][] {{2024, 366}, {1900, 365}}) {
      for (int dayOfYear = 1; dayOfYear <= year[1]; dayOfYear++) {
        LocalDate ad = LocalDate.ofYearDay(year[0], dayOfYear);
        NepaliDate date =
            NepaliDate.ofGregorian(ad.getYear(), ad.getMonthValue(), ad.getDayOfMonth());
        assertEquals(ad.getYear(), date.gregorianYear());
        assertEquals(ad.getMonthValue(), date.gregorianMonth());
        assertEquals(ad.getDayOfMonth(), date.gregorianDay());
      }
    }
  }

  @Test
  void monthArithmeticHandlesEverySpillAndTheRangeEnds() {
    for (int year = 2080; year <= 2082; year++) {
      for (int month = 1; month <= 12; month++) {
        int length = NepaliDate.of(year, month, 1).monthLength();
        for (int day = 1; day <= length; day++) {
          NepaliDate date = NepaliDate.of(year, month, day);

          int nextYear = month == 12 ? year + 1 : year;
          int nextMonth = month == 12 ? 1 : month + 1;
          int nextLength = NepaliDate.of(nextYear, nextMonth, 1).monthLength();
          NepaliDate spilled = date.plusMonths(1, MonthOverflow.SPILL);
          int expectedYear;
          int expectedMonth;
          int expectedDay;
          if (day <= nextLength) {
            expectedYear = nextYear;
            expectedMonth = nextMonth;
            expectedDay = day;
          } else {
            expectedYear = nextMonth == 12 ? nextYear + 1 : nextYear;
            expectedMonth = nextMonth == 12 ? 1 : nextMonth + 1;
            expectedDay = day - nextLength;
          }
          assertEquals(NepaliDate.of(expectedYear, expectedMonth, expectedDay), spilled);
          assertEquals(expectedYear, spilled.year());
          assertEquals(expectedMonth, spilled.monthNumber());
          assertEquals(expectedDay, spilled.day());
          NepaliDate clamped = date.plusMonths(1, MonthOverflow.CLAMP);
          assertEquals(NepaliDate.of(nextYear, nextMonth, Math.min(day, nextLength)), clamped);
          assertEquals(nextYear, clamped.year());
          assertEquals(nextMonth, clamped.monthNumber());
          assertEquals(Math.min(day, nextLength), clamped.day());
        }
        for (int day = 1; day <= length; day++) {
          NepaliDate date = NepaliDate.of(year, month, day);
          int previousYear = month == 1 ? year - 1 : year;
          int previousMonth = month == 1 ? 12 : month - 1;
          int previousLength = NepaliDate.of(previousYear, previousMonth, 1).monthLength();
          int expectedYear;
          int expectedMonth;
          int expectedDay;
          if (day <= previousLength) {
            expectedYear = previousYear;
            expectedMonth = previousMonth;
            expectedDay = day;
          } else {
            expectedYear = year;
            expectedMonth = month;
            expectedDay = day - previousLength;
          }
          NepaliDate spilled = date.plusMonths(-1, MonthOverflow.SPILL);
          assertEquals(NepaliDate.of(expectedYear, expectedMonth, expectedDay), spilled);
          assertEquals(expectedYear, spilled.year());
          assertEquals(expectedMonth, spilled.monthNumber());
          assertEquals(expectedDay, spilled.day());
        }
      }
    }

    assertEquals(
        DATE, DATE.plusMonths(0, MonthOverflow.SPILL), "zero months must not move the day");
    NepDateException beforeStart =
        assertThrows(NepDateException.class, () -> NepaliDate.MIN.plusMonths(-1));
    assertEquals(NepDateError.OUT_OF_RANGE, beforeStart.error());
    NepDateException afterEnd =
        assertThrows(NepDateException.class, () -> NepaliDate.MAX.plusMonths(1));
    assertEquals(NepDateError.OUT_OF_RANGE, afterEnd.error());
    NepDateException afterEndSpill =
        assertThrows(
            NepDateException.class, () -> NepaliDate.MAX.plusMonths(1, MonthOverflow.SPILL));
    assertEquals(NepDateError.OUT_OF_RANGE, afterEndSpill.error());
    assertThrows(NepDateException.class, () -> NepaliDate.MIN.plusMonths(CalendarTable.MONTHS));
    assertThrows(NepDateException.class, () -> NepaliDate.MAX.plusMonths(-CalendarTable.MONTHS));

    assertEquals(
        NepaliDate.of(2082, 4, 31), NepaliDate.of(2081, 4, 32).plusYears(1, MonthOverflow.CLAMP));
    assertEquals(NepaliDate.of(2080, 4, 15), NepaliDate.of(2081, 4, 15).plusYears(-1));
    assertThrows(NepDateException.class, () -> NepaliDate.of(1901, 1, 1).plusYears(-1));
    assertEquals(NepaliDate.of(2199, 1, 1), NepaliDate.of(1901, 1, 1).plusYears(298));
    assertThrows(NepDateException.class, () -> NepaliDate.of(1901, 1, 1).plusYears(299));
    assertThrows(NepDateException.class, () -> NepaliDate.of(2199, 1, 1).plusYears(299));
  }

  @Test
  void theCalendarDistanceCoversZeroOneMonthAndOneDay() {
    assertEquals(Period.ZERO, DATE.until(DATE));
    assertEquals(Period.ofMonths(1), DATE.until(DATE.plusMonths(1)));
    assertEquals(Period.ofDays(1), DATE.until(DATE.plusDays(1)));
    assertEquals(Period.of(0, 1, 1), DATE.until(DATE.plusMonths(1).plusDays(1)));
    assertEquals(Period.ofMonths(-1), DATE.until(DATE.plusMonths(-1)));
    assertEquals(-1L, DATE.daysUntil(DATE.minusDays(1)));
  }

  @Test
  void customPatternsCoverEscapesQuotesAndUnknownLetters() {
    assertEquals("2081/04/15", DATE.format("yyyy/MM/dd"));
    assertEquals("Shrawan 15, 2081", DATE.format("D"));
    assertEquals("2081-04-15", DATE.format("s"));
    assertEquals("2081/04/15", DATE.format("g"));
    assertEquals("2081/04/15", DATE.format("G"));
    assertEquals("2081M4", DATE.format("yyyy\\MM"));
    assertEquals("2081 at 04", DATE.format("yyyy 'at' MM"));
    assertEquals("2081 at", DATE.format("yyyy 'at"));
    assertEquals("2081\\", DATE.format("yyyy\\"));
    assertEquals("Q", DATE.format("Q"));
    assertEquals("Shr", DATE.format("MMM"));
    assertEquals("Shrawan", DATE.format("MMMM"));
    assertEquals("4", DATE.format("M"));
    assertEquals("04", DATE.format("MM"));
    assertEquals("15/4", DATE.format("d/M"));
    assertEquals("15/04", DATE.format("dd/MM"));
    NepaliDate singleDigit = NepaliDate.of(2081, 4, 5);
    assertEquals("5/4", singleDigit.format("d/M"));
    assertEquals("05/04", singleDigit.format("dd/MM"));
    assertEquals("Shrawan 05, 2081", singleDigit.toLongString(LongDateOptions.DEFAULT));
    assertEquals("2081/04/05", singleDigit.toShortString());
    assertEquals("81", DATE.format("yy"));
    assertEquals("२०८१/०४/१५", DATE.format("yyyy/MM/dd", Language.NEPALI));

    assertEquals("2081/04/15", DATE.toShortString(DateOrder.YMD, Separator.SLASH, true));
    assertEquals("2081/4/15", DATE.toShortString(DateOrder.YMD, Separator.SLASH, false));
    assertEquals("2081/04/15", DATE.toShortString());
    assertEquals("Shrawan 15, 2081", DATE.toLongString(LongDateOptions.DEFAULT));
    assertEquals("Shrawan 15", DATE.toLongString(LongDateOptions.DEFAULT.withYear(false)));
    assertEquals("Shrawan 15, 2081", DATE.toLongString(LongDateOptions.DEFAULT.withWeekday(false)));
    assertEquals(
        "Tuesday, Shrawan 15, 2081", DATE.toLongString(LongDateOptions.DEFAULT.withWeekday(true)));
    assertEquals("साउन १५, २०८१", DATE.toLongString(LongDateOptions.DEFAULT, Language.NEPALI));

    char[] exact = new char[10];
    assertTrue(DATE.tryFormat(exact));
    assertEquals("2081/04/15", new String(exact));
    char[] small = {'x', 'x', 'x', 'x', 'x'};
    assertFalse(DATE.tryFormat(small));
    assertEquals("xxxxx", new String(small));
  }

  @Test
  void enumLookupsAtTheirBoundaries() {
    assertEquals(Weekday.SUNDAY, Weekday.ofIndex(0));
    assertEquals(Weekday.SATURDAY, Weekday.ofIndex(6));
    assertEquals("Tue", Weekday.TUESDAY.shortName(Language.ENGLISH));
    assertEquals("मङ्गल", Weekday.TUESDAY.shortName(Language.NEPALI));
    assertThrows(IllegalArgumentException.class, () -> Weekday.ofIndex(-1));
    assertThrows(IllegalArgumentException.class, () -> Weekday.ofIndex(7));

    assertEquals(Month.BAISHAKH, Month.of(1));
    assertEquals(Month.CHAITRA, Month.of(12));
    assertThrows(NepDateException.class, () -> Month.of(0));
    assertThrows(NepDateException.class, () -> Month.of(13));
  }

  @Test
  void rangesMeetAndPassAtTheEndpoints() {
    NepaliDate day1 = NepaliDate.of(2081, 4, 1);
    NepaliDateRange first = NepaliDateRange.of(day1, day1.plusDays(4));
    NepaliDateRange overlapping = NepaliDateRange.of(day1.plusDays(4), day1.plusDays(8));
    NepaliDateRange after = NepaliDateRange.of(day1.plusDays(5), day1.plusDays(8));
    NepaliDateRange before = NepaliDateRange.of(day1.minusDays(5), day1.minusDays(1));

    assertTrue(first.overlaps(overlapping));
    assertTrue(first.overlaps(first));
    assertTrue(overlapping.overlaps(first));
    assertEquals(1, first.intersect(overlapping).orElseThrow().length());
    assertEquals(day1.plusDays(4), first.intersect(overlapping).orElseThrow().start());

    assertFalse(first.overlaps(after));
    assertTrue(first.intersect(after).isEmpty());
    assertTrue(first.isAdjacentTo(after));
    assertTrue(first.isAdjacentTo(before));
    assertFalse(first.overlaps(before));
  }

  @Test
  void fiscalYearBoundaries() {
    FiscalYear fiscal = FiscalYear.of(2081);
    assertEquals(NepaliDate.of(2082, 3, 1).lastDayOfMonth(), fiscal.end());
    assertEquals(NepaliDate.of(2081, 4, 1), fiscal.start());
    assertEquals("2081/82", fiscal.label());
    assertEquals("२०८१/८२", fiscal.label(Language.NEPALI));
    assertEquals(fiscal.end(), fiscal.quarterRange(Quarter.Q4).end());
    assertEquals(fiscal.start(), fiscal.quarterRange(Quarter.Q1).start());
    assertEquals("2089/90", FiscalYear.of(2089).label());
    assertEquals("२०८९/९०", FiscalYear.of(2089).label(Language.NEPALI));
    assertEquals(NepaliDate.of(2199, 3, 1).lastDayOfMonth(), FiscalYear.of(2198).end());
    assertEquals(FiscalYear.of(2198).end(), FiscalYear.of(2198).quarterRange(Quarter.Q4).end());
    assertThrows(NepDateException.class, () -> FiscalYear.of(2199).quarterRange(Quarter.Q4));
    assertNotEquals(fiscal, FiscalYear.of(2082));
    assertThrows(NepDateException.class, () -> FiscalYear.of(2199).end());
  }

  @Test
  void lenientParsingAtTheTokenEdges() {
    assertEquals(DATE, NepaliDate.parseLenient("2081-04-15"));
    assertEquals(DATE, NepaliDate.parseLenient("15 Shrawan 2081"));
    assertEquals(DATE, NepaliDate.parseLenient("Shrawan 15, 2081"));
    assertEquals(DATE, NepaliDate.parseLenient("2081/4/15 BS"));
    assertEquals(NepaliDate.of(2081, 4, 15), NepaliDate.parseLenient("१५ साउन २०८१"));
    assertEquals(DATE, NepaliDate.parseLenient("2081.04.15"));
    assertEquals(DATE, NepaliDate.parseLenient("2081_04_15"));
    assertEquals(DATE, NepaliDate.parseLenient("2081|04|15"));
    assertEquals(DATE, NepaliDate.parseLenient(" 2081 / 04 / 15 "));
    assertEquals(DATE, NepaliDate.parseLenient("SHRAWAN 15, 2081"));
    assertEquals(DATE, NepaliDate.parseLenient("shrawan 15, 2081"));
    assertEquals(DATE, NepaliDate.parseLenient("/2081/04/15/"));

    for (String text :
        new String[] {
          "", "2081", "2081/04", "2081/04/15/16", "2081/04/00", "81/04/15", "Shrawan 2081"
        }) {
      assertThrows(NepDateException.class, () -> NepaliDate.parseLenient(text), text);
      assertTrue(NepaliDate.tryParseLenient(text).isEmpty(), text);
    }
    assertEquals(NepDateError.AMBIGUOUS, lenientError("0/0/0"));
  }

  @Test
  void strictParsingAtTheCharacterAndGroupEdges() {
    assertEquals(DATE, NepaliDate.parse("2081/04/15"));
    assertEquals(DATE, NepaliDate.parse("2081-4-15"));
    assertEquals(DATE, NepaliDate.parse("२०८१/०४/१५"));
    assertEquals(DATE, NepaliDate.parse("2081.04.15"));
    assertEquals(DATE, NepaliDate.parse("2081\\\\04\\\\15"));
    assertEquals(DATE, NepaliDate.parse("2081//04//15"));
    assertEquals(DATE, NepaliDate.parse("2081/04/15/"));

    assertEquals(NepDateError.INVALID_CHARACTER, parseError("2081:04:15"));
    assertEquals(NepDateError.INVALID_CHARACTER, parseError("2081/04/15x"));
    assertEquals(NepDateError.NUMBER_TOO_LONG, parseError("2081/04/12345"));
    assertEquals(NepDateError.WRONG_GROUP_COUNT, parseError("2081/04/15/16"));
    assertEquals(NepDateError.WRONG_GROUP_COUNT, parseError("2081/04/"));
    assertEquals(NepDateError.OUT_OF_RANGE, parseError("0/0/0"));
    assertTrue(NepaliDate.tryParse("0/0/0").isEmpty());
    assertEquals(
        NepaliDate.of(2081, 9, 9),
        NepaliDate.parse("\u0968\u0966\u096E\u0967/\u0966\u096F/\u0966\u096F"));
    assertEquals(NepDateError.INVALID_MONTH, parseError("2081/00/15"));
    assertEquals(NepDateError.INVALID_DAY, parseError("2081/04/33"));
    assertEquals(
        NepaliDate.of(2081, 9, 9),
        NepaliDate.parse("\u0968\u0966\u096E\u0967/\u0966\u096F/\u0966\u096F"));
    assertEquals(NepaliDate.of(2081, 10, 10), NepaliDate.parse("2081/10/10"));

    assertTrue(NepaliDate.tryParse("2081/04/15").isPresent());
    assertTrue(NepaliDate.tryParse("2081/04/33").isEmpty());
    assertTrue(NepaliDate.tryParse("not a date").isEmpty());
    Optional<NepaliDate> parsed = NepaliDate.tryParse("२०८१/०४/१५");
    assertTrue(parsed.isPresent());
    assertEquals(DATE, parsed.orElseThrow());
  }

  private static NepDateError ofGregorianError(int year, int month, int day) {
    return assertThrows(NepDateException.class, () -> NepaliDate.ofGregorian(year, month, day))
        .error();
  }

  private static NepDateError parseError(String text) {
    return assertThrows(NepDateException.class, () -> NepaliDate.parse(text)).error();
  }

  private static NepDateError lenientError(String text) {
    return assertThrows(NepDateException.class, () -> NepaliDate.parseLenient(text)).error();
  }
}
