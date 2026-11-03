package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/add-days.tsv}: {@link NepaliDate#plusDays(long)} against C#. */
class AddDaysVectorTest {
  private static final Vectors.Table ADD_DAYS =
      Vectors.table("spec/vectors/csharp-golden/add-days.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(ADD_DAYS.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("bs", "days", "result"), ADD_DAYS.header());
    assertEquals(9162, ADD_DAYS.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    NepaliDate date = NepaliDate.parse(ADD_DAYS.value(row, "bs"));
    int days = Integer.parseInt(ADD_DAYS.value(row, "days"));
    NepaliDate expected = Vectors.dateOrNull(ADD_DAYS.value(row, "result"));
    if (expected == null) {
      Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> date.plusDays(days));
    } else {
      assertEquals(expected, date.plusDays(days), ADD_DAYS.value(row, "bs") + " plus " + days);
      assertEquals(date, expected.minusDays(days));
    }
  }
}
