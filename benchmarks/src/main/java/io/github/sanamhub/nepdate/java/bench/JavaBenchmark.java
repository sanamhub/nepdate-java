package io.github.sanamhub.nepdate.java.bench;

import io.github.sanamhub.nepdate.java.Language;
import io.github.sanamhub.nepdate.java.LongDateOptions;
import io.github.sanamhub.nepdate.java.MonthOverflow;
import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.NepaliDateRange;
import io.github.sanamhub.nepdate.java.patro.CalendarInfo;
import io.github.sanamhub.nepdate.java.patro.Patro;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/** B1 to B8 of {@code spec/BENCHMARK.md} for this port. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
public class JavaBenchmark {
  /** Built once: the measured loop must not allocate the options object. */
  private static final LongDateOptions NEPALI_LONG = LongDateOptions.DEFAULT.withWeekday(true);

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b1BsToAd() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate date =
          NepaliDate.of(
              BenchmarkInputs.BS_YEAR[i], BenchmarkInputs.BS_MONTH[i], BenchmarkInputs.BS_DAY[i]);
      sum += date.gregorianYear() + date.gregorianMonth() + date.gregorianDay();
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
      sum += date.year() + date.monthNumber() + date.day();
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
      sum += date.toYmd();
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
      sum += date.toYmd();
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
      sum += date.toLongString(NEPALI_LONG, Language.NEPALI).length();
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b6StrictParse() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      sum += NepaliDate.parse(BenchmarkInputs.TEXT[i]).toYmd();
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b7PatroInfo() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      CalendarInfo info = Patro.info(NepaliDate.MIN.plusDays(BenchmarkInputs.PATRO_SERIAL[i]));
      sum += info.events().size() + (info.isPublicHoliday() ? 1 : 0);
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b8PatroMonthGrid() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate date = NepaliDate.MIN.plusDays(BenchmarkInputs.PATRO_SERIAL[i]);
      NepaliDateRange month = NepaliDateRange.forMonth(date.year(), date.monthNumber());
      for (NepaliDate day : month) {
        CalendarInfo info = Patro.info(day);
        sum += info.events().size() + (info.isPublicHoliday() ? 1 : 0);
      }
    }
    return sum;
  }
}
