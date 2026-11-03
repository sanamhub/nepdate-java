package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The arithmetic at the ends of the supported span and at the widest arguments. */
class ArithmeticEdgeTest {
  @Test
  void stepsPastTheEndAreRefusedNotWrapped() {
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.MAX.plusDays(1));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.minusDays(1));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.plusDays(Long.MAX_VALUE));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.plusDays(Long.MIN_VALUE));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.MAX.plusDays(-Long.MAX_VALUE));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.minusDays(Long.MIN_VALUE));
    assertEquals(NepaliDate.MAX, NepaliDate.MIN.plusDays(CalendarTable.MAX_SERIAL));
    assertEquals(NepaliDate.MIN, NepaliDate.MAX.minusDays(CalendarTable.MAX_SERIAL));
  }

  @Test
  void monthArithmeticAtTheEnds() {
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.MAX.plusMonths(1));
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDate.MAX.plusMonths(1, MonthOverflow.SPILL));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.plusMonths(-1));
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.plusMonths(Long.MAX_VALUE));
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDate.MAX.plusYears(Integer.MAX_VALUE));
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.plusYears(Integer.MIN_VALUE));
    assertEquals(NepaliDate.parse("2199-12-30"), NepaliDate.parse("2199-11-30").plusMonths(1));
    assertEquals(
        NepaliDate.parse("2199-12-30"),
        NepaliDate.parse("2199-11-30").plusMonths(1, MonthOverflow.SPILL));
  }

  @Test
  void rangesAtTheEndsKeepEveryDay() {
    NepaliDateRange lastDay = NepaliDateRange.singleDay(NepaliDate.MAX);
    assertEquals(1, lastDay.length());
    assertEquals(1, lastDay.stream().count());
    assertEquals(1, lastDay.workingDays(false).count());
    assertEquals(0, lastDay.weekendDays(true).count());
    assertEquals(1, lastDay.datesWithInterval(Integer.MAX_VALUE).count());
    assertEquals(1, lastDay.splitByMonth().size());
    assertEquals(1, lastDay.splitByFiscalQuarter().size());
    assertEquals(NepaliDate.MAX, lastDay.splitByMonth().get(0).end());
    assertEquals(NepaliDate.MAX, lastDay.splitByFiscalQuarter().get(0).end());
    assertEquals(false, lastDay.isAdjacentTo(NepaliDateRange.singleDay(NepaliDate.MIN)));
  }

  @Test
  void fromDayCountAtTheWidestArguments() {
    NepaliDateRange all = NepaliDateRange.fromDayCount(NepaliDate.MIN, CalendarTable.TOTAL_DAYS);
    assertEquals(CalendarTable.TOTAL_DAYS, all.length());
    assertEquals(NepaliDate.MAX, all.end());
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE,
        () -> NepaliDateRange.fromDayCount(NepaliDate.MIN, Integer.MAX_VALUE));
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDateRange.fromDayCount(NepaliDate.MAX, 2));
    assertEquals(1, NepaliDateRange.fromDayCount(NepaliDate.MAX, 1).length());
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDateRange.fromDayCount(NepaliDate.MIN, 0));
  }

  @Test
  void aHugeNegativeStepDoesNotWrapTheEnumerator() {
    NepaliDateRange range = Vectors.range("2081-04-10..2081-04-20");
    assertEquals(1, range.datesWithInterval(Integer.MAX_VALUE).count());
    assertEquals(
        NepaliDate.parse("2081-04-10"),
        range.datesWithInterval(Integer.MAX_VALUE).findFirst().orElseThrow());
  }

  @Test
  void plusMonthsDoesNotOverflowTheMonthIndex() {
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.plusMonths(CalendarTable.MONTHS));
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> NepaliDate.MIN.plusMonths(-CalendarTable.MONTHS));
    assertEquals(NepaliDate.MIN, NepaliDate.MIN.plusMonths(0, MonthOverflow.SPILL));
  }
}
