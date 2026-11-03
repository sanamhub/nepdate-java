package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The public surface that the vector files do not reach directly. */
class ApiTest {
  @Test
  void constructionReportsTheFirstFailureInOrder() {
    NepDateException year = assertThrows(NepDateException.class, () -> NepaliDate.of(1900, 13, 40));
    assertEquals(NepDateError.OUT_OF_RANGE, year.error());
    assertEquals(0, year.monthLength());
    assertEquals(true, year.getMessage().contains("1900"));

    NepDateException month =
        assertThrows(NepDateException.class, () -> NepaliDate.of(2081, 13, 40));
    assertEquals(NepDateError.INVALID_MONTH, month.error());

    NepDateException day = assertThrows(NepDateException.class, () -> NepaliDate.of(2081, 4, 40));
    assertEquals(NepDateError.INVALID_DAY, day.error());
    assertEquals(32, day.monthLength());
    assertTrue(day instanceof IllegalArgumentException);
  }

  @Test
  void theMonthConstantFactoryWorks() {
    assertEquals(NepaliDate.of(2081, 4, 15), NepaliDate.of(2081, Month.SHRAWAN, 15));
    Vectors.assertThrows(NepDateError.INVALID_DAY, () -> NepaliDate.of(2081, Month.SHRAWAN, 33));
  }

  @Test
  void gregorianConstructionValidatesTheAdCalendarFirst() {
    Vectors.assertThrows(NepDateError.INVALID_GREGORIAN, () -> NepaliDate.ofGregorian(2023, 2, 29));
    Vectors.assertThrows(NepDateError.INVALID_GREGORIAN, () -> NepaliDate.ofGregorian(2024, 13, 1));
    Vectors.assertThrows(NepDateError.INVALID_GREGORIAN, () -> NepaliDate.ofGregorian(2024, 4, 31));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.ofGregorian(1844, 4, 10));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.ofGregorian(2143, 4, 16));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.ofGregorian(1000, 1, 1));
    assertEquals(NepaliDate.parse("2081-04-15"), NepaliDate.ofGregorian(2024, 7, 30));
    assertEquals(NepaliDate.parse("2081-04-15"), NepaliDate.of(LocalDate.of(2024, 7, 30)));
  }

  @Test
  void epochDayConstructionChecksTheRange() {
    assertEquals(NepaliDate.MIN, NepaliDate.ofEpochDay(-45920));
    assertEquals(NepaliDate.MAX, NepaliDate.ofEpochDay(63291));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.ofEpochDay(-45921));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.ofEpochDay(63292));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.ofEpochDay(Long.MAX_VALUE));
    assertEquals(-45920, NepaliDate.MIN.toEpochDay());
    assertEquals(63291, NepaliDate.MAX.toEpochDay());
  }

  @Test
  void accessorsAndDerivedValues() {
    NepaliDate date = NepaliDate.parse("2081-04-15");
    assertEquals(2081, date.year());
    assertEquals(4, date.monthNumber());
    assertEquals(Month.SHRAWAN, date.month());
    assertEquals("Shrawan", date.month().english());
    assertEquals("Shr", date.month().shortEnglish());
    assertEquals("साउन", date.month().nepali());
    assertEquals("साउन", date.month().name(Language.NEPALI));
    assertEquals("Shr", date.month().shortName(Language.ENGLISH));
    assertEquals("साउन", date.month().shortName(Language.NEPALI));
    assertEquals(15, date.day());
    assertEquals(Weekday.TUESDAY, date.weekday());
    assertEquals(java.time.DayOfWeek.TUESDAY, date.weekday().toDayOfWeek());
    assertEquals("Tuesday", date.weekday().english());
    assertEquals("Tue", date.weekday().shortEnglish());
    assertEquals("मङ्गलवार", date.weekday().nepali());
    assertEquals("मङ्गल", date.weekday().shortNepali());
    assertEquals("मङ्गलवार", date.weekday().name(Language.NEPALI));
    assertEquals(109, date.dayOfYear());
    assertEquals(32, date.monthLength());
    assertEquals(366, date.yearLength());
    assertEquals(NepaliDate.parse("2081-04-01"), date.firstDayOfMonth());
    assertEquals(NepaliDate.parse("2081-04-32"), date.lastDayOfMonth());
    assertEquals(LocalDate.of(2024, 7, 30), date.toLocalDate());
    assertEquals(2024, date.gregorianYear());
    assertEquals(7, date.gregorianMonth());
    assertEquals(30, date.gregorianDay());
    assertEquals(20810415, date.toYmd());
  }

  @Test
  void equalityOrderingAndHashing() {
    NepaliDate first = NepaliDate.parse("2081-04-15");
    NepaliDate same = NepaliDate.of(2081, 4, 15);
    NepaliDate later = NepaliDate.parse("2081-04-16");
    assertEquals(first, same);
    assertEquals(first.hashCode(), same.hashCode());
    assertNotEquals(first, later);
    assertEquals(false, first.equals(null));
    assertEquals(false, first.equals("2081-04-15"));
    assertEquals(0, first.compareTo(same));
    assertTrue(first.compareTo(later) < 0);
    assertTrue(later.compareTo(first) > 0);
    List<NepaliDate> dates = new ArrayList<>(List.of(later, first));
    Collections.sort(dates);
    assertEquals(List.of(first, later), dates);
    assertEquals(NepaliDate.MIN, Collections.min(List.of(later, NepaliDate.MIN)));
    assertEquals(NepaliDate.MAX, Collections.max(List.of(later, NepaliDate.MAX)));
  }

  @Test
  void everyFormattingEntryPoint() {
    NepaliDate date = NepaliDate.parse("2081-04-15");
    assertEquals("2081/04/15", date.toShortString());
    assertEquals("2081/04/15", date.toShortString(DateOrder.YMD, Separator.SLASH, true));
    assertEquals("2081-4-15", date.toShortString(DateOrder.YMD, Separator.DASH, false));
    assertEquals(
        "१५/०४/२०८१", date.toShortString(DateOrder.DMY, Separator.SLASH, true, Language.NEPALI));
    assertEquals("Shrawan 15, 2081", date.toLongString());
    assertEquals("Shrawan 15, 2081", date.toLongString(LongDateOptions.DEFAULT));
    assertEquals(
        "Tuesday, Shrawan 15, 2081", date.toLongString(LongDateOptions.DEFAULT.withWeekday(true)));
    assertEquals("Tuesday, Shrawan 15", date.toLongString(new LongDateOptions(true, false, true)));
    assertEquals("Tuesday, Shrawan 15", date.toLongString(new LongDateOptions(true, false, false)));
    assertEquals("साउन १५, २०८१", date.toLongString(LongDateOptions.DEFAULT, Language.NEPALI));
    assertEquals("साउन १५, २०८१", date.format("MMMM d, yyyy", Language.NEPALI));
    StringBuilder out = new StringBuilder();
    assertEquals(out, date.appendTo(out));
    assertEquals("2081/04/15", out.toString());
    StringBuilder pattern = date.appendTo(new StringBuilder(), "yyyy-MM-dd");
    assertEquals("2081-04-15", pattern.toString());
    char[] buffer = new char[10];
    assertEquals(true, date.tryFormat(buffer));
    assertEquals("2081/04/15", new String(buffer));
    assertEquals(false, date.tryFormat(new char[9]));
    assertEquals(true, date.tryFormat(new char[10], "yyyy-MM-dd"));
    assertEquals(false, date.tryFormat(new char[9], "yyyy-MM-dd"));
    assertEquals(true, date.tryFormat(new char[20], "MMMM d, yyyy", Language.ENGLISH));
  }

  @Test
  void longDateOptionsAreValues() {
    LongDateOptions options = LongDateOptions.DEFAULT;
    assertEquals(false, options.weekday());
    assertEquals(true, options.year());
    assertEquals(true, options.pad());
    assertEquals(new LongDateOptions(true, true, true), options.withWeekday(true));
    assertEquals(new LongDateOptions(false, false, true), options.withYear(false));
    assertEquals(new LongDateOptions(false, true, false), options.withPad(false));
  }

  @Test
  void fiscalYearAndQuarter() {
    NepaliDate date = NepaliDate.parse("2081-04-15");
    assertEquals(FiscalYear.of(2081), date.fiscalYear());
    assertEquals(2081, date.fiscalYear().startYear());
    assertEquals(NepaliDate.parse("2081-04-01"), date.fiscalYear().start());
    assertEquals(NepaliDate.parse("2082-03-32"), date.fiscalYear().end());
    assertEquals("2081/82", date.fiscalYear().label());
    assertEquals("२०८१/८२", date.fiscalYear().label(Language.NEPALI));
    assertEquals("2081/82", date.fiscalYear().toString());
    assertEquals(Quarter.Q1, date.quarter());
    assertEquals(Quarter.Q1, Quarter.ofMonth(4));
    assertEquals(Quarter.Q4, Quarter.ofMonth(1));
    assertEquals(Quarter.Q2, Quarter.ofMonth(9));
    assertEquals(Quarter.Q3, Quarter.ofMonth(12));
    assertEquals(List.of(Month.SHRAWAN, Month.BHADRA, Month.ASHOJ), Quarter.Q1.months());
    assertEquals(
        NepaliDateRange.of(NepaliDate.parse("2081-04-01"), NepaliDate.parse("2081-06-30")),
        date.fiscalYear().quarterRange(Quarter.Q1));
    assertEquals(FiscalYear.of(2081), FiscalYear.containing(date));
    assertEquals(FiscalYear.of(2080), FiscalYear.containing(NepaliDate.parse("2081-03-30")));
    assertTrue(FiscalYear.of(2081).compareTo(FiscalYear.of(2082)) < 0);
    assertEquals(FiscalYear.of(2081), FiscalYear.of(2081));
    assertEquals(FiscalYear.of(2081).hashCode(), FiscalYear.of(2081).hashCode());
    assertNotEquals(FiscalYear.of(2081), FiscalYear.of(2082));
  }

  @Test
  void theCalendarBreakdownAndDayDifferences() {
    NepaliDate earlier = NepaliDate.parse("2081-01-31");
    NepaliDate later = NepaliDate.parse("2081-02-30");
    assertEquals(30, earlier.daysUntil(later));
    assertEquals(-30, earlier.daysSince(later));
    assertEquals(Period.of(0, 0, 30), earlier.until(later));
    assertEquals(Period.of(0, 0, -30), later.until(earlier));
    assertEquals(Period.ZERO, earlier.until(earlier));
  }

  @Test
  void batchConversionKeepsOrderAndRejectsABadElement() {
    List<LocalDate> gregorian = List.of(LocalDate.of(2024, 7, 30), LocalDate.of(1844, 4, 11));
    assertEquals(
        List.of(NepaliDate.parse("2081-04-15"), NepaliDate.MIN),
        NepaliDate.toNepaliDates(gregorian));
    assertEquals(
        gregorian,
        NepaliDate.toEnglishDates(List.of(NepaliDate.parse("2081-04-15"), NepaliDate.MIN)));
    assertEquals(gregorian, NepaliDate.parseToEnglishDates(List.of("2081-04-15", "1901-01-01")));
    assertEquals(List.of(), NepaliDate.toNepaliDates(List.of()));
    assertEquals(List.of(), NepaliDate.toEnglishDates(List.of()));
    assertEquals(List.of(), NepaliDate.parseToEnglishDates(List.of()));
    Iterable<LocalDate> notACollection = () -> gregorian.iterator();
    assertEquals(2, NepaliDate.toNepaliDates(notACollection).size());
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE,
        () -> NepaliDate.toNepaliDates(List.of(LocalDate.of(1800, 1, 1))));
  }

  @Test
  void theOptionalForms() {
    assertEquals(Optional.of(NepaliDate.parse("2081-04-15")), NepaliDate.tryParse("2081/04/15"));
    assertEquals(Optional.empty(), NepaliDate.tryParse("nope"));
    assertEquals(
        Optional.of(NepaliDate.parse("2081-04-15")), NepaliDate.tryParseLenient("15 Shrawan 2081"));
    assertEquals(Optional.empty(), NepaliDate.tryParseLenient("nope"));
  }

  @Test
  void theEnumGuards() {
    Vectors.assertThrows(NepDateError.INVALID_MONTH, () -> Month.of(0));
    Vectors.assertThrows(NepDateError.INVALID_MONTH, () -> Month.of(13));
    Vectors.assertThrows(NepDateError.INVALID_MONTH, () -> Quarter.ofMonth(0));
    assertThrows(IllegalArgumentException.class, () -> Weekday.ofIndex(-1));
    assertThrows(IllegalArgumentException.class, () -> Weekday.ofIndex(7));
    assertEquals(Weekday.SUNDAY, Weekday.ofIndex(0));
    assertEquals(6, Weekday.SATURDAY.index());
    assertEquals('\\', Separator.BACKSLASH.character());
    assertEquals('/', Separator.SLASH.character());
    assertEquals(6, DateOrder.values().length);
    assertEquals(6, Separator.values().length);
    assertEquals(12, Month.values().length);
    assertEquals(7, Weekday.values().length);
    assertEquals(2, MonthOverflow.values().length);
    assertEquals(2, Language.values().length);
    assertEquals(4, Quarter.values().length);
    assertEquals(9, NepDateError.values().length);
  }

  @Test
  void theErrorMessageIsForPeople() {
    NepDateException error =
        assertThrows(NepDateException.class, () -> NepaliDate.parse("2080/13/01"));
    assertEquals(NepDateError.INVALID_MONTH, error.error());
    assertTrue(error.getMessage().contains("13"));
    NepDateException parse = assertThrows(NepDateException.class, () -> NepaliDate.parse("hello"));
    assertEquals(NepDateError.INVALID_CHARACTER, parse.error());
    assertTrue(parse.getMessage().contains("hello"));
    assertEquals(0, parse.monthLength());
  }
}
