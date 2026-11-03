package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code year-length.tsv}: the length of every BS year, the reference vector for the leap rule. */
class YearLengthVectorTest {
  private static final Vectors.Table YEAR_LENGTH = Vectors.table("spec/vectors/year-length.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(YEAR_LENGTH.rows());
  }

  @Test
  void theFileCoversEveryYear() {
    assertEquals(List.of("bs_year", "year_length"), YEAR_LENGTH.header());
    assertEquals(299, YEAR_LENGTH.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyYearLengthMatches(String[] row) {
    int year = Integer.parseInt(YEAR_LENGTH.value(row, "bs_year"));
    int length = Integer.parseInt(YEAR_LENGTH.value(row, "year_length"));
    NepaliDate first = NepaliDate.of(year, 1, 1);
    assertEquals(length, first.yearLength(), "year length of " + year);

    NepaliDate last = first.plusDays(length - 1L);
    assertEquals(year, last.year(), "last day of " + year);
    assertEquals(12, last.monthNumber(), "last month of " + year);
    assertEquals(last.monthLength(), last.day(), "last day of the last month of " + year);

    if (year < CalendarTable.MAX_YEAR) {
      assertEquals(
          NepaliDate.of(year + 1, 1, 1),
          first.plusDays(length),
          "the next year starts " + length + " days later");
    } else {
      Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> first.plusDays(length));
    }
  }
}
