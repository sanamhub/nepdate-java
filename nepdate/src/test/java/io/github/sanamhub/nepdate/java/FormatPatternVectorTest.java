package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/format-pattern.tsv}: the standard and custom pattern language. */
class FormatPatternVectorTest {
  private static final Vectors.Table PATTERNS =
      Vectors.table("spec/vectors/csharp-golden/format-pattern.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(PATTERNS.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("bs", "pattern", "result"), PATTERNS.header());
    assertEquals(452, PATTERNS.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    NepaliDate date = NepaliDate.parse(PATTERNS.value(row, "bs"));
    String pattern = PATTERNS.value(row, "pattern");
    String expected = PATTERNS.value(row, "result");
    // D-04: the C# short month is "Ash" for both Ashad and Ashoj; every port writes Asa and Aso.
    if ((date.month() == Month.ASHAD || date.month() == Month.ASHOJ)
        && hasShortMonthToken(pattern)
        && expected.contains("Ash")) {
      expected = expected.replace("Ash", date.month() == Month.ASHAD ? "Asa" : "Aso");
    }
    assertEquals(expected, date.format(pattern), date + " with " + pattern);
  }

  /** Whether the pattern has an unescaped, unquoted run of exactly three {@code M} tokens. */
  private static boolean hasShortMonthToken(String pattern) {
    int index = 0;
    while (index < pattern.length()) {
      char c = pattern.charAt(index);
      if (c == '\\') {
        index += 2;
      } else if (c == '\'') {
        int close = pattern.indexOf('\'', index + 1);
        index = close < 0 ? pattern.length() : close + 1;
      } else if (c == 'M') {
        int run = index;
        while (run < pattern.length() && pattern.charAt(run) == 'M') {
          run++;
        }
        if (run - index == 3) {
          return true;
        }
        index = run;
      } else {
        index++;
      }
    }
    return false;
  }

  @Test
  void theDocumentedExamplesHold() {
    NepaliDate date = NepaliDate.parse("2081-04-15");
    assertEquals("2081-04-15", date.format("yyyy-MM-dd"));
    assertEquals("4/15/2081", date.format("M/d/yyyy"));
    assertEquals("2081BS", date.format("yyyy'BS'"));
    assertEquals("2081MM-dd", date.format("yyyy\\M\\M-\\d\\d"));
    assertEquals("Shr 2081", date.format("MMM yyyy"));
    assertEquals("15.04.81", date.format("dd.MM.yy"));
    assertEquals("\\", date.format("\\"));
    assertEquals(date.toString(), date.format(""));
    assertEquals(date.toString(), date.format("G"));
    assertEquals(date.toString(), date.format("g"));
    assertEquals(date.toString(), date.format("d"));
    assertEquals(date.toLongString(), date.format("D"));
    assertEquals(date.toIsoString(), date.format("s"));
    assertEquals("2081/04/15", date.format("yyyy/MM/dd"));
    assertEquals("साउन १५, २०८१", date.format("MMMM d, yyyy", Language.NEPALI));
    assertEquals("Asa", NepaliDate.parse("2081-03-01").format("MMM", Language.ENGLISH));
    assertEquals("Aso", NepaliDate.parse("2081-06-01").format("MMM"));
  }
}
