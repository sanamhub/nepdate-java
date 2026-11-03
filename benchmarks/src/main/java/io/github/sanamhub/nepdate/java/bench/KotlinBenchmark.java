package io.github.sanamhub.nepdate.java.bench;

import io.github.sanamhub.nepdate.Lang;
import io.github.sanamhub.nepdate.MonthOverflow;
import io.github.sanamhub.nepdate.NepaliDate;
import io.github.sanamhub.nepdate.NepaliDateRange;
import io.github.sanamhub.nepdate.patro.CalendarInfo;
import io.github.sanamhub.nepdate.patro.Patro;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/**
 * B1 to B8 for the published Kotlin artifact, called from Java in the same JVM and the same run as
 * {@link JavaBenchmark}. This is the comparison ADR-0008 asks for, not a separate harness.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
public class KotlinBenchmark {
  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b1BsToAd() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate date =
          NepaliDate.of(
              BenchmarkInputs.BS_YEAR[i], BenchmarkInputs.BS_MONTH[i], BenchmarkInputs.BS_DAY[i]);
      sum += date.getGregorianYear() + date.getGregorianMonth() + date.getGregorianDay();
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b2AdToBs() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate date =
          NepaliDate.ofGregorian(
              BenchmarkInputs.AD_YEAR[i], BenchmarkInputs.AD_MONTH[i], BenchmarkInputs.AD_DAY[i]);
      sum += date.getYear() + date.getMonthValue() + date.getDayOfMonth();
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b3AddDays() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate date =
          NepaliDate.of(
                  BenchmarkInputs.BS_YEAR[i],
                  BenchmarkInputs.BS_MONTH[i],
                  BenchmarkInputs.BS_DAY[i])
              .plusDays(1000);
      sum += (date.getYear() + date.getMonthValue() + date.getDayOfMonth());
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b4AddMonths() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate date =
          NepaliDate.of(
                  BenchmarkInputs.BS_YEAR[i],
                  BenchmarkInputs.BS_MONTH[i],
                  BenchmarkInputs.BS_DAY[i])
              .plusMonths(13, MonthOverflow.CLAMP);
      sum += (date.getYear() + date.getMonthValue() + date.getDayOfMonth());
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b5LongNepali() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate date =
          NepaliDate.of(
              BenchmarkInputs.BS_YEAR[i], BenchmarkInputs.BS_MONTH[i], BenchmarkInputs.BS_DAY[i]);
      sum += date.toLongString(true, true, true, Lang.NEPALI).length();
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b6StrictParse() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      sum += NepaliDate.parse(BenchmarkInputs.TEXT[i]).getYear();
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b7PatroInfo() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      CalendarInfo info = Patro.info(BenchmarkInputs.KOTLIN_PATRO_DATES[i]);
      sum += info.getEvents().size() + (info.isPublicHoliday() ? 1 : 0);
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b8PatroMonthGrid() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate date = BenchmarkInputs.KOTLIN_PATRO_DATES[i];
      NepaliDateRange month = NepaliDateRange.ofMonth(date.getYear(), date.getMonthValue());
      for (NepaliDate day : month) {
        CalendarInfo info = Patro.info(day);
        sum += info.getEvents().size() + (info.isPublicHoliday() ? 1 : 0);
      }
    }
    return sum;
  }
}
