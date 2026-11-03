package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/format-matrix.tsv}: every short order and separator, and TryFormat. */
class FormatMatrixVectorTest {
  private static final Vectors.Table MATRIX =
      Vectors.table("spec/vectors/csharp-golden/format-matrix.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(MATRIX.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(
        List.of(
            "kind",
            "bs",
            "order",
            "separator",
            "pad",
            "lang",
            "format",
            "capacity",
            "result",
            "note"),
        MATRIX.header());
    assertEquals(102, MATRIX.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    NepaliDate date = NepaliDate.parse(MATRIX.value(row, "bs"));
    Language language = Vectors.language(MATRIX.value(row, "lang"));
    String result = MATRIX.value(row, "result");
    if (MATRIX.value(row, "kind").equals("short")) {
      DateOrder order = Vectors.order(MATRIX.value(row, "order"));
      Separator separator = Vectors.separator(MATRIX.value(row, "separator"));
      boolean pad = Boolean.parseBoolean(MATRIX.value(row, "pad"));
      assertEquals(result, date.toShortString(order, separator, pad, language));
    } else {
      int capacity = Integer.parseInt(MATRIX.value(row, "capacity"));
      String format = MATRIX.value(row, "format");
      String pattern = "default".equals(format) ? "" : format;
      char[] buffer = new char[capacity];
      java.util.Arrays.fill(buffer, '\u0000');
      boolean fits = date.tryFormat(buffer, pattern, language);
      if (result.equals("ERR")) {
        assertFalse(fits, "capacity " + capacity + " should be too small for " + pattern);
        for (char c : buffer) {
          assertEquals('\u0000', c, "a rejected format must leave the buffer unchanged");
        }
      } else {
        assertTrue(fits, "capacity " + capacity + " should fit " + pattern);
        assertEquals(result, new String(buffer, 0, result.length()));
      }
    }
  }
}
