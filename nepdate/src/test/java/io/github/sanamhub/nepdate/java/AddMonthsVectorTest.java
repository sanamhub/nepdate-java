package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/add-months.tsv}: both overflow modes of month arithmetic. */
class AddMonthsVectorTest {
  private static final Vectors.Table ADD_MONTHS =
      Vectors.table("spec/vectors/csharp-golden/add-months.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(ADD_MONTHS.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("bs", "months", "clamp", "overflow"), ADD_MONTHS.header());
    assertEquals(10180, ADD_MONTHS.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatchesBothModes(String[] row) {
    NepaliDate date = NepaliDate.parse(ADD_MONTHS.value(row, "bs"));
    int months = Integer.parseInt(ADD_MONTHS.value(row, "months"));
    String label = ADD_MONTHS.value(row, "bs") + " plus " + months + " months";

    NepaliDate clamped = Vectors.dateOrNull(ADD_MONTHS.value(row, "clamp"));
    if (clamped == null) {
      Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> date.plusMonths(months));
    } else {
      assertEquals(clamped, date.plusMonths(months), label + ", clamp");
      assertEquals(
          clamped, date.plusMonths(months, MonthOverflow.CLAMP), label + ", explicit clamp");
    }

    NepaliDate spilled = Vectors.dateOrNull(ADD_MONTHS.value(row, "overflow"));
    if (spilled == null) {
      Vectors.assertThrows(
          NepDateError.OUT_OF_RANGE, () -> date.plusMonths(months, MonthOverflow.SPILL));
    } else {
      assertEquals(spilled, date.plusMonths(months, MonthOverflow.SPILL), label + ", spill");
    }
  }
}
