package io.github.sanamhub.nepdate.java.bench;

import dev.shivathapaa.nepalidatepickerkmp.calendar_model.NepaliDateConverter;
import dev.shivathapaa.nepalidatepickerkmp.data.CustomCalendar;
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
 * B1 and B2 for the one Maven Central competitor that passes BENCHMARK section 7, {@code
 * io.github.shivathapaa:nepali-date-picker-core-jvm} 3.3.0.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
public class ShivathapaaBenchmark {
  private static final NepaliDateConverter CONVERTER = NepaliDateConverter.INSTANCE;

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b1BsToAd() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      CustomCalendar date =
          CONVERTER.convertNepaliToEnglish(
              BenchmarkInputs.BS_YEAR[i], BenchmarkInputs.BS_MONTH[i], BenchmarkInputs.BS_DAY[i]);
      sum += date.getYear() + date.getMonth() + date.getDayOfMonth();
    }
    return sum;
  }

  @Benchmark
  @OperationsPerInvocation(BenchmarkInputs.COUNT)
  public int b2AdToBs() {
    int sum = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      CustomCalendar date =
          CONVERTER.convertEnglishToNepali(
              BenchmarkInputs.AD_YEAR[i], BenchmarkInputs.AD_MONTH[i], BenchmarkInputs.AD_DAY[i]);
      sum += date.getYear() + date.getMonth() + date.getDayOfMonth();
    }
    return sum;
  }
}
