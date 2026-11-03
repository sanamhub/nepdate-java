package io.github.sanamhub.nepdate.java;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An inclusive range of BS days, from a first day to a last day.
 *
 * <p>A range is never empty: {@link #of(NepaliDate, NepaliDate)} refuses a start after the end, and
 * {@link #intersect(NepaliDateRange)} returns an empty {@link Optional} when two ranges do not
 * overlap. The class holds only its two bounds, so a range of years costs the same as a range of
 * one day, and it is immutable.
 *
 * <p>Iterating yields every day in order. The day helpers never throw because a range touches the
 * end of the supported data: a range that reaches 2199-12-30 still returns all its days, and {@link
 * #isAdjacentTo(NepaliDateRange)} answers false rather than looking one day past the end.
 */
public final class NepaliDateRange implements Iterable<NepaliDate> {
  private static final int ALL = 0;
  private static final int WORKING = 1;
  private static final int WEEKEND = 2;

  private final int first;
  private final int last;

  private NepaliDateRange(int first, int last) {
    this.first = first;
    this.last = last;
  }

  /**
   * The days from {@code start} to {@code endInclusive}, both included.
   *
   * @param start the first day
   * @param endInclusive the last day
   * @return the range
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when {@code start} is after
   *     {@code endInclusive}
   */
  public static NepaliDateRange of(NepaliDate start, NepaliDate endInclusive) {
    if (start.compareTo(endInclusive) > 0) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE,
          0,
          "range start " + start + " is after its end " + endInclusive);
    }
    return new NepaliDateRange(start.serial(), endInclusive.serial());
  }

  /**
   * The range of one day, as the C# {@code SingleDay}.
   *
   * @param date the day
   * @return the range from {@code date} to {@code date}
   */
  public static NepaliDateRange singleDay(NepaliDate date) {
    return new NepaliDateRange(date.serial(), date.serial());
  }

  /**
   * The range of {@code days} days starting at {@code start}, as the C# {@code FromDayCount}.
   *
   * @param start the first day
   * @param days the number of days, at least 1
   * @return the range from {@code start} to its {@code days - 1}th following day
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when {@code days} is below 1 or
   *     the last day is outside the supported span
   */
  public static NepaliDateRange fromDayCount(NepaliDate start, int days) {
    if (days < 1) {
      throw new NepDateException(NepDateError.OUT_OF_RANGE, 0, days + " days is below 1");
    }
    long lastSerial = (long) start.serial() + days - 1;
    if (lastSerial > CalendarTable.MAX_SERIAL) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE,
          0,
          days + " days from " + start + " ends after " + NepaliDate.MAX);
    }
    return new NepaliDateRange(start.serial(), (int) lastSerial);
  }

  /**
   * Every day of a BS month, as the C# {@code ForMonth}.
   *
   * @param year the BS year
   * @param month the month number, 1 to 12
   * @return the range of the month
   * @throws NepDateException as {@link NepaliDate#of(int, int, int)}
   */
  public static NepaliDateRange forMonth(int year, int month) {
    NepaliDate firstDay = NepaliDate.of(year, month, 1);
    int index = (year - CalendarTable.MIN_YEAR) * 12 + month - 1;
    return new NepaliDateRange(firstDay.serial(), CalendarTable.MONTH_START[index + 1] - 1);
  }

  /**
   * Every day of a BS year, as the C# {@code ForCalendarYear}.
   *
   * @param year the BS year, 1901 to 2199
   * @return the range of the year, 364 to 367 days
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} outside 1901 to 2199
   */
  public static NepaliDateRange forYear(int year) {
    if (year < CalendarTable.MIN_YEAR || year > CalendarTable.MAX_YEAR) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE,
          0,
          "year " + year + " is outside " + CalendarTable.MIN_YEAR + ".." + CalendarTable.MAX_YEAR);
    }
    int index = (year - CalendarTable.MIN_YEAR) * 12;
    return new NepaliDateRange(
        CalendarTable.MONTH_START[index], CalendarTable.MONTH_START[index + 12] - 1);
  }

  /**
   * Every day of a fiscal year, as the C# {@code ForFiscalYear}.
   *
   * @param fiscalYear the BS year the fiscal year starts in
   * @return the range from 1 Shrawan to the last day of Ashad of the next year
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when an end is outside the
   *     supported span
   */
  public static NepaliDateRange forFiscalYear(int fiscalYear) {
    return FiscalYear.of(fiscalYear).range();
  }

  /**
   * Every day of the BS month a zone is in now, as the C# {@code CurrentMonth}.
   *
   * @param zone the zone whose current date is used
   * @return the current month's range
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public static NepaliDateRange currentMonth(ZoneId zone) {
    NepaliDate today = NepaliDate.today(zone);
    return forMonth(today.year(), today.monthNumber());
  }

  /**
   * Every day of the BS month the clock's zone is in now.
   *
   * @param clock the clock to read
   * @return the current month's range
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public static NepaliDateRange currentMonth(Clock clock) {
    NepaliDate today = NepaliDate.today(clock);
    return forMonth(today.year(), today.monthNumber());
  }

  /**
   * Every day of the fiscal year a zone is in now, as the C# {@code CurrentFiscalYear}.
   *
   * @param zone the zone whose current date is used
   * @return the current fiscal year's range
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when an end is outside the
   *     supported span
   */
  public static NepaliDateRange currentFiscalYear(ZoneId zone) {
    return NepaliDate.today(zone).fiscalYear().range();
  }

  /**
   * Every day of the fiscal year the clock's zone is in now.
   *
   * @param clock the clock to read
   * @return the current fiscal year's range
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when an end is outside the
   *     supported span
   */
  public static NepaliDateRange currentFiscalYear(Clock clock) {
    return NepaliDate.today(clock).fiscalYear().range();
  }

  /**
   * Every day of the BS year a zone is in now, as the C# {@code CurrentCalendarYear}.
   *
   * @param zone the zone whose current date is used
   * @return the current BS year's range
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public static NepaliDateRange currentYear(ZoneId zone) {
    return forYear(NepaliDate.today(zone).year());
  }

  /**
   * Every day of the BS year the clock's zone is in now.
   *
   * @param clock the clock to read
   * @return the current BS year's range
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when today is outside the
   *     supported span
   */
  public static NepaliDateRange currentYear(Clock clock) {
    return forYear(NepaliDate.today(clock).year());
  }

  /**
   * The first day, as the C# {@code Start}.
   *
   * @return the first day
   */
  public NepaliDate start() {
    return NepaliDate.ofSerial(first);
  }

  /**
   * The last day, included, as the C# {@code End}.
   *
   * @return the last day
   */
  public NepaliDate end() {
    return NepaliDate.ofSerial(last);
  }

  /**
   * The number of days, first to last included, as the C# {@code Length}.
   *
   * @return the number of days, at least 1
   */
  public int length() {
    return last - first + 1;
  }

  /**
   * Whether a day is in this range, as the C# {@code Contains(NepaliDate)}.
   *
   * @param date the day
   * @return true when it is between the bounds, both included
   */
  public boolean contains(NepaliDate date) {
    return date.serial() >= first && date.serial() <= last;
  }

  /**
   * Whether another range lies entirely inside this one, as the C# {@code
   * Contains(NepaliDateRange)}.
   *
   * @param other the other range
   * @return true when every day of {@code other} is also a day of this range
   */
  public boolean contains(NepaliDateRange other) {
    return other.first >= first && other.last <= last;
  }

  /**
   * Whether the two ranges share at least one day, as the C# {@code Overlaps}.
   *
   * @param other the other range
   * @return true when the bounds overlap
   */
  public boolean overlaps(NepaliDateRange other) {
    return first <= other.last && other.first <= last;
  }

  /**
   * Whether one range starts the day after the other ends, as the C# {@code IsAdjacentTo}.
   *
   * <p>A range that ends at 2199-12-30 has no following day inside the supported span, so this
   * answers false instead of failing, and likewise at 1901-01-01 in the other direction.
   *
   * @param other the other range
   * @return true when the two ranges touch without overlapping
   */
  public boolean isAdjacentTo(NepaliDateRange other) {
    return last + 1 == other.first || other.last + 1 == first;
  }

  /**
   * The days both ranges have, as the C# {@code Intersect}.
   *
   * @param other the other range
   * @return the overlapping range, or an empty {@link Optional} when they do not overlap
   */
  public Optional<NepaliDateRange> intersect(NepaliDateRange other) {
    int low = Math.max(first, other.first);
    int high = Math.min(last, other.last);
    return low <= high ? Optional.of(new NepaliDateRange(low, high)) : Optional.empty();
  }

  /**
   * The smallest range covering both, as the C# {@code Union}.
   *
   * @param other the other range
   * @return the covering range, which may be this range or {@code other}
   */
  public NepaliDateRange union(NepaliDateRange other) {
    return new NepaliDateRange(Math.min(first, other.first), Math.max(last, other.last));
  }

  /**
   * This range with another removed, as the C# {@code Except}.
   *
   * @param other the range to remove
   * @return an empty list when {@code other} covers this range, otherwise the one or two remaining
   *     ranges in date order
   */
  public List<NepaliDateRange> except(NepaliDateRange other) {
    if (!overlaps(other)) {
      return List.of(this);
    }
    NepaliDateRange left = other.first > first ? new NepaliDateRange(first, other.first - 1) : null;
    NepaliDateRange right = other.last < last ? new NepaliDateRange(other.last + 1, last) : null;
    if (left == null) {
      return right == null ? List.of() : List.of(right);
    }
    return right == null ? List.of(left) : List.of(left, right);
  }

  /**
   * One range per BS month this range touches, as the C# {@code SplitByMonth}.
   *
   * <p>The first and last pieces are clipped to the range ends. The last piece ends at {@link
   * #end()} even at 2199-12-30, where the C# method throws.
   *
   * @return the pieces in date order, 1 to 24 of them
   */
  public List<NepaliDateRange> splitByMonth() {
    List<NepaliDateRange> pieces = new ArrayList<>();
    int day = first;
    while (day <= last) {
      NepaliDate date = NepaliDate.ofSerial(day);
      int monthStart = Calendar.toSerial(date.year(), date.monthNumber(), 1);
      int pieceEnd = Math.min(last, monthStart + date.monthLength() - 1);
      pieces.add(new NepaliDateRange(day, pieceEnd));
      day = pieceEnd + 1;
    }
    return List.copyOf(pieces);
  }

  /**
   * One range per fiscal quarter this range touches, as the C# {@code SplitByFiscalQuarter}.
   *
   * <p>The first and last pieces are clipped to the range ends. The last piece ends at {@link
   * #end()} even at 2199-12-30, where the C# method throws.
   *
   * @return the pieces in date order, 1 to 8 of them
   */
  public List<NepaliDateRange> splitByFiscalQuarter() {
    List<NepaliDateRange> pieces = new ArrayList<>();
    int day = first;
    while (day <= last) {
      int pieceEnd = Math.min(last, quarterEndSerial(NepaliDate.ofSerial(day)));
      pieces.add(new NepaliDateRange(day, pieceEnd));
      day = pieceEnd + 1;
    }
    return List.copyOf(pieces);
  }

  private static int quarterEndSerial(NepaliDate date) {
    Quarter quarter = date.quarter();
    int year =
        quarter == Quarter.Q4 ? date.fiscalYear().startYear() + 1 : date.fiscalYear().startYear();
    int firstMonth = quarter.months().get(0).number();
    // The quarter's three months are consecutive, so its end is where the month after them starts.
    int index = (year - CalendarTable.MIN_YEAR) * 12 + firstMonth - 1 + 3;
    return CalendarTable.MONTH_START[index] - 1;
  }

  /**
   * Every {@code interval}-th day from {@link #start()}, as the C# {@code DatesWithInterval}.
   *
   * @param interval the number of days between two results, at least 1; every positive {@code int}
   *     is accepted, including {@link Integer#MAX_VALUE}
   * @return the days in date order, ending at or before {@link #end()}
   * @throws NepDateException with {@link NepDateError#OUT_OF_RANGE} when {@code interval} is below
   *     1
   */
  public Stream<NepaliDate> datesWithInterval(int interval) {
    if (interval < 1) {
      throw new NepDateException(
          NepDateError.OUT_OF_RANGE, 0, "interval " + interval + " is below 1");
    }
    return stream(new Cursor(first, last, interval, ALL, false));
  }

  /**
   * Every day except Saturday, as the C# {@code WorkingDays}.
   *
   * @param excludeSunday also skip Sunday
   * @return the working days in date order
   */
  public Stream<NepaliDate> workingDays(boolean excludeSunday) {
    return stream(new Cursor(first, last, 1, WORKING, excludeSunday));
  }

  /**
   * Every Saturday, as the C# {@code WeekendDays}.
   *
   * @param includeSunday also include Sunday
   * @return the weekend days in date order
   */
  public Stream<NepaliDate> weekendDays(boolean includeSunday) {
    return stream(new Cursor(first, last, 1, WEEKEND, includeSunday));
  }

  /**
   * Every day from {@link #start()} to {@link #end()}, as a stream.
   *
   * @return the days in date order
   */
  public Stream<NepaliDate> stream() {
    return stream(new Cursor(first, last, 1, ALL, false));
  }

  /**
   * Every day from {@link #start()} to {@link #end()}, in order.
   *
   * @return an iterator over the days
   */
  @Override
  public Iterator<NepaliDate> iterator() {
    return new Cursor(first, last, 1, ALL, false);
  }

  private static Stream<NepaliDate> stream(Iterator<NepaliDate> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
            iterator, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.DISTINCT),
        false);
  }

  /**
   * The two bounds in the default display, as the C# {@code ToString()}.
   *
   * @return for example {@code 2081/04/10 - 2081/04/20}
   */
  @Override
  public String toString() {
    return toShortString(DateOrder.YMD, Separator.SLASH);
  }

  /**
   * The two bounds in an order and with a separator, as the C# {@code ToString(DateFormats,
   * Separators)}.
   *
   * @param order the field order
   * @param separator the separator character
   * @return for example {@code 2081-04-10 - 2081-04-20}
   */
  public String toShortString(DateOrder order, Separator separator) {
    return toShortString(order, separator, Language.ENGLISH);
  }

  /**
   * The two bounds in an order, a separator and a language.
   *
   * @param order the field order
   * @param separator the separator character
   * @param language English or Nepali digits
   * @return for example {@code १०/०४/२०८१ - २०/०४/२०८१}
   */
  public String toShortString(DateOrder order, Separator separator, Language language) {
    StringBuilder out = new StringBuilder(2 * Text.SHORT_CAPACITY + 3);
    Text.appendShort(out, start(), order, separator, true, language);
    out.append(" - ");
    return Text.appendShort(out, end(), order, separator, true, language).toString();
  }

  /**
   * Whether both ranges have the same bounds.
   *
   * @param other the other value
   * @return true for the same two bounds
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof NepaliDateRange range && range.first == first && range.last == last;
  }

  /**
   * A hash code from the two bounds.
   *
   * @return the hash of the bounds
   */
  @Override
  public int hashCode() {
    return 31 * first + last;
  }

  /** The days of a range, in order, skipping the ones a filter rejects. */
  private static final class Cursor implements Iterator<NepaliDate> {
    private final int last;
    private final int step;
    private final int mode;
    private final boolean flag;
    private long next;

    Cursor(int first, int last, int step, int mode, boolean flag) {
      this.last = last;
      this.step = step;
      this.mode = mode;
      this.flag = flag;
      this.next = first;
      advance();
    }

    private void advance() {
      while (next <= last && !accept((int) next)) {
        next += step;
      }
    }

    private boolean accept(int serial) {
      int weekday = Calendar.weekdayOf(serial);
      return switch (mode) {
        case WORKING -> weekday != 6 && !(flag && weekday == 0);
        case WEEKEND -> weekday == 6 || (flag && weekday == 0);
        default -> true;
      };
    }

    @Override
    public boolean hasNext() {
      return next <= last;
    }

    @Override
    public NepaliDate next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      NepaliDate date = NepaliDate.ofSerial((int) next);
      next += step;
      advance();
      return date;
    }
  }
}
