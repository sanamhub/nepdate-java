package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/range-days.tsv}: the interval, working-day and weekend helpers. */
class RangeDaysVectorTest {
  private static final Vectors.Table DAYS =
      Vectors.table("spec/vectors/csharp-golden/range-days.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(DAYS.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("start", "end", "operation", "argument", "result", "note"), DAYS.header());
    assertEquals(18, DAYS.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    NepaliDateRange range =
        NepaliDateRange.of(
            NepaliDate.parse(DAYS.value(row, "start")), NepaliDate.parse(DAYS.value(row, "end")));
    String operation = DAYS.value(row, "operation");
    String argument = DAYS.value(row, "argument");
    String result = DAYS.value(row, "result");

    List<NepaliDate> dates =
        switch (operation) {
          case "dates_with_interval" -> {
            int interval = Integer.parseInt(argument);
            if (result.equals("ERR")) {
              Vectors.assertThrows(
                  NepDateError.OUT_OF_RANGE, () -> range.datesWithInterval(interval));
              yield List.of();
            }
            yield range.datesWithInterval(interval).toList();
          }
          case "working_days" -> range.workingDays(Boolean.parseBoolean(argument)).toList();
          case "weekend_days" -> range.weekendDays(Boolean.parseBoolean(argument)).toList();
          case "enumerate" -> range.stream().toList();
          default -> throw new IllegalArgumentException("unknown operation " + operation);
        };
    if (!result.equals("ERR")) {
      assertEquals(result, Vectors.describe(dates));
    }
  }

  @Test
  void theIntervalIsValidated() {
    NepaliDateRange range = Vectors.range("2081-04-10..2081-04-20");
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> range.datesWithInterval(0));
    Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> range.datesWithInterval(-1));
  }

  @Test
  void aHugeIntervalDoesNotOverflow() {
    NepaliDateRange range = Vectors.range("2081-04-10..2081-04-20");
    assertEquals(
        List.of(NepaliDate.parse("2081-04-10")),
        range.datesWithInterval(Integer.MAX_VALUE).toList());
  }
}
