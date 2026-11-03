package io.github.sanamhub.nepdate.java;

/**
 * Serial-day arithmetic (ALGORITHM sections 2 to 8) on the tables generated from the shared data.
 *
 * <p>A serial is the number of days since BS 1901-01-01: 0 for that day and 109211 for 2199-12-30.
 * A BS date is packed into one {@code int} as {@code year << 10 | month << 6 | day}, which leaves
 * six bits for the day, enough for 32. The class is package-private because a serial is an
 * implementation detail; {@link NepaliDate} is the value type.
 */
final class Calendar {
  private Calendar() {}

  /**
   * Whether a year is a leap year in the proleptic Gregorian calendar.
   *
   * @param year the Gregorian year, which may be negative in principle
   * @return true for 29 February
   */
  static boolean isGregorianLeap(long year) {
    return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
  }

  /**
   * The length of a BS month.
   *
   * @param year 1901 to 2199
   * @param month 1 to 12
   * @return 29 to 32
   */
  static int monthLength(int year, int month) {
    int index = (year - CalendarTable.MIN_YEAR) * 12 + month - 1;
    return CalendarTable.MONTH_START[index + 1] - CalendarTable.MONTH_START[index];
  }

  /**
   * The length of a BS year, the sum of its twelve months.
   *
   * @param year 1901 to 2199
   * @return 364 to 367
   */
  static int yearLength(int year) {
    int index = (year - CalendarTable.MIN_YEAR) * 12;
    return CalendarTable.MONTH_START[index + 12] - CalendarTable.MONTH_START[index];
  }

  /**
   * The serial of a valid BS date.
   *
   * @param year 1901 to 2199
   * @param month 1 to 12
   * @param day 1 to the month length
   * @return the serial
   */
  static int toSerial(int year, int month, int day) {
    return CalendarTable.MONTH_START[(year - CalendarTable.MIN_YEAR) * 12 + month - 1] + day - 1;
  }

  /**
   * The BS date of a valid serial, packed.
   *
   * <p>The estimate {@code ((serial - 25) * 2153) >> 16} is at most one month low, and the compare
   * that follows fixes it; the exhaustive test walks all 109212 serials.
   *
   * @param serial 0 to 109211
   * @return the packed year, month and day
   */
  static int fromSerial(int serial) {
    int monthIndex = ((serial - 25) * 2153) >> 16;
    if (serial >= CalendarTable.MONTH_START[monthIndex + 1]) {
      monthIndex++;
    }
    int year = CalendarTable.MIN_YEAR + monthIndex / 12;
    int month = monthIndex % 12 + 1;
    return pack(year, month, serial - CalendarTable.MONTH_START[monthIndex] + 1);
  }

  /**
   * The weekday of a serial.
   *
   * @param serial 0 to 109211
   * @return 0 for Sunday to 6 for Saturday
   */
  static int weekdayOf(int serial) {
    return (serial + CalendarTable.EPOCH_WEEKDAY) % 7;
  }

  /**
   * The serial of a BS date, packed.
   *
   * @param year 1901 to 2199
   * @param month 1 to 12
   * @param day 1 to the month length
   * @return the packed date
   */
  static int pack(int year, int month, int day) {
    return (year << 10) | (month << 6) | day;
  }

  /**
   * The year of a packed date.
   *
   * @param packed the packed date
   * @return the year
   */
  static int yearOf(int packed) {
    return packed >>> 10;
  }

  /**
   * The month of a packed date.
   *
   * @param packed the packed date
   * @return the month number
   */
  static int monthOf(int packed) {
    return (packed >>> 6) & 0xF;
  }

  /**
   * The day of a packed date.
   *
   * @param packed the packed date
   * @return the day of the month
   */
  static int dayOf(int packed) {
    return packed & 0x3F;
  }

  /**
   * The days from 1970-01-01 to a real proleptic Gregorian date (Howard Hinnant, days_from_civil).
   *
   * @param year 1 or more
   * @param month 1 to 12
   * @param day 1 to the Gregorian month length
   * @return the epoch day
   */
  static int daysFromCivil(int year, int month, int day) {
    int y = month <= 2 ? year - 1 : year;
    int era = y / 400;
    int yearOfEra = y - era * 400;
    int marchMonth = (month + 9) % 12;
    int dayOfYear = (153 * marchMonth + 2) / 5 + day - 1;
    int dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
    return era * 146097 + dayOfEra - 719468;
  }

  /**
   * The real Gregorian date of an epoch day, packed like a BS date (Hinnant, civil_from_days).
   *
   * @param epochDay days since 1970-01-01, within the supported range
   * @return the packed Gregorian year, month and day
   */
  static int packedCivilFromDays(int epochDay) {
    int shifted = epochDay + 719468;
    int era = shifted / 146097;
    int dayOfEra = shifted - era * 146097;
    int yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365;
    int year = yearOfEra + era * 400;
    int dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100);
    int marchMonth = (5 * dayOfYear + 2) / 153;
    int day = dayOfYear - (153 * marchMonth + 2) / 5 + 1;
    int month = marchMonth < 10 ? marchMonth + 3 : marchMonth - 9;
    if (month <= 2) {
      year++;
    }
    return pack(year, month, day);
  }

  /**
   * The serial of a real Gregorian date.
   *
   * @param year 1844 to 2143
   * @param month 1 to 12
   * @param day 1 to the Gregorian month length
   * @return the serial
   */
  static int serialOfCivil(int year, int month, int day) {
    return daysFromCivil(year, month, day) - CalendarTable.EPOCH_UNIX_DAYS;
  }

  /**
   * The packed Gregorian date of a serial.
   *
   * @param serial 0 to 109211
   * @return the packed Gregorian year, month and day
   */
  static int packedCivilOf(int serial) {
    return packedCivilFromDays(serial + CalendarTable.EPOCH_UNIX_DAYS);
  }

  /**
   * The number of days in a Gregorian month.
   *
   * @param year the Gregorian year
   * @param month 1 to 12
   * @return 28 to 31
   */
  static int gregorianMonthLength(int year, int month) {
    return switch (month) {
      case 2 -> isGregorianLeap(year) ? 29 : 28;
      case 4, 6, 9, 11 -> 30;
      default -> 31;
    };
  }
}
