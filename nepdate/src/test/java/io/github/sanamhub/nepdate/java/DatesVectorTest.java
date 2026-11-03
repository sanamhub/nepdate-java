package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/dates.tsv}: every column of the C# date record. */
class DatesVectorTest {
  private static final Vectors.Table DATES = Vectors.table("spec/vectors/csharp-golden/dates.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(DATES.rows());
  }

  @Test
  void theFileHasTheExpectedColumns() {
    assertEquals(
        List.of(
            "bs",
            "ad",
            "weekday",
            "day_of_year",
            "month_length",
            "to_string",
            "unicode",
            "long_en",
            "long_en_weekday",
            "long_ne",
            "long_ne_weekday",
            "dmy_dash_nopad",
            "fy_start",
            "fy_end",
            "quarter_start",
            "quarter_end"),
        DATES.header());
    assertEquals(1018, DATES.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyColumnMatches(String[] row) {
    NepaliDate date = NepaliDate.parse(DATES.value(row, "bs"));
    String bs = DATES.value(row, "bs");

    assertEquals(LocalDate.parse(DATES.value(row, "ad")), date.toLocalDate(), "AD of " + bs);
    assertEquals(DATES.value(row, "weekday"), date.weekday().english(), "weekday of " + bs);
    assertEquals(
        Integer.parseInt(DATES.value(row, "day_of_year")),
        date.dayOfYear(),
        "day of year of " + bs);
    assertEquals(
        Integer.parseInt(DATES.value(row, "month_length")),
        date.monthLength(),
        "month length of " + bs);
    assertEquals(DATES.value(row, "to_string"), date.toString(), "default of " + bs);
    assertEquals(
        DATES.value(row, "unicode"),
        date.toShortString(DateOrder.YMD, Separator.SLASH, true, Language.NEPALI),
        "Nepali short of " + bs);
    assertEquals(DATES.value(row, "long_en"), date.toLongString(), "English long of " + bs);
    assertEquals(
        DATES.value(row, "long_en_weekday"),
        date.toLongString(LongDateOptions.DEFAULT.withWeekday(true)),
        "English long with weekday of " + bs);
    assertEquals(
        DATES.value(row, "long_ne"),
        date.toLongString(LongDateOptions.DEFAULT, Language.NEPALI),
        "Nepali long of " + bs);
    assertEquals(
        DATES.value(row, "long_ne_weekday"),
        date.toLongString(LongDateOptions.DEFAULT.withWeekday(true), Language.NEPALI),
        "Nepali long with weekday of " + bs);
    assertEquals(
        DATES.value(row, "dmy_dash_nopad"),
        date.toShortString(DateOrder.DMY, Separator.DASH, false),
        "short DMY without padding of " + bs);
  }

  @ParameterizedTest
  @MethodSource("rows")
  void theFiscalYearColumnsMatch(String[] row) {
    NepaliDate date = NepaliDate.parse(DATES.value(row, "bs"));
    String bs = DATES.value(row, "bs");

    String start = DATES.value(row, "fy_start");
    if ("ERR".equals(start)) {
      Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> date.fiscalYear().start());
    } else {
      assertEquals(NepaliDate.parse(start), date.fiscalYear().start(), "fiscal start of " + bs);
    }

    String end = DATES.value(row, "fy_end");
    if ("ERR".equals(end)) {
      Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> date.fiscalYear().end());
    } else {
      assertEquals(NepaliDate.parse(end), date.fiscalYear().end(), "fiscal end of " + bs);
    }

    String quarterStart = DATES.value(row, "quarter_start");
    String quarterEnd = DATES.value(row, "quarter_end");
    if ("ERR".equals(quarterStart) || "ERR".equals(quarterEnd)) {
      Vectors.assertThrows(
          NepDateError.OUT_OF_RANGE, () -> date.fiscalYear().quarterRange(date.quarter()));
    } else {
      NepaliDateRange quarter = date.fiscalYear().quarterRange(date.quarter());
      assertEquals(NepaliDate.parse(quarterStart), quarter.start(), "quarter start of " + bs);
      assertEquals(NepaliDate.parse(quarterEnd), quarter.end(), "quarter end of " + bs);
    }
  }

  @Test
  void theBoundaryRowsAreTheSupportedSpan() {
    NepaliDate first = NepaliDate.parse("1901-01-01");
    assertEquals(LocalDate.of(1844, 4, 11), first.toLocalDate());
    assertEquals(NepaliDate.MIN, first);
    assertEquals(Weekday.THURSDAY, first.weekday());

    NepaliDate last = NepaliDate.parse("2199-12-30");
    assertEquals(LocalDate.of(2143, 4, 15), last.toLocalDate());
    assertEquals(NepaliDate.MAX, last);
    assertThrows(NepDateException.class, () -> last.plusDays(1));
  }
}
