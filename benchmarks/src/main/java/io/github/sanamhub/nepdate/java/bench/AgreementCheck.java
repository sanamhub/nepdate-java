package io.github.sanamhub.nepdate.java.bench;

import dev.shivathapaa.nepalidatepickerkmp.calendar_model.NepaliDateConverter;
import dev.shivathapaa.nepalidatepickerkmp.data.CustomCalendar;
import io.github.sanamhub.nepdate.java.NepaliDate;

/**
 * Checks the competitor's 64 answers against the shared data, in both directions, and prints the
 * count. BENCHMARK section 4 requires the agreement column; a fast wrong answer is listed, never
 * ranked first.
 */
public final class AgreementCheck {
  private AgreementCheck() {}

  public static void main(String[] args) {
    NepaliDateConverter converter = NepaliDateConverter.INSTANCE;
    int agreesToAd = 0;
    int agreesToBs = 0;
    for (int i = 0; i < BenchmarkInputs.COUNT; i++) {
      NepaliDate bs =
          NepaliDate.of(
              BenchmarkInputs.BS_YEAR[i], BenchmarkInputs.BS_MONTH[i], BenchmarkInputs.BS_DAY[i]);
      CustomCalendar ad =
          converter.convertNepaliToEnglish(
              BenchmarkInputs.BS_YEAR[i], BenchmarkInputs.BS_MONTH[i], BenchmarkInputs.BS_DAY[i]);
      if (ad.getYear() == bs.gregorianYear()
          && ad.getMonth() == bs.gregorianMonth()
          && ad.getDayOfMonth() == bs.gregorianDay()) {
        agreesToAd++;
      } else {
        System.out.println(
            "BS->AD differs at "
                + bs
                + ": expected "
                + bs.toLocalDate()
                + ", got "
                + ad.getYear()
                + "-"
                + ad.getMonth()
                + "-"
                + ad.getDayOfMonth());
      }

      NepaliDate expected =
          NepaliDate.ofGregorian(
              BenchmarkInputs.AD_YEAR[i], BenchmarkInputs.AD_MONTH[i], BenchmarkInputs.AD_DAY[i]);
      CustomCalendar got =
          converter.convertEnglishToNepali(
              BenchmarkInputs.AD_YEAR[i], BenchmarkInputs.AD_MONTH[i], BenchmarkInputs.AD_DAY[i]);
      if (got.getYear() == expected.year()
          && got.getMonth() == expected.monthNumber()
          && got.getDayOfMonth() == expected.day()) {
        agreesToBs++;
      } else {
        System.out.println(
            "AD->BS differs at "
                + expected.toLocalDate()
                + ": expected "
                + expected
                + ", got "
                + got.getYear()
                + "-"
                + got.getMonth()
                + "-"
                + got.getDayOfMonth());
      }
    }
    System.out.println("shivathapaa agrees BS->AD on " + agreesToAd + "/" + BenchmarkInputs.COUNT);
    System.out.println("shivathapaa agrees AD->BS on " + agreesToBs + "/" + BenchmarkInputs.COUNT);
  }
}
