package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/range-format.tsv}: both string forms of a range. */
class RangeFormatVectorTest {
  private static final Vectors.Table FORMATS =
      Vectors.table("spec/vectors/csharp-golden/range-format.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(FORMATS.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(
        List.of("start", "end", "kind", "order", "separator", "lang", "result", "note"),
        FORMATS.header());
    assertEquals(18, FORMATS.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    String start = FORMATS.value(row, "start");
    String end = FORMATS.value(row, "end");
    String result = FORMATS.value(row, "result");
    if (result.equals("ERR")) {
      Vectors.assertThrows(
          NepDateError.OUT_OF_RANGE,
          () -> NepaliDateRange.of(NepaliDate.parse(start), NepaliDate.parse(end)));
      return;
    }
    NepaliDateRange range = Vectors.range(start + ".." + end);
    if (FORMATS.value(row, "kind").equals("default")) {
      assertEquals(result, range.toString());
    } else {
      assertEquals(
          result,
          range.toShortString(
              Vectors.order(FORMATS.value(row, "order")),
              Vectors.separator(FORMATS.value(row, "separator")),
              Vectors.language(FORMATS.value(row, "lang"))));
    }
  }
}
