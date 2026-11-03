package io.github.sanamhub.nepdate.java.codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** The month lengths and serial arithmetic the generator needs, read from the hub's data. */
final class CalendarData {
  static final int MIN_YEAR = 1901;
  static final int MAX_YEAR = 2199;
  static final int YEARS = MAX_YEAR - MIN_YEAR + 1;

  private final int[][] lengths = new int[YEARS][12];

  private CalendarData() {}

  static CalendarData read(Path calendarFile) throws IOException {
    Map<String, Object> root =
        object(Json.parse(Files.readString(calendarFile, StandardCharsets.UTF_8)));
    require(longValue(root, "schema") == 1L, calendarFile + ": schema is not 1");
    require(intValue(root, "min_year") == MIN_YEAR, calendarFile + ": min_year is not " + MIN_YEAR);
    require(intValue(root, "max_year") == MAX_YEAR, calendarFile + ": max_year is not " + MAX_YEAR);

    Map<String, Object> epoch = object(root.get("epoch"));
    require("1901-01-01".equals(epoch.get("bs")), calendarFile + ": epoch.bs is not 1901-01-01");
    require("1844-04-11".equals(epoch.get("ad")), calendarFile + ": epoch.ad is not 1844-04-11");

    Map<String, Object> months = object(root.get("month_lengths"));
    CalendarData data = new CalendarData();
    for (int year = MIN_YEAR; year <= MAX_YEAR; year++) {
      Object value = months.get(Integer.toString(year));
      require(value != null, calendarFile + ": no month lengths for " + year);
      List<Object> row = array(value);
      require(row.size() == 12, calendarFile + ": " + year + " has " + row.size() + " months");
      for (int month = 1; month <= 12; month++) {
        int length = number(row.get(month - 1)).intValue();
        require(
            length >= 29 && length <= 32,
            calendarFile + ": " + year + "-" + month + " length " + length + " is outside 29..32");
        data.lengths[year - MIN_YEAR][month - 1] = length;
      }
    }
    require(
        months.size() == YEARS, calendarFile + ": " + months.size() + " years, expected " + YEARS);
    return data;
  }

  int monthLength(int year, int month) {
    return lengths[year - MIN_YEAR][month - 1];
  }

  int yearLength(int year) {
    int sum = 0;
    for (int month = 1; month <= 12; month++) {
      sum += monthLength(year, month);
    }
    return sum;
  }

  int totalDays() {
    int sum = 0;
    for (int year = MIN_YEAR; year <= MAX_YEAR; year++) {
      sum += yearLength(year);
    }
    return sum;
  }

  /** The serial of a BS date the caller has already validated. */
  int serialOf(int year, int month, int day) {
    int serial = 0;
    for (int y = MIN_YEAR; y < year; y++) {
      serial += yearLength(y);
    }
    for (int m = 1; m < month; m++) {
      serial += monthLength(year, m);
    }
    return serial + day - 1;
  }

  boolean valid(int year, int month, int day) {
    return year >= MIN_YEAR
        && year <= MAX_YEAR
        && month >= 1
        && month <= 12
        && day >= 1
        && day <= monthLength(year, month);
  }

  /** Days from 1970-01-01 to a real proleptic Gregorian date (Howard Hinnant). */
  static int daysFromCivil(int year, int month, int day) {
    int y = month <= 2 ? year - 1 : year;
    int era = y / 400;
    int yearOfEra = y - era * 400;
    int marchMonth = (month + 9) % 12;
    int dayOfYear = (153 * marchMonth + 2) / 5 + day - 1;
    int dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
    return era * 146097 + dayOfEra - 719468;
  }

  static int weekdayOf(long unixDays) {
    return (int) Math.floorMod(unixDays + 4, 7L);
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> object(Object value) {
    require(value instanceof Map<?, ?>, "expected a JSON object, found " + describe(value));
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  static List<Object> array(Object value) {
    require(value instanceof List<?>, "expected a JSON array, found " + describe(value));
    return (List<Object>) value;
  }

  static Number number(Object value) {
    require(value instanceof Number, "expected a number, found " + describe(value));
    return (Number) value;
  }

  static long longValue(Map<String, Object> map, String key) {
    require(map.containsKey(key), "missing key " + key);
    return number(map.get(key)).longValue();
  }

  static int intValue(Map<String, Object> map, String key) {
    return Math.toIntExact(longValue(map, key));
  }

  static String text(Map<String, Object> map, String key) {
    Object value = map.get(key);
    require(value instanceof String, "missing string " + key);
    return (String) value;
  }

  private static String describe(Object value) {
    return value == null ? "null" : value.getClass().getSimpleName();
  }

  static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("codegen: " + message);
    }
  }
}
