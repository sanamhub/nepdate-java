package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code month-boundaries.csv}: the first and last day of every BS month. */
class MonthBoundariesVectorTest {
  private static final Vectors.Table BOUNDARIES = Vectors.monthBoundaries();

  static Stream<Arguments> rows() {
    return Vectors.arguments(BOUNDARIES.rows());
  }

  @Test
  void theFileCoversEveryMonthTwice() {
    assertEquals(List.of("bs", "ad", "weekday", "month_length"), BOUNDARIES.header());
    assertEquals(299 * 12 * 2, BOUNDARIES.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyBoundaryMatches(String[] row) {
    NepaliDate date = NepaliDate.parse(BOUNDARIES.value(row, "bs"));
    String bs = BOUNDARIES.value(row, "bs");
    int length = Integer.parseInt(BOUNDARIES.value(row, "month_length"));

    assertEquals(LocalDate.parse(BOUNDARIES.value(row, "ad")), date.toLocalDate(), "AD of " + bs);
    assertEquals(BOUNDARIES.value(row, "weekday"), date.weekday().english(), "weekday of " + bs);
    assertEquals(length, date.monthLength(), "month length of " + bs);
    assertTrue(date.day() == 1 || date.day() == length, "first or last day of " + bs);
    if (date.day() == 1) {
      assertEquals(date, date.firstDayOfMonth(), "first day of " + bs);
    } else {
      assertEquals(date, date.lastDayOfMonth(), "last day of " + bs);
    }
  }

  @Test
  void theRowsAreFirstThenLastForEveryMonth() {
    for (int i = 0; i < BOUNDARIES.rows().size(); i += 2) {
      NepaliDate first = NepaliDate.parse(BOUNDARIES.rows().get(i)[0]);
      NepaliDate last = NepaliDate.parse(BOUNDARIES.rows().get(i + 1)[0]);
      assertEquals(1, first.day(), "row " + i + " is the first day");
      assertEquals(first.monthLength(), last.day(), "row " + (i + 1) + " is the last day");
      assertEquals(first.year(), last.year());
      assertEquals(first.monthNumber(), last.monthNumber());
      assertEquals(first.toLocalDate().plusDays(first.monthLength() - 1L), last.toLocalDate());
    }
  }
}
