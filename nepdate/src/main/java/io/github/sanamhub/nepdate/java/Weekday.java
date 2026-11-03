package io.github.sanamhub.nepdate.java;

import java.time.DayOfWeek;

/**
 * The days of the week, Sunday first as Nepali calendars print them.
 *
 * <p>{@link #index()} is the number the specification uses, 0 for Sunday to 6 for Saturday. {@link
 * #toDayOfWeek()} gives the {@link java.time.DayOfWeek} of the same day, where Monday is 1 and
 * Sunday is 7.
 */
public enum Weekday {
  /** Sunday, the first day of the Nepali week. */
  SUNDAY(0, "Sunday", "Sun", "आइतवार", "आइत", DayOfWeek.SUNDAY),
  /** Monday. */
  MONDAY(1, "Monday", "Mon", "सोमवार", "सोम", DayOfWeek.MONDAY),
  /** Tuesday. */
  TUESDAY(2, "Tuesday", "Tue", "मङ्गलवार", "मङ्गल", DayOfWeek.TUESDAY),
  /** Wednesday. */
  WEDNESDAY(3, "Wednesday", "Wed", "बुधवार", "बुध", DayOfWeek.WEDNESDAY),
  /** Thursday. */
  THURSDAY(4, "Thursday", "Thu", "बिहिवार", "बिहि", DayOfWeek.THURSDAY),
  /** Friday. */
  FRIDAY(5, "Friday", "Fri", "शुक्रवार", "शुक्र", DayOfWeek.FRIDAY),
  /** Saturday, the weekly holiday in Nepal. */
  SATURDAY(6, "Saturday", "Sat", "शनिवार", "शनि", DayOfWeek.SATURDAY);

  private final int index;
  private final String english;
  private final String shortEnglish;
  private final String nepali;
  private final String shortNepali;
  private final DayOfWeek dayOfWeek;

  /** Cached because {@link #values()} clones the array on every call. */
  private static final Weekday[] VALUES = values();

  Weekday(
      int index,
      String english,
      String shortEnglish,
      String nepali,
      String shortNepali,
      DayOfWeek dayOfWeek) {
    this.index = index;
    this.english = english;
    this.shortEnglish = shortEnglish;
    this.nepali = nepali;
    this.shortNepali = shortNepali;
    this.dayOfWeek = dayOfWeek;
  }

  /**
   * The specification's weekday number.
   *
   * @return 0 for Sunday to 6 for Saturday
   */
  public int index() {
    return index;
  }

  /**
   * The full English name.
   *
   * @return for example {@code Tuesday}
   */
  public String english() {
    return english;
  }

  /**
   * The three-letter English name used in calendar headers.
   *
   * @return for example {@code Tue}
   */
  public String shortEnglish() {
    return shortEnglish;
  }

  /**
   * The full Nepali name in Devanagari.
   *
   * @return for example {@code मङ्गलवार}
   */
  public String nepali() {
    return nepali;
  }

  /**
   * The short Nepali name used in calendar headers.
   *
   * @return for example {@code मङ्गल}
   */
  public String shortNepali() {
    return shortNepali;
  }

  /**
   * The weekday's name in one language.
   *
   * @param language the language of the name
   * @return the English or the Nepali name
   */
  public String name(Language language) {
    return language == Language.NEPALI ? nepali : english;
  }

  /**
   * The weekday's short name in one language.
   *
   * @param language the language of the name
   * @return the English or the Nepali short name
   */
  public String shortName(Language language) {
    return language == Language.NEPALI ? shortNepali : shortEnglish;
  }

  /**
   * The {@link java.time.DayOfWeek} of the same day, for code that works with {@code java.time}.
   *
   * @return the same day in the ISO numbering, where Monday is 1 and Sunday is 7
   */
  public DayOfWeek toDayOfWeek() {
    return dayOfWeek;
  }

  /**
   * The weekday with a specification index.
   *
   * @param index 0 for Sunday to 6 for Saturday
   * @return the weekday
   * @throws IllegalArgumentException outside 0 to 6
   */
  public static Weekday ofIndex(int index) {
    if (index < 0 || index > 6) {
      throw new IllegalArgumentException("weekday index " + index + " is outside 0..6");
    }
    return VALUES[index];
  }
}
