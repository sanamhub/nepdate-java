package io.github.sanamhub.nepdate.java.patro;

import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.NepaliDateRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The patro queries of PATRO section 2, as functions that take a core date.
 *
 * <p>The tables are compiled into this artifact; JSON is never read at runtime. The answer for
 * every covered day is built once, on first use, and {@link #info(NepaliDate)} returns the shared
 * instance for that day: a lookup is one subtraction and one array read, and it allocates nothing.
 * The core artifact holds none of the strings.
 */
public final class Patro {
  private Patro() {}

  /** The answer for a day without data, shared by every such day. */
  private static final CalendarInfo EMPTY = new CalendarInfo(null, false, List.of());

  /** The name pool pairs, built once. */
  private static final Named[] NAMES = buildNames();

  /** One shared answer per day from {@code FIRST_SERIAL} to {@code LAST_SERIAL}. */
  private static final CalendarInfo[] BY_DAY = buildByDay();

  /**
   * Everything the patro knows about a day.
   *
   * <p>It never fails and never allocates: a day outside the coverage returns one shared empty
   * {@link CalendarInfo}, never null.
   *
   * @param date the day
   * @return the day's tithi, holiday flag and events
   */
  public static CalendarInfo info(NepaliDate date) {
    int offset = serialOf(date) - PatroTable.FIRST_SERIAL;
    return offset >= 0 && offset < BY_DAY.length ? BY_DAY[offset] : EMPTY;
  }

  /**
   * The smallest inclusive range containing every day with any metadata.
   *
   * <p>It is BS 2077-01-01 to 2084-08-20 for the current data. A caller uses it to decide whether
   * to say "no data for this year".
   *
   * @return the coverage range
   */
  public static NepaliDateRange coverage() {
    return NepaliDateRange.of(
        NepaliDate.MIN.plusDays(PatroTable.FIRST_SERIAL),
        NepaliDate.MIN.plusDays(PatroTable.LAST_SERIAL));
  }

  /**
   * Every public holiday in date order.
   *
   * @return the holiday dates, 294 of them for the current data
   */
  public static List<NepaliDate> publicHolidays() {
    return Holder.PUBLIC_HOLIDAYS;
  }

  private static CalendarInfo[] buildByDay() {
    CalendarInfo[] byDay = new CalendarInfo[PatroTable.LAST_SERIAL - PatroTable.FIRST_SERIAL + 1];
    for (int offset = 0; offset < byDay.length; offset++) {
      int serial = PatroTable.FIRST_SERIAL + offset;
      byDay[offset] =
          new CalendarInfo(tithiOf(serial, NAMES), isHoliday(serial), eventsOf(serial, NAMES));
    }
    return byDay;
  }

  private static Named tithiOf(int serial, Named[] names) {
    int offset = serial - PatroTable.TITHI_FIRST_SERIAL;
    if (offset < 0 || offset >= PatroTable.TITHI_DAYS) {
      return null;
    }
    return names[PatroTable.TITHI_INDEX[offset]];
  }

  private static boolean isHoliday(int serial) {
    return Arrays.binarySearch(PatroTable.HOLIDAY_SERIALS, serial) >= 0;
  }

  private static List<Named> eventsOf(int serial, Named[] names) {
    int found = Arrays.binarySearch(PatroTable.EVENT_DAY_SERIALS, serial);
    if (found < 0) {
      return List.of();
    }
    int from = PatroTable.EVENT_START[found];
    int to = PatroTable.EVENT_START[found + 1];
    List<Named> events = new ArrayList<>(to - from);
    for (int i = from; i < to; i++) {
      events.add(names[PatroTable.EVENT_INDEX[i]]);
    }
    return events;
  }

  private static int serialOf(NepaliDate date) {
    return (int) date.daysSince(NepaliDate.MIN);
  }

  private static Named[] buildNames() {
    String[] pool = PatroTable.NAMES_POOL;
    Named[] names = new Named[PatroTable.NAMES];
    for (int i = 0; i < names.length; i++) {
      names[i] = new Named(pool[2 * i], pool[2 * i + 1]);
    }
    return names;
  }

  /** Holds the holiday list so it is built once, on first use. */
  private static final class Holder {
    private static final List<NepaliDate> PUBLIC_HOLIDAYS = build();

    private static List<NepaliDate> build() {
      List<NepaliDate> holidays = new ArrayList<>(PatroTable.HOLIDAYS);
      for (int serial : PatroTable.HOLIDAY_SERIALS) {
        holidays.add(NepaliDate.MIN.plusDays(serial));
      }
      return List.copyOf(holidays);
    }
  }
}
