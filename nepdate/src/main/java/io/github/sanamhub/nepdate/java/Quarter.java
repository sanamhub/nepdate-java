package io.github.sanamhub.nepdate.java;

import java.util.List;

/** The four quarters of Nepal's fiscal year, which starts on 1 Shrawan. */
public enum Quarter {
  /** Shrawan, Bhadra and Ashoj, the first three months of the fiscal year. */
  Q1(Month.SHRAWAN, Month.BHADRA, Month.ASHOJ),
  /** Kartik, Mangsir and Poush. */
  Q2(Month.KARTIK, Month.MANGSIR, Month.POUSH),
  /** Magh, Falgun and Chaitra, the last three months of the calendar year. */
  Q3(Month.MAGH, Month.FALGUN, Month.CHAITRA),
  /** Baishakh, Jestha and Ashad, the last three months of the fiscal year. */
  Q4(Month.BAISHAKH, Month.JESTHA, Month.ASHAD);

  private final List<Month> months;

  Quarter(Month first, Month second, Month third) {
    this.months = List.of(first, second, third);
  }

  /**
   * The quarter's three months, in order.
   *
   * @return an unmodifiable list of three months
   */
  public List<Month> months() {
    return months;
  }

  /**
   * The quarter a BS month belongs to.
   *
   * @param month the month number, 1 to 12
   * @return Q1 for Shrawan to Ashoj, Q2 for Kartik to Poush, Q3 for Magh to Chaitra and Q4 for
   *     Baishakh to Ashad
   * @throws NepDateException with {@link NepDateError#INVALID_MONTH} outside 1 to 12
   */
  public static Quarter ofMonth(int month) {
    if (month < 1 || month > 12) {
      throw new NepDateException(
          NepDateError.INVALID_MONTH, 0, "month " + month + " is outside 1..12");
    }
    return values()[((month + 8) % 12) / 3];
  }
}
