package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/ymd-int.tsv}: the packed {@code YYYYMMDD} integer both ways. */
class YmdIntVectorTest {
  private static final Vectors.Table YMD = Vectors.table("spec/vectors/csharp-golden/ymd-int.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(YMD.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("kind", "input", "ymd", "result", "note"), YMD.header());
    assertEquals(14, YMD.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) {
    String kind = YMD.value(row, "kind");
    int packed = Integer.parseInt(YMD.value(row, "ymd"));
    String result = YMD.value(row, "result");

    if (kind.equals("date")) {
      NepaliDate date = NepaliDate.parse(YMD.value(row, "input"));
      assertEquals(packed, date.toYmd(), "packed form of " + date);
      assertEquals(date, NepaliDate.ofYmd(packed), "reading " + packed);
      assertEquals(NepaliDate.parse(result), NepaliDate.ofYmd(packed));
    } else {
      assertEquals("-", YMD.value(row, "input"), "an int row has no date input");
      if (result.equals("ERR")) {
        // The vector says ERR; which kind follows the validation order year, month, day.
        assertThrows(NepDateException.class, () -> NepaliDate.ofYmd(packed));
        assertNull(tryOfYmd(packed), "no value for " + packed);
      } else {
        assertEquals(NepaliDate.parse(result), NepaliDate.ofYmd(packed), "reading " + packed);
      }
    }
  }

  private static NepaliDate tryOfYmd(int packed) {
    try {
      return NepaliDate.ofYmd(packed);
    } catch (NepDateException expected) {
      return null;
    }
  }

  @Test
  void noIntegerWrapsIntoTheRange() {
    for (int value : new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE, -1, 0, 2081}) {
      Vectors.assertThrows(NepDateError.OUT_OF_RANGE, () -> NepaliDate.ofYmd(value));
    }
    // The year and month are in range and the day is not, so the day is the first failure.
    Vectors.assertThrows(NepDateError.INVALID_DAY, () -> NepaliDate.ofYmd(20810400));
    Vectors.assertThrows(NepDateError.INVALID_MONTH, () -> NepaliDate.ofYmd(20810015));
    assertEquals(NepaliDate.parse("2081-04-32"), NepaliDate.ofYmd(20810432));
  }

  @Test
  void aNegativeIntegerIsNotSilentlyTruncated() {
    assertNull(tryOfYmd(-1));
    assertNull(tryOfYmd(-20810415));
  }
}
