package io.github.sanamhub.nepdate.java;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A date in the Bikram Sambat (BS) calendar, from 1901-01-01 to 2199-12-30, which is AD 1844-04-11
 * to 2143-04-15.
 *
 * <p>A value is always a real day: an invalid date cannot be constructed, so there is no default or
 * zero date and no method returns {@code null}. Instances are immutable and safe to share between
 * threads. Build one with {@link #of(int, int, int)}, {@link #of(LocalDate)}, {@link
 * #parse(CharSequence)}, {@link #ofYmd(int)} or {@link #today(ZoneId)}.
 *
 * <p>The C# library calls this type {@code NepaliDate} and its three parts {@code Year}, {@code
 * Month}, {@code Day} and {@code MonthName}; here they are {@link #year()}, {@link #monthNumber()},
 * {@link #day()} and {@link #month()}.
 *
 * <p>This class is {@link Serializable}: the stream holds the canonical {@code YYYY-MM-DD} text and
 * reads it back with {@link #parse(CharSequence)}, so a serialized date is portable and readable.
 */
public final class NepaliDate implements Comparable<NepaliDate>, Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** The first supported date, BS 1901-01-01 (AD 1844-04-11). */
  public static final NepaliDate MIN = new NepaliDate(CalendarTable.MIN_SERIAL);

  /** The last supported date, BS 2199-12-30 (AD 2143-04-15). */
  public static final NepaliDate MAX = new NepaliDate(CalendarTable.MAX_SERIAL);

  /**
   * Nepal's time zone, {@code Asia/Kathmandu} (UTC+05:45, no daylight saving).
   *
   * <p>It is a convenience constant, not a default: every clock method takes its zone explicitly,
   * so nothing reads a hidden local zone. On a JDK without time-zone data it falls back to a fixed
   * +05:45 offset.
   */
  public static final ZoneId KATHMANDU = kathmandu();

  /** Days since BS 1901-01-01. */
  private final int serial;

  /** The BS date packed as year, month and day. */
  private final int ymd;

  private NepaliDate(int serial) {
    this.serial = serial;
    this.ymd = Calendar.fromSerial(serial);
  }

  private static ZoneId kathmandu() {
    try {
      return ZoneId.of("Asia/Kathmandu");
    } catch (ZoneRulesException missingZoneData) {
      return ZoneOffset.ofHoursMinutes(5, 45);
    }
  }

  /**
   * The BS date {@code year-month-day}.
   *
   * <p>The checks run in the order year, month, day, and the first failure is the error kind.
   *
   * @param year the BS year, 1901 to 2199
   * @param month the month number, 1 to 12
   * @param day the day of the month, 1 to its length
   * @return the date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE}, {@link
   *     NepDateError#INVALID_MONTH} or {@link NepDateError#INVALID_DAY}
   */
  public static NepaliDate of(int year, int month, int day) {
    NepDateError error = validationError(year, month, day);
    if (error != null) {
      throw errorOf(error, year, month, day);
    }
    return new NepaliDate(Calendar.toSerial(year, month, day));
  }

  /**
   * The BS date {@code year-month-day} with a month constant.
   *
   * @param year the BS year
   * @param month the month
   * @param day the day of the month
   * @return the date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} or {@link
   *     NepDateError#INVALID_DAY}
   */
  public static NepaliDate of(int year, Month month, int day) {
    return of(year, month.number(), day);
  }

  /**
   * The BS date of a Gregorian date.
   *
   * @param date the Gregorian date; its time of day, if any, does not exist on this type
   * @return the same day in the Bikram Sambat calendar
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} outside AD 1844-04-11 to
   *     2143-04-15
   */
  public static NepaliDate of(LocalDate date) {
    long epochDay = date.toEpochDay();
    return ofEpochDay(epochDay);
  }

  /**
   * The BS date of a Gregorian date given as three numbers.
   *
   * @param year the Gregorian year, 1844 to 2143
   * @param month the Gregorian month, 1 to 12
   * @param day the day of the month
   * @return the BS date
   * @throws NepDateException with {@link NepDateError#INVALID_GREGORIAN} when the AD date does not
   *     exist, else {@link NepDateError#OUT_OF_RANGE} outside the supported span
   */
  public static NepaliDate ofGregorian(int year, int month, int day) {
    boolean realDate =
        month >= 1 && month <= 12 && day >= 1 && day <= Calendar.gregorianMonthLength(year, month);
    if (!realDate) {
      throw new NepDateException(
          NepDateError.INVALID_GREGORIAN,
          0,
          "AD " + year + "-" + month + "-" + day + " is not a date in the Gregorian calendar");
    }
    if (year < 1844 || year > 2143) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE,
          0,
          "AD " + year + "-" + month + "-" + day + " is outside 1844-04-11..2143-04-15");
    }
    int serial = Calendar.serialOfCivil(year, month, day);
    if (serial < CalendarTable.MIN_SERIAL || serial > CalendarTable.MAX_SERIAL) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE,
          0,
          "AD " + year + "-" + month + "-" + day + " is outside 1844-04-11..2143-04-15");
    }
    return new NepaliDate(serial);
  }

  /**
   * The BS date {@code days} days after AD 1970-01-01, as {@link LocalDate#ofEpochDay(long)}.
   *
   * @param epochDay days since 1970-01-01, -45920 to 63291
   * @return the BS date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} outside that span
   */
  public static NepaliDate ofEpochDay(long epochDay) {
    long first = CalendarTable.MIN_SERIAL + (long) CalendarTable.EPOCH_UNIX_DAYS;
    long last = CalendarTable.MAX_SERIAL + (long) CalendarTable.EPOCH_UNIX_DAYS;
    if (epochDay < first || epochDay > last) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE,
          0,
          "epoch day " + epochDay + " is outside " + first + ".." + last);
    }
    return new NepaliDate((int) (epochDay - CalendarTable.EPOCH_UNIX_DAYS));
  }

  /**
   * The date of the packed {@code YYYYMMDD} integer, for example {@code 20810415}.
   *
   * <p>Every {@code int} is accepted as input and none is truncated: the value is split into year,
   * month and day and then validated, so {@code 2147483647} is an out-of-range year.
   *
   * @param value the packed integer, 19010101 to 21991230
   * @return the date
   * @throws NepDateException as {@link #of(int, int, int)}
   */
  public static NepaliDate ofYmd(int value) {
    return of(value / 10000, value / 100 % 100, value % 100);
  }

  /**
   * Parses text strictly: exactly year, month and day in that order.
   *
   * <p>Digits may be ASCII or Devanagari and may be mixed. Separators are {@code - / . _ \ space ।
   * |}; leading, trailing and repeated separators are ignored. Any other character, a fourth group,
   * fewer than three groups or a group longer than four digits fails.
   *
   * @param text the input, for example {@code 2080/05/15}
   * @return the date
   * @throws NepDateException with a parse error, then a validation error as {@link #of(int, int,
   *     int)}
   */
  public static NepaliDate parse(CharSequence text) {
    return Parsing.parse(text);
  }

  /**
   * Parses text strictly without throwing.
   *
   * @param text the input
   * @return the date, or empty when it does not parse or is not a valid date
   */
  public static Optional<NepaliDate> tryParse(CharSequence text) {
    return Parsing.tryParse(text);
  }

  /**
   * Parses human input leniently: the strict form first, then month names, era markers and either
   * date order.
   *
   * <p>Accepted forms include {@code 2080/05/15}, {@code 15/05/2080}, {@code 15 Shrawan 2080},
   * {@code Shrawan 15, 2080}, {@code १५ साउन २०८०} and {@code 2080/04/15 BS}. It matches the input
   * as given: no Unicode normalisation and no removal of zero-width joiners. A two-digit year is
   * ambiguous and is rejected rather than guessed.
   *
   * @param text the input
   * @return the date
   * @throws NepDateException with a parse error or a validation error
   */
  public static NepaliDate parseLenient(CharSequence text) {
    return LenientParser.parse(text);
  }

  /**
   * Parses human input leniently without throwing.
   *
   * @param text the input
   * @return the date, or empty
   */
  public static Optional<NepaliDate> tryParseLenient(CharSequence text) {
    return LenientParser.tryParse(text);
  }

  /**
   * Today's date in a zone, read from the system clock.
   *
   * @param zone the zone whose current date is wanted
   * @return today's date in that zone
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when the date is outside the
   *     supported span
   */
  public static NepaliDate today(ZoneId zone) {
    return of(LocalDate.now(zone));
  }

  /**
   * Today's date in the clock's zone, the testable form of {@link #today(ZoneId)}.
   *
   * @param clock the clock, which carries its own zone
   * @return the clock's current date in its zone
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when the date is outside the
   *     supported span
   */
  public static NepaliDate today(Clock clock) {
    return of(LocalDate.now(clock));
  }

  /**
   * The BS dates of the given Gregorian dates, in input order.
   *
   * <p>This is the one ordered batch conversion in this direction: the C# {@code
   * BulkConvert.ToNepaliDates} and {@code BatchProcessToNepaliDates} are the same operation here,
   * and the {@code useParallel} and {@code batchSize} arguments do not cross into this API.
   *
   * @param gregorianDates the Gregorian dates
   * @return one date per input, in the order given
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} for the first input outside the
   *     supported span
   */
  public static List<NepaliDate> toNepaliDates(Iterable<? extends LocalDate> gregorianDates) {
    List<NepaliDate> dates = new ArrayList<>(sizeHint(gregorianDates));
    for (LocalDate date : gregorianDates) {
      dates.add(of(date));
    }
    return List.copyOf(dates);
  }

  /**
   * The Gregorian dates of the given BS dates, in input order.
   *
   * <p>The C# {@code BulkConvert.ToEnglishDates} and {@code BatchProcessToEnglishDates} are this
   * one operation.
   *
   * @param nepaliDates the BS dates
   * @return one Gregorian date per input, in the order given
   */
  public static List<LocalDate> toEnglishDates(Iterable<NepaliDate> nepaliDates) {
    List<LocalDate> dates = new ArrayList<>(sizeHint(nepaliDates));
    for (NepaliDate date : nepaliDates) {
      dates.add(date.toLocalDate());
    }
    return List.copyOf(dates);
  }

  /**
   * The Gregorian dates of BS date strings, each read by {@link #parse(CharSequence)}, in input
   * order.
   *
   * @param texts the BS dates as text
   * @return one Gregorian date per input, in the order given
   * @throws NepDateException when a string does not parse, so no partial list is returned
   */
  public static List<LocalDate> parseToEnglishDates(Iterable<String> texts) {
    List<LocalDate> dates = new ArrayList<>(sizeHint(texts));
    for (String text : texts) {
      dates.add(parse(text).toLocalDate());
    }
    return List.copyOf(dates);
  }

  private static int sizeHint(Iterable<?> items) {
    return items instanceof Collection<?> collection ? collection.size() : 10;
  }

  /**
   * The BS year.
   *
   * @return 1901 to 2199
   */
  public int year() {
    return Calendar.yearOf(ymd);
  }

  /**
   * The BS month number, as the C# {@code Month} property.
   *
   * @return 1 to 12
   */
  public int monthNumber() {
    return Calendar.monthOf(ymd);
  }

  /**
   * The BS month, as the C# {@code NepaliMonths MonthName} property.
   *
   * @return the month constant
   */
  public Month month() {
    return Month.of(monthNumber());
  }

  /**
   * The day of the BS month.
   *
   * @return 1 to the length of the month
   */
  public int day() {
    return Calendar.dayOf(ymd);
  }

  /**
   * The weekday, as the C# {@code DayOfWeek} property.
   *
   * @return the weekday, Sunday first
   */
  public Weekday weekday() {
    return Weekday.ofIndex(Calendar.weekdayOf(serial));
  }

  /**
   * The 1-based day of the BS year.
   *
   * <p>The count restarts at 1 on 1 Baishakh, so it is not the Gregorian day of the year.
   *
   * @return 1 to 366
   */
  public int dayOfYear() {
    int index = (year() - CalendarTable.MIN_YEAR) * 12;
    return serial - CalendarTable.MONTH_START[index] + 1;
  }

  /**
   * The length of this date's BS month, as the C# {@code MonthEndDay} property.
   *
   * @return 29 to 32
   */
  public int monthLength() {
    return Calendar.monthLength(year(), monthNumber());
  }

  /**
   * The length of this date's BS year, the sum of its twelve months.
   *
   * <p>This replaces the C# {@code IsLeapYear()}, whose result follows the Gregorian year and so
   * changes in the middle of a BS year.
   *
   * @return 364 to 367
   */
  public int yearLength() {
    return Calendar.yearLength(year());
  }

  /**
   * The first day of this date's BS month.
   *
   * @return the month's first day
   */
  public NepaliDate firstDayOfMonth() {
    return new NepaliDate(Calendar.toSerial(year(), monthNumber(), 1));
  }

  /**
   * The last day of this date's BS month, as the C# {@code MonthEndDate()}.
   *
   * @return the month's last day
   */
  public NepaliDate lastDayOfMonth() {
    return new NepaliDate(Calendar.toSerial(year(), monthNumber(), monthLength()));
  }

  /**
   * The fiscal year containing this date.
   *
   * @return the fiscal year, which starts on 1 Shrawan
   */
  public FiscalYear fiscalYear() {
    return FiscalYear.containing(this);
  }

  /**
   * The fiscal quarter containing this date's month.
   *
   * @return the quarter
   */
  public Quarter quarter() {
    return Quarter.ofMonth(monthNumber());
  }

  /**
   * The Gregorian year of this date.
   *
   * @return the AD year
   */
  public int gregorianYear() {
    return Calendar.yearOf(Calendar.packedCivilOf(serial));
  }

  /**
   * The Gregorian month of this date.
   *
   * @return 1 to 12
   */
  public int gregorianMonth() {
    return Calendar.monthOf(Calendar.packedCivilOf(serial));
  }

  /**
   * The Gregorian day of the month of this date.
   *
   * @return the day
   */
  public int gregorianDay() {
    return Calendar.dayOf(Calendar.packedCivilOf(serial));
  }

  /**
   * This date as a {@link LocalDate}.
   *
   * @return the same day in the ISO calendar, at midnight when a time of day is wanted
   */
  public LocalDate toLocalDate() {
    return LocalDate.ofEpochDay(toEpochDay());
  }

  /**
   * The days from AD 1970-01-01 to this date, the value {@link LocalDate#toEpochDay()} returns.
   *
   * @return -45920 for {@link #MIN} to 63291 for {@link #MAX}
   */
  public long toEpochDay() {
    return (long) serial + CalendarTable.EPOCH_UNIX_DAYS;
  }

  /**
   * This date packed as the {@code YYYYMMDD} integer, the inverse of {@link #ofYmd(int)}.
   *
   * @return 19010101 to 21991230
   */
  public int toYmd() {
    return year() * 10000 + monthNumber() * 100 + day();
  }

  /**
   * The date {@code days} later, or earlier for a negative value, as the C# {@code AddDays}.
   *
   * @param days the number of days
   * @return the new date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when the result leaves the
   *     supported span
   */
  public NepaliDate plusDays(long days) {
    if (days < -CalendarTable.MAX_SERIAL || days > CalendarTable.MAX_SERIAL) {
      throw daysOutOfRange(days);
    }
    long result = serial + days;
    if (result < CalendarTable.MIN_SERIAL || result > CalendarTable.MAX_SERIAL) {
      throw daysOutOfRange(days);
    }
    return new NepaliDate((int) result);
  }

  /**
   * The date {@code days} earlier, as the C# subtraction path.
   *
   * @param days the number of days
   * @return the new date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when the result leaves the
   *     supported span
   */
  public NepaliDate minusDays(long days) {
    if (days == Long.MIN_VALUE) {
      throw daysOutOfRange(days);
    }
    return plusDays(-days);
  }

  /**
   * The same day {@code months} months later, or earlier for a negative value, clamped to the last
   * day of the target month, as the C# {@code AddMonths(months)}.
   *
   * @param months the number of months
   * @return the new date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when the result leaves the
   *     supported span
   */
  public NepaliDate plusMonths(long months) {
    return plusMonths(months, MonthOverflow.CLAMP);
  }

  /**
   * The same day {@code months} months later, with an explicit choice when the day does not exist
   * in the target month, as the C# {@code AddMonths(months, awayFromMonthEnd)}.
   *
   * @param months the number of months
   * @param overflow clamp to the target month's last day, or spill into the next month
   * @return the new date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when the result leaves the
   *     supported span
   */
  public NepaliDate plusMonths(long months, MonthOverflow overflow) {
    if (months < -CalendarTable.MONTHS || months > CalendarTable.MONTHS) {
      throw monthsOutOfRange(months);
    }
    long target = (long) (year() - CalendarTable.MIN_YEAR) * 12 + monthNumber() - 1 + months;
    if (target < 0 || target >= CalendarTable.MONTHS) {
      throw monthsOutOfRange(months);
    }
    int year = CalendarTable.MIN_YEAR + (int) (target / 12);
    int month = (int) (target % 12) + 1;
    int day = day();
    int length = Calendar.monthLength(year, month);
    if (day <= length) {
      return new NepaliDate(Calendar.toSerial(year, month, day));
    }
    if (overflow == MonthOverflow.CLAMP) {
      return new NepaliDate(Calendar.toSerial(year, month, length));
    }
    // Day d of a month too short for it is day (d - length) of the next month, which starts at
    // MONTH_START[target + 1].
    int serial = CalendarTable.MONTH_START[(int) target + 1] + day - length - 1;
    if (serial > CalendarTable.MAX_SERIAL) {
      throw monthsOutOfRange(months);
    }
    return new NepaliDate(serial);
  }

  /**
   * The same day {@code years} years later, clamped, as the C# {@code AddYears(years)}.
   *
   * @param years the number of years
   * @return the new date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when the result leaves the
   *     supported span
   */
  public NepaliDate plusYears(long years) {
    return plusYears(years, MonthOverflow.CLAMP);
  }

  /**
   * The same day {@code years} years later, with an explicit month overflow choice.
   *
   * @param years the number of years
   * @param overflow clamp to the target month's last day, or spill into the next month
   * @return the new date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when the result leaves the
   *     supported span
   */
  public NepaliDate plusYears(long years, MonthOverflow overflow) {
    if (years < -CalendarTable.YEARS || years > CalendarTable.YEARS) {
      throw outOfRange(years + " years");
    }
    return plusMonths(years * 12, overflow);
  }

  /**
   * The number of days from this date to {@code other}: positive when {@code other} is later.
   *
   * <p>This is the direction of the C# named {@code Subtract} replacement and of the
   * specification's {@code days_between(this, other)}. The opposite direction, positive when this
   * date is later, is {@link #daysSince(NepaliDate)}.
   *
   * @param other the other date
   * @return the signed day count, -109211 to 109211
   */
  public long daysUntil(NepaliDate other) {
    return (long) other.serial - serial;
  }

  /**
   * The number of days from {@code other} to this date: positive when this date is later.
   *
   * <p>This keeps the C# operator's direction, {@code this - other}; the named {@link
   * #daysUntil(NepaliDate)} has the opposite sign.
   *
   * @param other the other date
   * @return the signed day count
   */
  public long daysSince(NepaliDate other) {
    return (long) serial - other.serial;
  }

  /**
   * The calendar distance to {@code other} in years, months and days.
   *
   * <p>It is the most whole months that, added with clamping, do not pass {@code other}, and then
   * the days left. The components carry the direction: when {@code other} is earlier, every
   * component is negative, so {@code Period.negated()} gives the distance the other way.
   *
   * @param other the other date
   * @return the distance, zero for the same date
   */
  public java.time.Period until(NepaliDate other) {
    boolean negative = other.serial < serial;
    NepaliDate from = negative ? other : this;
    NepaliDate to = negative ? this : other;
    int months = (to.year() - from.year()) * 12 + (to.monthNumber() - from.monthNumber());
    NepaliDate landed = from.plusMonths(months, MonthOverflow.CLAMP);
    if (landed.compareTo(to) > 0) {
      months--;
      landed = from.plusMonths(months, MonthOverflow.CLAMP);
    }
    int days = to.serial - landed.serial;
    return negative
        ? java.time.Period.of(-(months / 12), -(months % 12), -days)
        : java.time.Period.of(months / 12, months % 12, days);
  }

  /**
   * Whether this date is today in a zone, as the C# {@code IsToday()} with an explicit zone.
   *
   * @param zone the zone whose current date is compared
   * @return true when the zone's current date is this date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public boolean isToday(ZoneId zone) {
    return serial == today(zone).serial;
  }

  /**
   * Whether this date is today in the clock's zone, the testable form of {@link #isToday(ZoneId)}.
   *
   * @param clock the clock to read
   * @return true when the clock's current date is this date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public boolean isToday(Clock clock) {
    return serial == today(clock).serial;
  }

  /**
   * Whether this date is yesterday in a zone, as the C# {@code IsYesterday()}.
   *
   * @param zone the zone whose current date is compared
   * @return true when the day before the zone's current date is this date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public boolean isYesterday(ZoneId zone) {
    return serial == today(zone).serial - 1;
  }

  /**
   * Whether this date is yesterday in the clock's zone.
   *
   * @param clock the clock to read
   * @return true when the day before the clock's current date is this date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public boolean isYesterday(Clock clock) {
    return serial == today(clock).serial - 1;
  }

  /**
   * Whether this date is tomorrow in a zone, as the C# {@code IsTomorrow()}.
   *
   * @param zone the zone whose current date is compared
   * @return true when the day after the zone's current date is this date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public boolean isTomorrow(ZoneId zone) {
    return serial == today(zone).serial + 1;
  }

  /**
   * Whether this date is tomorrow in the clock's zone.
   *
   * @param clock the clock to read
   * @return true when the day after the clock's current date is this date
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public boolean isTomorrow(Clock clock) {
    return serial == today(clock).serial + 1;
  }

  /**
   * The date as {@code YYYY/MM/DD} with ASCII digits, the C# {@code ToString()}.
   *
   * @return for example {@code 2081/04/15}
   */
  @Override
  public String toString() {
    return Text.shortString(this, DateOrder.YMD, Separator.SLASH, true, Language.ENGLISH);
  }

  /**
   * The date as {@code YYYY-MM-DD} with ASCII digits, the canonical interchange text and the form
   * the Java serialization hook writes.
   *
   * @return for example {@code 2081-04-15}
   */
  public String toIsoString() {
    return Text.shortString(this, DateOrder.YMD, Separator.DASH, true, Language.ENGLISH);
  }

  /**
   * The default display, {@code YYYY/MM/DD}, as the C# {@code ToString(DateFormats)} does by
   * default.
   *
   * @return for example {@code 2081/04/15}
   */
  public String toShortString() {
    return toString();
  }

  /**
   * The three numbers in an order and with a separator, the C# {@code ToString(DateFormats,
   * Separators, leadingZeros)}.
   *
   * @param order the field order
   * @param separator the separator character
   * @param pad four-digit year and two-digit month and day
   * @return for example {@code 2081-4-15} with {@link DateOrder#YMD}, {@link Separator#DASH} and no
   *     padding
   */
  public String toShortString(DateOrder order, Separator separator, boolean pad) {
    return Text.shortString(this, order, separator, pad, Language.ENGLISH);
  }

  /**
   * The three numbers in an order, with a separator and a language, the C# {@code ToUnicodeString}
   * generalised.
   *
   * @param order the field order
   * @param separator the separator character
   * @param pad four-digit year and two-digit month and day
   * @param language English or Nepali names and digits
   * @return for example {@code २१८१/०४/१५} for 2081-04-15 in Nepali
   */
  public String toShortString(
      DateOrder order, Separator separator, boolean pad, Language language) {
    return Text.shortString(this, order, separator, pad, language);
  }

  /**
   * The date with the month name, the C# {@code ToLongDateString} with its defaults.
   *
   * @return for example {@code Shrawan 15, 2081}
   */
  public String toLongString() {
    return Text.longString(this, LongDateOptions.DEFAULT, Language.ENGLISH);
  }

  /**
   * The date with the month name, the C# {@code ToLongDateString(leadingZeros, displayDayName,
   * displayYear)} as options.
   *
   * @param options which parts to write
   * @return for example {@code Tuesday, Shrawan 15, 2081}
   */
  public String toLongString(LongDateOptions options) {
    return Text.longString(this, options, Language.ENGLISH);
  }

  /**
   * The date with the month name in a language, the C# {@code ToLongDateUnicodeString} generalised.
   *
   * @param options which parts to write
   * @param language English or Nepali names and digits
   * @return for example {@code मङ्गलवार, साउन १५, २०८१}
   */
  public String toLongString(LongDateOptions options, Language language) {
    return Text.longString(this, options, language);
  }

  /**
   * The date in a .NET-style pattern, the C# {@code ToString(string format)}.
   *
   * <p>The standard patterns are {@code ""}, {@code G}, {@code g} and {@code d} for the default
   * display, {@code D} for the long form and {@code s} for {@code YYYY-MM-DD}. A custom pattern
   * uses {@code yyyy}, {@code yy}, {@code MMMM}, {@code MMM}, {@code MM}, {@code M}, {@code dd} and
   * {@code d}, quoted text and {@code \} escapes.
   *
   * @param pattern the pattern
   * @return the formatted date
   */
  public String format(String pattern) {
    return format(pattern, Language.ENGLISH);
  }

  /**
   * The date in a .NET-style pattern with a language for names and digits.
   *
   * @param pattern the pattern
   * @param language English or Nepali names and digits
   * @return the formatted date
   */
  public String format(String pattern, Language language) {
    return Text.pattern(this, pattern, language);
  }

  /**
   * Appends the default display to a builder, the allocation-free form of {@link #toShortString()}.
   *
   * @param out the destination
   * @return {@code out}
   */
  public StringBuilder appendTo(StringBuilder out) {
    return Text.appendShort(out, this, DateOrder.YMD, Separator.SLASH, true, Language.ENGLISH);
  }

  /**
   * Appends a pattern to a builder, the allocation-free form of {@link #format(String)}.
   *
   * @param out the destination
   * @param pattern the pattern
   * @return {@code out}
   */
  public StringBuilder appendTo(StringBuilder out, String pattern) {
    return appendTo(out, pattern, Language.ENGLISH);
  }

  /**
   * Appends a pattern in a language to a builder.
   *
   * @param out the destination
   * @param pattern the pattern
   * @param language English or Nepali names and digits
   * @return {@code out}
   */
  public StringBuilder appendTo(StringBuilder out, String pattern, Language language) {
    return Text.appendPattern(out, this, pattern, language);
  }

  /**
   * Writes the default display into a fixed buffer, the C# {@code TryFormat}.
   *
   * @param buffer the destination, which is left unchanged when the output does not fit
   * @return true when the output fits
   */
  public boolean tryFormat(char[] buffer) {
    return tryFormat(buffer, "", Language.ENGLISH);
  }

  /**
   * Writes a pattern into a fixed buffer.
   *
   * @param buffer the destination, which is left unchanged when the output does not fit
   * @param pattern the pattern
   * @return true when the output fits
   */
  public boolean tryFormat(char[] buffer, String pattern) {
    return tryFormat(buffer, pattern, Language.ENGLISH);
  }

  /**
   * Writes a pattern in a language into a fixed buffer.
   *
   * @param buffer the destination, which is left unchanged when the output does not fit
   * @param pattern the pattern
   * @param language English or Nepali names and digits
   * @return true when the output fits
   */
  public boolean tryFormat(char[] buffer, String pattern, Language language) {
    return Text.tryFormat(this, buffer, pattern, language);
  }

  /**
   * Compares by day.
   *
   * @param other the other date
   * @return negative when this date is earlier, zero when it is the same day, positive when later
   */
  @Override
  public int compareTo(NepaliDate other) {
    return Integer.compare(serial, other.serial);
  }

  /**
   * Whether both values are the same day.
   *
   * @param other the other value
   * @return true when it is a date on the same day
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof NepaliDate date && date.serial == serial;
  }

  /**
   * A hash code from the day.
   *
   * @return the day's serial
   */
  @Override
  public int hashCode() {
    return serial;
  }

  /**
   * The serial of this date, for the package's own use.
   *
   * @return 0 to 109211
   */
  int serial() {
    return serial;
  }

  /**
   * The date with a serial, for the package's own use.
   *
   * @param serial 0 to 109211
   * @return the date
   */
  static NepaliDate ofSerial(int serial) {
    return new NepaliDate(serial);
  }

  /**
   * The first failing check of ALGORITHM section 3.
   *
   * @param year the BS year
   * @param month the month number
   * @param day the day of the month
   * @return the error, or null for a valid date
   */
  static NepDateError validationError(int year, int month, int day) {
    if (year < CalendarTable.MIN_YEAR || year > CalendarTable.MAX_YEAR) {
      return NepDateError.OUT_OF_RANGE;
    }
    if (month < 1 || month > 12) {
      return NepDateError.INVALID_MONTH;
    }
    if (day < 1 || day > Calendar.monthLength(year, month)) {
      return NepDateError.INVALID_DAY;
    }
    return null;
  }

  /**
   * The exception for a validation error.
   *
   * @param error the error
   * @param year the year that was tried
   * @param month the month that was tried
   * @param day the day that was tried
   * @return the exception, with the month length set for an invalid day
   */
  static NepDateException errorOf(NepDateError error, int year, int month, int day) {
    return switch (error) {
      case OUT_OF_RANGE ->
          new NepDateException(
              error,
              0,
              "year "
                  + year
                  + " is outside "
                  + CalendarTable.MIN_YEAR
                  + ".."
                  + CalendarTable.MAX_YEAR);
      case INVALID_MONTH -> new NepDateException(error, 0, "month " + month + " is outside 1..12");
      default -> {
        int length = Calendar.monthLength(year, month);
        yield new NepDateException(
            error,
            length,
            "day " + day + " is outside 1.." + length + " for " + year + "-" + month);
      }
    };
  }

  private NepDateException daysOutOfRange(long days) {
    return outOfRange(days + " days");
  }

  private NepDateException monthsOutOfRange(long months) {
    return outOfRange(months + " months");
  }

  private NepDateException outOfRange(String amount) {
    return new NepDateException(
        NepDateError.OUT_OF_RANGE, 0, this + " plus " + amount + " is outside " + MIN + ".." + MAX);
  }

  /**
   * Replaces the value with its canonical-text proxy before it is written.
   *
   * @return the proxy that holds {@link #toIsoString()}
   */
  private Object writeReplace() {
    return new SerializationProxy(toIsoString());
  }

  /**
   * Refuses a stream that names the class directly, so no uninitialized date can be read.
   *
   * @param stream the stream being read
   * @throws InvalidObjectException always, because the proxy is the only serialized form
   */
  private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("NepaliDate is read through its serialization proxy");
  }

  /** Holds the canonical text in the stream, so a serialized date is portable and readable. */
  private static final class SerializationProxy implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final String text;

    SerializationProxy(String text) {
      this.text = text;
    }

    private Object readResolve() {
      return NepaliDate.parse(text);
    }
  }
}
