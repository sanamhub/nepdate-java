package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/bulk.tsv}: ordered batch conversion in both directions. */
class BulkVectorTest {
  private static final Vectors.Table BULK = Vectors.table("spec/vectors/csharp-golden/bulk.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(BULK.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("operation", "input", "output", "note"), BULK.header());
    assertEquals(6, BULK.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    String operation = BULK.value(row, "operation");
    String[] inputs = BULK.value(row, "input").split(";");
    String output = BULK.value(row, "output");

    Runnable call =
        switch (operation) {
          case "to_nepali_dates", "batch_to_nepali_dates" ->
              () -> {
                List<LocalDate> dates = new ArrayList<>();
                for (String input : inputs) {
                  dates.add(LocalDate.parse(input));
                }
                assertEquals(output, Vectors.describe(NepaliDate.toNepaliDates(dates)));
              };
          case "to_english_dates_from_strings" ->
              () -> {
                List<String> texts = List.of(inputs);
                if (output.equals("ERR")) {
                  Vectors.assertThrows(
                      NepDateError.INVALID_DAY, () -> NepaliDate.parseToEnglishDates(texts));
                } else {
                  StringBuilder actual = new StringBuilder();
                  for (LocalDate date : NepaliDate.parseToEnglishDates(texts)) {
                    if (!actual.isEmpty()) {
                      actual.append(';');
                    }
                    actual.append(date);
                  }
                  assertEquals(output, actual.toString());
                }
              };
          case "to_english_dates_from_dates", "batch_to_english_dates" ->
              () -> {
                List<NepaliDate> dates = new ArrayList<>();
                for (String input : inputs) {
                  dates.add(NepaliDate.parse(input));
                }
                StringBuilder actual = new StringBuilder();
                for (LocalDate date : NepaliDate.toEnglishDates(dates)) {
                  if (!actual.isEmpty()) {
                    actual.append(';');
                  }
                  actual.append(date);
                }
                assertEquals(output, actual.toString());
              };
          default -> throw new IllegalArgumentException("unknown operation " + operation);
        };
    call.run();
  }

  @Test
  void oneBadInputFailsTheWholeCall() {
    List<String> texts = List.of("2081-04-15", "2081-01-32");
    try {
      NepaliDate.parseToEnglishDates(texts);
      throw new AssertionError("the call should have failed");
    } catch (NepDateException expected) {
      assertEquals(NepDateError.INVALID_DAY, expected.error());
    }
  }
}
