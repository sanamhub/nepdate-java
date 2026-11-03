package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code parse.tsv}: the strict and the lenient parser, with every error kind. */
class ParseVectorTest {
  private static final Vectors.Table PARSE = Vectors.table("spec/vectors/parse.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(PARSE.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("input", "strict", "lenient"), PARSE.header());
    assertEquals(69, PARSE.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void theStrictColumnMatches(String[] row) {
    String input = PARSE.value(row, "input");
    String expected = PARSE.value(row, "strict");
    Optional<NepaliDate> parsed = NepaliDate.tryParse(input);
    if (expected.startsWith("ERR:")) {
      NepDateError error = Vectors.error(expected.substring(4));
      Vectors.assertThrows(error, () -> NepaliDate.parse(input));
      assertFalse(parsed.isPresent(), "tryParse should be empty for " + input);
    } else {
      NepaliDate date = NepaliDate.parse(input);
      assertEquals(expected, date.toIsoString(), "strict parse of \"" + input + "\"");
      assertEquals(Optional.of(date), parsed);
    }
  }

  @ParameterizedTest
  @MethodSource("rows")
  void theLenientColumnMatches(String[] row) {
    String input = PARSE.value(row, "input");
    String expected = PARSE.value(row, "lenient");
    Optional<NepaliDate> parsed = NepaliDate.tryParseLenient(input);
    if (expected.startsWith("ERR:")) {
      NepDateError error = Vectors.error(expected.substring(4));
      Vectors.assertThrows(error, () -> NepaliDate.parseLenient(input));
      assertFalse(parsed.isPresent(), "tryParseLenient should be empty for " + input);
    } else {
      NepaliDate date = NepaliDate.parseLenient(input);
      assertEquals(expected, date.toIsoString(), "lenient parse of \"" + input + "\"");
      assertEquals(Optional.of(date), parsed);
    }
  }

  @Test
  void theTwoParsersDifferOnlyWhereTheVectorsSaySo() {
    long strictOnly = 0;
    long lenientOnly = 0;
    for (String[] row : PARSE.rows()) {
      String input = PARSE.value(row, "input");
      boolean strict = NepaliDate.tryParse(input).isPresent();
      boolean lenient = NepaliDate.tryParseLenient(input).isPresent();
      if (strict && !lenient) {
        strictOnly++;
      }
      if (!strict && lenient) {
        lenientOnly++;
      }
    }
    assertEquals(0, strictOnly, "lenient accepts everything strict accepts");
    assertTrue(lenientOnly > 0, "the vectors contain lenient-only forms");
  }
}
