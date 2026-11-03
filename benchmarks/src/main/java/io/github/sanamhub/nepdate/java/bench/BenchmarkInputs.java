package io.github.sanamhub.nepdate.java.bench;

import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.patro.Patro;
import java.time.LocalDate;

/**
 * The 64 dates of {@code spec/BENCHMARK.md} section 2, shared by the Java and Kotlin benchmarks so
 * both measure the same work on the same inputs in the same JVM.
 */
final class BenchmarkInputs {
  static final int COUNT = 64;

  static final int[] BS_YEAR = new int[COUNT];
  static final int[] BS_MONTH = new int[COUNT];
  static final int[] BS_DAY = new int[COUNT];
  static final int[] AD_YEAR = new int[COUNT];
  static final int[] AD_MONTH = new int[COUNT];
  static final int[] AD_DAY = new int[COUNT];
  static final int[] PATRO_SERIAL = new int[COUNT];
  static final String[] TEXT = new String[COUNT];

  /** The same patro dates as the Kotlin port's own type, so its benchmark does no conversion. */
  static final io.github.sanamhub.nepdate.NepaliDate[] KOTLIN_PATRO_DATES =
      new io.github.sanamhub.nepdate.NepaliDate[COUNT];

  static {
    // Date i is the BS date at serial s0 + i * ((s1 - s0) / 63), where s0 is 2000-01-01 and s1 is
    // 2090-12-30. The patro dates use the coverage range in the same way.
    int start = (int) NepaliDate.of(2000, 1, 1).daysSince(NepaliDate.MIN);
    int end = (int) NepaliDate.of(2090, 12, 30).daysSince(NepaliDate.MIN);
    int step = (end - start) / (COUNT - 1);
    for (int i = 0; i < COUNT; i++) {
      NepaliDate date = NepaliDate.MIN.plusDays(start + (long) i * step);
      BS_YEAR[i] = date.year();
      BS_MONTH[i] = date.monthNumber();
      BS_DAY[i] = date.day();
      LocalDate ad = date.toLocalDate();
      AD_YEAR[i] = ad.getYear();
      AD_MONTH[i] = ad.getMonthValue();
      AD_DAY[i] = ad.getDayOfMonth();
      TEXT[i] = date.toShortString();
    }
    int patroStart = (int) Patro.coverage().start().daysSince(NepaliDate.MIN);
    int patroEnd = (int) Patro.coverage().end().daysSince(NepaliDate.MIN);
    int patroStep = (patroEnd - patroStart) / (COUNT - 1);
    for (int i = 0; i < COUNT; i++) {
      PATRO_SERIAL[i] = patroStart + i * patroStep;
      NepaliDate date = NepaliDate.MIN.plusDays(PATRO_SERIAL[i]);
      KOTLIN_PATRO_DATES[i] = io.github.sanamhub.nepdate.NepaliDate.ofEpochDay(date.toEpochDay());
    }
  }

  private BenchmarkInputs() {}
}
