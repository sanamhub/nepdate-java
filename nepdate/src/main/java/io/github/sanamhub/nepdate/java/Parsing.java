package io.github.sanamhub.nepdate.java;

import java.util.Optional;

/**
 * The strict parser (PARSING section 1).
 *
 * <p>The scanner packs three fields into a {@code long}: year at bits 28 and up, month at bits 14
 * to 27, day at 0 to 13. A negative value is {@code -(error.ordinal() + 1)}. Nothing is allocated
 * until the date or the exception is built.
 */
final class Parsing {
  private static final int YEAR_SHIFT = 28;
  private static final int MONTH_SHIFT = 14;
  private static final int FIELD_MASK = 0x3FFF;
  private static final int MAX_DIGITS = 4;

  private Parsing() {}

  /**
   * The digit value of an ASCII or Devanagari digit.
   *
   * @param c the character
   * @return 0 to 9, or -1 for anything else
   */
  static int digitValue(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    }
    if (c >= '\u0966' && c <= '\u096F') {
      return c - '\u0966';
    }
    return -1;
  }

  /**
   * Whether a character separates the three groups.
   *
   * @param c the character
   * @return true for {@code - / . _ \ space । |}
   */
  static boolean isSeparator(char c) {
    return c == '-'
        || c == '/'
        || c == '.'
        || c == '_'
        || c == '\\'
        || c == ' '
        || c == '\u0964'
        || c == '|';
  }

  static long scanned(int year, int month, int day) {
    return ((long) year << YEAR_SHIFT) | ((long) month << MONTH_SHIFT) | day;
  }

  static int scannedYear(long value) {
    return (int) (value >>> YEAR_SHIFT);
  }

  static int scannedMonth(long value) {
    return (int) ((value >>> MONTH_SHIFT) & FIELD_MASK);
  }

  static int scannedDay(long value) {
    return (int) (value & FIELD_MASK);
  }

  static long scanError(NepDateError error) {
    return -(error.ordinal() + 1L);
  }

  static NepDateError scanErrorOf(long value) {
    return NepDateError.values()[(int) (-value - 1)];
  }

  /**
   * Reads three groups of digits (PARSING section 1 steps 1 to 3).
   *
   * @param text the input
   * @return the packed fields, or a negative error code
   */
  static long scanStrict(CharSequence text) {
    int groups = 0;
    int current = 0;
    int length = 0;
    int year = 0;
    int month = 0;
    int day = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      int digit = digitValue(c);
      if (digit >= 0) {
        if (length == MAX_DIGITS) {
          return scanError(NepDateError.NUMBER_TOO_LONG);
        }
        current = current * 10 + digit;
        length++;
      } else if (isSeparator(c)) {
        if (length > 0) {
          if (groups == 0) {
            year = current;
          } else if (groups == 1) {
            month = current;
          } else if (groups == 2) {
            day = current;
          }
          groups++;
          current = 0;
          length = 0;
        }
      } else {
        return scanError(NepDateError.INVALID_CHARACTER);
      }
      // A digit after three complete groups starts a fourth.
      if (groups == 3 && length > 0) {
        return scanError(NepDateError.WRONG_GROUP_COUNT);
      }
    }
    if (groups == 2 && length > 0) {
      return scanned(year, month, current);
    }
    if (groups == 3) {
      return scanned(year, month, day);
    }
    return scanError(NepDateError.WRONG_GROUP_COUNT);
  }

  /**
   * Reads three groups and validates them.
   *
   * @param text the input
   * @return the packed date, or a negative error code
   */
  static long scanStrictValid(CharSequence text) {
    long value = scanStrict(text);
    if (value < 0) {
      return value;
    }
    NepDateError error =
        NepaliDate.validationError(scannedYear(value), scannedMonth(value), scannedDay(value));
    return error == null ? value : scanError(error);
  }

  /**
   * Parses strictly (PARSING section 1).
   *
   * @param text the input
   * @return the date
   * @throws NepDateException with a parse error or a validation error
   */
  static NepaliDate parse(CharSequence text) {
    long value = scanStrict(text);
    if (value < 0) {
      throw parseException(text, scanErrorOf(value));
    }
    return NepaliDate.of(scannedYear(value), scannedMonth(value), scannedDay(value));
  }

  /**
   * Parses strictly without throwing.
   *
   * @param text the input
   * @return the date, or empty
   */
  static Optional<NepaliDate> tryParse(CharSequence text) {
    long value = scanStrictValid(text);
    if (value < 0) {
      return Optional.empty();
    }
    return Optional.of(NepaliDate.of(scannedYear(value), scannedMonth(value), scannedDay(value)));
  }

  static NepDateException parseException(CharSequence text, NepDateError error) {
    return new NepDateException(error, 0, "\"" + text + "\" is not a date: " + error);
  }
}
