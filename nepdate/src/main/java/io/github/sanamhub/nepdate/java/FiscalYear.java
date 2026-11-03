package io.github.sanamhub.nepdate.java;

/**
 * A Nepali fiscal year, from 1 Shrawan to the last day of Ashad of the next BS year.
 *
 * <p>Fiscal year 2082/83 is {@code FiscalYear.of(2082)}. The value accepts any start year, and a
 * method whose result falls outside BS 1901 to 2199 throws: {@code FiscalYear.of(1900).start()}
 * fails because 1 Shrawan 1900 is before the data, while {@code
 * FiscalYear.containing(NepaliDate.MIN)} is that same value.
 */
public final class FiscalYear implements Comparable<FiscalYear> {
  private final int startYear;

  private FiscalYear(int startYear) {
    this.startYear = startYear;
  }

  /**
   * The fiscal year that starts in a BS year.
   *
   * @param startYear the BS year the fiscal year starts in
   * @return the fiscal year value
   */
  public static FiscalYear of(int startYear) {
    return new FiscalYear(startYear);
  }

  /**
   * The fiscal year containing a date: its BS year from Shrawan on, the year before until Ashad.
   *
   * @param date the date
   * @return the fiscal year containing it
   */
  public static FiscalYear containing(NepaliDate date) {
    return new FiscalYear(date.monthNumber() >= 4 ? date.year() : date.year() - 1);
  }

  /**
   * The BS year the fiscal year starts in.
   *
   * @return the start year
   */
  public int startYear() {
    return startYear;
  }

  /**
   * 1 Shrawan of the start year.
   *
   * @return the first day
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} outside the supported span
   */
  public NepaliDate start() {
    return NepaliDate.of(startYear, 4, 1);
  }

  /**
   * The last day of Ashad of the next BS year.
   *
   * @return the last day
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} outside the supported span
   */
  public NepaliDate end() {
    long ashadYear = (long) startYear + 1;
    if (ashadYear > CalendarTable.MAX_YEAR) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE,
          0,
          "fiscal year " + startYear + " ends after " + NepaliDate.MAX);
    }
    return NepaliDate.of((int) ashadYear, 3, 1).lastDayOfMonth();
  }

  /**
   * Every day of the fiscal year.
   *
   * @return the range from {@link #start()} to {@link #end()}
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when an end is outside the
   *     supported span
   */
  public NepaliDateRange range() {
    return NepaliDateRange.of(start(), end());
  }

  /**
   * Every day of one quarter of the fiscal year.
   *
   * <p>This works when only part of the fiscal year is supported, for example {@code
   * FiscalYear.of(1900).quarterRange(Quarter.Q4)}.
   *
   * @param quarter the quarter
   * @return the range of the quarter's three months
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} outside the supported span
   */
  public NepaliDateRange quarterRange(Quarter quarter) {
    long year = (long) startYear + (quarter == Quarter.Q4 ? 1 : 0);
    if (year > CalendarTable.MAX_YEAR) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE,
          0,
          "quarter " + quarter + " of fiscal year " + startYear + " ends after " + NepaliDate.MAX);
    }
    int firstMonth = quarter.months().get(0).number();
    NepaliDate firstDay = NepaliDate.of((int) year, firstMonth, 1);
    return NepaliDateRange.of(
        firstDay, NepaliDate.of((int) year, firstMonth + 2, 1).lastDayOfMonth());
  }

  /**
   * The label of the fiscal year, for example {@code 2082/83}: the start year, a slash and the last
   * two digits of the next year.
   *
   * @return the label in ASCII digits
   */
  public String label() {
    return label(Language.ENGLISH);
  }

  /**
   * The label of the fiscal year in one language.
   *
   * @param language ASCII or Devanagari digits
   * @return the label, for example {@code २०८२/८३}
   */
  public String label(Language language) {
    StringBuilder out = new StringBuilder(10);
    String year = Integer.toString(startYear);
    if (language == Language.NEPALI) {
      for (int i = 0; i < year.length(); i++) {
        char c = year.charAt(i);
        out.append(c >= '0' && c <= '9' ? (char) ('\u0966' + (c - '0')) : c);
      }
    } else {
      out.append(year);
    }
    out.append('/');
    Text.appendNumber(out, Math.floorMod((long) startYear + 1, 100L), 2, language);
    return out.toString();
  }

  /**
   * Compares by start year.
   *
   * @param other the other fiscal year
   * @return negative when this one starts earlier
   */
  @Override
  public int compareTo(FiscalYear other) {
    return Integer.compare(startYear, other.startYear);
  }

  /**
   * Whether both values start in the same BS year.
   *
   * @param other the other value
   * @return true for the same start year
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof FiscalYear year && year.startYear == startYear;
  }

  /**
   * A hash code from the start year.
   *
   * @return the start year
   */
  @Override
  public int hashCode() {
    return startYear;
  }

  /**
   * The same as {@link #label()}, for example {@code 2082/83}.
   *
   * @return the label
   */
  @Override
  public String toString() {
    return label();
  }
}
