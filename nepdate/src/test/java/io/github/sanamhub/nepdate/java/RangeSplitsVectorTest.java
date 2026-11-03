package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/range-splits.tsv}: the month and fiscal-quarter splits. */
class RangeSplitsVectorTest {
  private static final Vectors.Table SPLITS =
      Vectors.table("spec/vectors/csharp-golden/range-splits.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(SPLITS.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("start", "end", "operation", "result", "note"), SPLITS.header());
    assertEquals(9, SPLITS.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    NepaliDateRange range =
        NepaliDateRange.of(
            NepaliDate.parse(SPLITS.value(row, "start")),
            NepaliDate.parse(SPLITS.value(row, "end")));
    List<NepaliDateRange> pieces =
        switch (SPLITS.value(row, "operation")) {
          case "split_by_month" -> range.splitByMonth();
          case "split_by_fiscal_quarter" -> range.splitByFiscalQuarter();
          default -> throw new IllegalArgumentException(SPLITS.value(row, "operation"));
        };
    assertEquals(SPLITS.value(row, "result"), Vectors.describeRanges(pieces));
    assertPiecesCover(pieces, range);
  }

  private static void assertPiecesCover(List<NepaliDateRange> pieces, NepaliDateRange range) {
    assertEquals(range.start(), pieces.get(0).start(), "the first piece starts at the range");
    assertEquals(
        range.end(), pieces.get(pieces.size() - 1).end(), "the last piece ends at the range");
    for (int i = 1; i < pieces.size(); i++) {
      assertEquals(
          pieces.get(i - 1).end().plusDays(1),
          pieces.get(i).start(),
          "pieces are contiguous at " + i);
    }
    long covered = 0;
    for (NepaliDateRange piece : pieces) {
      covered += piece.length();
    }
    assertEquals(range.length(), covered, "the pieces cover the range exactly");
  }
}
