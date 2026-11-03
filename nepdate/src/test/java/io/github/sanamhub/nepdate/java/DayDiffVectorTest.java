package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/day-diff.tsv}: both directions of the day difference. */
class DayDiffVectorTest {
  private static final Vectors.Table DAY_DIFF =
      Vectors.table("spec/vectors/csharp-golden/day-diff.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(DAY_DIFF.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("a", "b", "days_until", "operator_minus", "note"), DAY_DIFF.header());
    assertEquals(16, DAY_DIFF.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void bothDirectionsMatch(String[] row) {
    NepaliDate a = NepaliDate.parse(DAY_DIFF.value(row, "a"));
    NepaliDate b = NepaliDate.parse(DAY_DIFF.value(row, "b"));
    long until = Long.parseLong(DAY_DIFF.value(row, "days_until"));
    long operatorMinus = Long.parseLong(DAY_DIFF.value(row, "operator_minus"));

    assertEquals(until, a.daysUntil(b), "daysUntil for " + a + " and " + b);
    assertEquals(operatorMinus, a.daysSince(b), "daysSince for " + a + " and " + b);
    assertEquals(-until, b.daysUntil(a), "daysUntil is antisymmetric");
    assertEquals(until, b.daysSince(a), "daysSince is antisymmetric");
    assertEquals(until, a.toLocalDate().until(b.toLocalDate(), java.time.temporal.ChronoUnit.DAYS));
  }

  @Test
  void theCalendarBreakdownMatchesAlgorithmSection8a() {
    assertPeriod(NepaliDate.parse("2080-01-01"), NepaliDate.parse("2081-04-15"), 1, 3, 14);
    assertPeriod(NepaliDate.parse("2081-04-15"), NepaliDate.parse("2080-01-01"), -1, -3, -14);
    assertPeriod(NepaliDate.parse("2081-04-32"), NepaliDate.parse("2081-05-31"), 0, 1, 0);
    assertPeriod(NepaliDate.parse("2081-04-32"), NepaliDate.parse("2081-06-01"), 0, 1, 1);
    assertPeriod(NepaliDate.parse("2081-01-31"), NepaliDate.parse("2081-02-30"), 0, 0, 30);
    assertPeriod(NepaliDate.parse("2081-03-15"), NepaliDate.parse("2081-03-15"), 0, 0, 0);
  }

  private static void assertPeriod(NepaliDate a, NepaliDate b, int years, int months, int days) {
    java.time.Period period = a.until(b);
    assertEquals(years, period.getYears(), "years of " + a + " to " + b);
    assertEquals(months, period.getMonths(), "months of " + a + " to " + b);
    assertEquals(days, period.getDays(), "days of " + a + " to " + b);
  }
}
