package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/range-factories.tsv}: the named factories. */
class RangeFactoriesVectorTest {
  private static final Vectors.Table FACTORIES =
      Vectors.table("spec/vectors/csharp-golden/range-factories.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(FACTORIES.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("factory", "argument", "result", "note"), FACTORIES.header());
    assertEquals(27, FACTORIES.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    String factory = FACTORIES.value(row, "factory");
    String argument = FACTORIES.value(row, "argument");
    String result = FACTORIES.value(row, "result");

    if (result.equals("ERR")) {
      // The vector says ERR; the kind follows the validation order, for example a bad month.
      org.junit.jupiter.api.Assertions.assertThrows(
          NepDateException.class, () -> build(factory, argument));
    } else {
      assertEquals(result, Vectors.describe(build(factory, argument)));
    }
  }

  private static NepaliDateRange build(String factory, String argument) {
    return switch (factory) {
      case "single_day" -> NepaliDateRange.singleDay(NepaliDate.parse(argument));
      case "from_day_count" -> {
        String[] parts = argument.split(",");
        yield NepaliDateRange.fromDayCount(NepaliDate.parse(parts[0]), Integer.parseInt(parts[1]));
      }
      case "for_month" -> {
        String[] parts = argument.split(",");
        yield NepaliDateRange.forMonth(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      }
      case "for_calendar_year" -> NepaliDateRange.forYear(Integer.parseInt(argument));
      case "for_fiscal_year" -> NepaliDateRange.forFiscalYear(Integer.parseInt(argument));
      case "construct" -> {
        String[] parts = argument.split(",");
        yield NepaliDateRange.of(NepaliDate.parse(parts[0]), NepaliDate.parse(parts[1]));
      }
      default -> throw new IllegalArgumentException("unknown factory " + factory);
    };
  }
}
