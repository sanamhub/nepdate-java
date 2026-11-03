package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/ranges.tsv}: construction, day queries and the set operations. */
class RangesVectorTest {
  private static final Vectors.Table RANGES =
      Vectors.table("spec/vectors/csharp-golden/ranges.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(RANGES.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(
        List.of("start", "end", "operation", "argument", "result", "note"), RANGES.header());
    assertEquals(54, RANGES.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    String start = RANGES.value(row, "start");
    String end = RANGES.value(row, "end");
    String operation = RANGES.value(row, "operation");
    String argument = RANGES.value(row, "argument");
    String result = RANGES.value(row, "result");

    if (operation.equals("construct")) {
      if (result.equals("ERR")) {
        Vectors.assertThrows(
            NepDateError.OUT_OF_RANGE,
            () -> NepaliDateRange.of(NepaliDate.parse(start), NepaliDate.parse(end)));
      } else {
        assertEquals(result, Vectors.describe(range(start, end)));
      }
      return;
    }

    NepaliDateRange range = range(start, end);
    switch (operation) {
      case "length" -> assertEquals(Integer.parseInt(result), range.length());
      case "contains_date" ->
          assertEquals(Boolean.parseBoolean(result), range.contains(NepaliDate.parse(argument)));
      case "contains_range" ->
          assertEquals(Boolean.parseBoolean(result), range.contains(Vectors.range(argument)));
      case "overlaps" ->
          assertEquals(Boolean.parseBoolean(result), range.overlaps(Vectors.range(argument)));
      case "is_adjacent_to" ->
          assertEquals(Boolean.parseBoolean(result), range.isAdjacentTo(Vectors.range(argument)));
      case "intersect" -> {
        Optional<NepaliDateRange> found = range.intersect(Vectors.range(argument));
        if (result.equals("none")) {
          assertEquals(Optional.empty(), found);
        } else {
          assertEquals(
              Vectors.describe(Vectors.range(result)), Vectors.describe(found.orElseThrow()));
        }
      }
      case "union" ->
          assertEquals(
              Vectors.describe(Vectors.range(result)),
              Vectors.describe(range.union(Vectors.range(argument))));
      case "except" -> {
        List<NepaliDateRange> pieces = range.except(Vectors.range(argument));
        assertEquals(result, Vectors.describeRanges(pieces));
        int covered = 0;
        for (NepaliDateRange piece : pieces) {
          covered += piece.length();
          assertEquals(true, range.contains(piece));
        }
        if (!pieces.isEmpty()) {
          assertEquals(true, covered <= range.length());
        }
      }
      case "equals" ->
          assertEquals(Boolean.parseBoolean(result), range.equals(Vectors.range(argument)));
      case "to_string" -> assertEquals(result, range.toString());
      case "enumerate" -> assertEquals(result, Vectors.describe(range.stream().toList()));
      default -> throw new IllegalArgumentException("unknown operation " + operation);
    }
  }

  private static NepaliDateRange range(String start, String end) {
    return NepaliDateRange.of(NepaliDate.parse(start), NepaliDate.parse(end));
  }

  @Test
  void theEmptyIntersectionIsOptionalEmpty() {
    NepaliDateRange first = Vectors.range("2081-04-10..2081-04-20");
    NepaliDateRange second = Vectors.range("2081-05-01..2081-05-05");
    assertEquals(Optional.empty(), first.intersect(second));
    assertEquals(Optional.empty(), second.intersect(first));
  }

  @Test
  void anEmptyRangeCannotBeBuilt() {
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE,
        () -> NepaliDateRange.of(NepaliDate.parse("2081-04-20"), NepaliDate.parse("2081-04-10")));
  }
}
