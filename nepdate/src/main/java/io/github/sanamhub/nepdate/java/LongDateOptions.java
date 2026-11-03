package io.github.sanamhub.nepdate.java;

/**
 * The three switches of the long date form.
 *
 * <p>The defaults match the C# {@code ToLongDateString}: no weekday, the year, and a zero-padded
 * day.
 *
 * @param weekday start with the weekday's name
 * @param year end with the year
 * @param pad write the day with two digits
 */
public record LongDateOptions(boolean weekday, boolean year, boolean pad) {
  /** No weekday, the year, and a zero-padded day: {@code Shrawan 05, 2080}. */
  public static final LongDateOptions DEFAULT = new LongDateOptions(false, true, true);

  /**
   * The same options with the weekday added or removed.
   *
   * @param withWeekday whether the weekday is written
   * @return the new options
   */
  public LongDateOptions withWeekday(boolean withWeekday) {
    return new LongDateOptions(withWeekday, year, pad);
  }

  /**
   * The same options with the year added or removed.
   *
   * @param withYear whether the year is written
   * @return the new options
   */
  public LongDateOptions withYear(boolean withYear) {
    return new LongDateOptions(weekday, withYear, pad);
  }

  /**
   * The same options with the day padding changed.
   *
   * @param withPad whether the day is written with two digits
   * @return the new options
   */
  public LongDateOptions withPad(boolean withPad) {
    return new LongDateOptions(weekday, year, withPad);
  }
}
