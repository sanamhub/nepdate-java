package io.github.sanamhub.nepdate.java;

/**
 * The formatters of FORMATTING sections 2 to 5.
 *
 * <p>Each format appends to a {@link StringBuilder}; the {@link String} forms in {@link NepaliDate}
 * wrap one. Nothing here allocates a formatter object.
 */
final class Text {
  /** The longest default display, {@code 2080/05/15}. */
  static final int SHORT_CAPACITY = 10;

  /** The longest long form, {@code Wednesday, Baishakh 15, 2080} plus room for Devanagari. */
  static final int LONG_CAPACITY = 32;

  private static final char DEVANAGARI_ZERO = '\u0966';

  /**
   * Room for the longest long form in either language: the longest weekday and month names, the two
   * {@code ", "}, the space, a two-digit day and a four-digit year.
   */
  private static final int LONG_CHARS = longestWeekday() + longestMonth() + 11;

  private Text() {}

  private static int longestWeekday() {
    int longest = 0;
    for (Weekday weekday : Weekday.values()) {
      longest = Math.max(longest, weekday.name(Language.ENGLISH).length());
      longest = Math.max(longest, weekday.name(Language.NEPALI).length());
    }
    return longest;
  }

  private static int longestMonth() {
    int longest = 0;
    for (Month month : Month.values()) {
      longest = Math.max(longest, month.name(Language.ENGLISH).length());
      longest = Math.max(longest, month.name(Language.NEPALI).length());
    }
    return longest;
  }

  private static char zeroOf(Language language) {
    return language == Language.NEPALI ? DEVANAGARI_ZERO : '0';
  }

  /** Writes a number below 100, as two digits when it has two or {@code pad} asks for them. */
  private static int writeField(char[] buffer, int position, int value, boolean pad, char zero) {
    int at = position;
    if (pad || value >= 10) {
      buffer[at++] = (char) (zero + value / 10);
    }
    buffer[at++] = (char) (zero + value % 10);
    return at;
  }

  /** Writes a four-digit year; every supported year has four digits. */
  private static int writeYear(char[] buffer, int position, int year, char zero) {
    int high = year / 100;
    int low = year % 100;
    buffer[position] = (char) (zero + high / 10);
    buffer[position + 1] = (char) (zero + high % 10);
    buffer[position + 2] = (char) (zero + low / 10);
    buffer[position + 3] = (char) (zero + low % 10);
    return position + 4;
  }

  private static int writeName(char[] buffer, int position, String name) {
    int length = name.length();
    name.getChars(0, length, buffer, position);
    return position + length;
  }

  /**
   * Appends a non-negative number with at least {@code width} digits, zero padded.
   *
   * @param out where to write
   * @param value the number
   * @param width the minimum number of digits
   * @param language the digits to use
   * @return {@code out}
   */
  static StringBuilder appendNumber(StringBuilder out, long value, int width, Language language) {
    int digits = 1;
    long scale = 1;
    while (value / scale >= 10) {
      scale *= 10;
      digits++;
    }
    for (int i = digits; i < width; i++) {
      appendDigit(out, 0, language);
    }
    while (scale > 0) {
      appendDigit(out, (int) ((value / scale) % 10), language);
      scale /= 10;
    }
    return out;
  }

  /**
   * Appends one digit in the given language.
   *
   * @param out where to write
   * @param digit 0 to 9
   * @param language the digit system
   * @return {@code out}
   */
  static StringBuilder appendDigit(StringBuilder out, int digit, Language language) {
    out.append(
        language == Language.NEPALI ? (char) (DEVANAGARI_ZERO + digit) : (char) ('0' + digit));
    return out;
  }

  /**
   * The three fields in an order and with a separator (FORMATTING section 3).
   *
   * @param date the date
   * @param order the field order
   * @param separator the separator character
   * @param pad four-digit year and two-digit month and day
   * @param language the digits to use
   * @return the short form
   */
  static String shortString(
      NepaliDate date, DateOrder order, Separator separator, boolean pad, Language language) {
    // One array filled in place: a StringBuilder starts in Latin-1 and copies itself on the first
    // Devanagari digit, and checks its capacity on every append.
    char[] buffer = new char[SHORT_CAPACITY];
    char zero = zeroOf(language);
    char between = separator.character();
    // The constant's name spells its field order.
    String fields = order.name();
    int length = 0;
    for (int i = 0; i < 3; i++) {
      if (i > 0) {
        buffer[length++] = between;
      }
      char field = fields.charAt(i);
      if (field == 'Y') {
        length = writeYear(buffer, length, date.year(), zero);
      } else if (field == 'M') {
        length = writeField(buffer, length, date.monthNumber(), pad, zero);
      } else {
        length = writeField(buffer, length, date.day(), pad, zero);
      }
    }
    return new String(buffer, 0, length);
  }

  /**
   * Appends the short form.
   *
   * @param out where to write
   * @param date the date
   * @param order the field order
   * @param separator the separator character
   * @param pad four-digit year and two-digit month and day
   * @param language the digits to use
   * @return {@code out}
   */
  static StringBuilder appendShort(
      StringBuilder out,
      NepaliDate date,
      DateOrder order,
      Separator separator,
      boolean pad,
      Language language) {
    int yearWidth = pad ? 4 : 0;
    int fieldWidth = pad ? 2 : 0;
    long year = date.year();
    long month = date.monthNumber();
    long day = date.day();
    char between = separator.character();
    switch (order) {
      case YMD -> {
        appendNumber(out, year, yearWidth, language).append(between);
        appendNumber(out, month, fieldWidth, language).append(between);
        appendNumber(out, day, fieldWidth, language);
      }
      case YDM -> {
        appendNumber(out, year, yearWidth, language).append(between);
        appendNumber(out, day, fieldWidth, language).append(between);
        appendNumber(out, month, fieldWidth, language);
      }
      case MYD -> {
        appendNumber(out, month, fieldWidth, language).append(between);
        appendNumber(out, year, yearWidth, language).append(between);
        appendNumber(out, day, fieldWidth, language);
      }
      case MDY -> {
        appendNumber(out, month, fieldWidth, language).append(between);
        appendNumber(out, day, fieldWidth, language).append(between);
        appendNumber(out, year, yearWidth, language);
      }
      case DYM -> {
        appendNumber(out, day, fieldWidth, language).append(between);
        appendNumber(out, year, yearWidth, language).append(between);
        appendNumber(out, month, fieldWidth, language);
      }
      case DMY -> {
        appendNumber(out, day, fieldWidth, language).append(between);
        appendNumber(out, month, fieldWidth, language).append(between);
        appendNumber(out, year, yearWidth, language);
      }
    }
    return out;
  }

  /**
   * The long form (FORMATTING section 4).
   *
   * @param date the date
   * @param options which parts to write
   * @param language the names and digits to use
   * @return the long form
   */
  static String longString(NepaliDate date, LongDateOptions options, Language language) {
    char[] buffer = new char[LONG_CHARS];
    char zero = zeroOf(language);
    int length = 0;
    if (options.weekday()) {
      length = writeName(buffer, length, date.weekday().name(language));
      buffer[length++] = ',';
      buffer[length++] = ' ';
    }
    length = writeName(buffer, length, date.month().name(language));
    buffer[length++] = ' ';
    length = writeField(buffer, length, date.day(), options.pad(), zero);
    if (options.year()) {
      buffer[length++] = ',';
      buffer[length++] = ' ';
      length = writeYear(buffer, length, date.year(), zero);
    }
    return new String(buffer, 0, length);
  }

  /**
   * Appends the long form.
   *
   * @param out where to write
   * @param date the date
   * @param options which parts to write
   * @param language the names and digits to use
   * @return {@code out}
   */
  static StringBuilder appendLong(
      StringBuilder out, NepaliDate date, LongDateOptions options, Language language) {
    if (options.weekday()) {
      out.append(date.weekday().name(language)).append(", ");
    }
    out.append(date.month().name(language)).append(' ');
    appendNumber(out, date.day(), options.pad() ? 2 : 0, language);
    if (options.year()) {
      out.append(", ");
      appendNumber(out, date.year(), 4, language);
    }
    return out;
  }

  /**
   * A standard pattern or a custom one (FORMATTING section 5).
   *
   * @param date the date
   * @param pattern the pattern
   * @param language the names and digits to use
   * @return the formatted date
   */
  static String pattern(NepaliDate date, String pattern, Language language) {
    return appendPattern(new StringBuilder(pattern.length() + 16), date, pattern, language)
        .toString();
  }

  /**
   * Appends a standard pattern or a custom one.
   *
   * @param out where to write
   * @param date the date
   * @param pattern the pattern
   * @param language the names and digits to use
   * @return {@code out}
   */
  static StringBuilder appendPattern(
      StringBuilder out, NepaliDate date, String pattern, Language language) {
    switch (pattern) {
      case "", "G", "g", "d" -> {
        return appendShort(out, date, DateOrder.YMD, Separator.SLASH, true, language);
      }
      case "D" -> {
        return appendLong(out, date, LongDateOptions.DEFAULT, language);
      }
      case "s" -> {
        return appendShort(out, date, DateOrder.YMD, Separator.DASH, true, language);
      }
      default -> {
        return appendCustom(out, date, pattern, language);
      }
    }
  }

  private static StringBuilder appendCustom(
      StringBuilder out, NepaliDate date, String pattern, Language language) {
    int index = 0;
    int length = pattern.length();
    while (index < length) {
      char c = pattern.charAt(index);
      switch (c) {
        case '\\' -> {
          // A backslash escapes the next character; a backslash at the very end writes itself.
          if (index + 1 < length) {
            out.append(pattern.charAt(index + 1));
            index += 2;
          } else {
            out.append('\\');
            index++;
          }
        }
        case '\'' -> {
          // An unterminated quote runs to the end of the pattern.
          int close = pattern.indexOf('\'', index + 1);
          int end = close < 0 ? length : close;
          out.append(pattern, index + 1, end);
          index = end + 1;
        }
        case 'y', 'M', 'd' -> {
          int run = index;
          while (run < length && pattern.charAt(run) == c) {
            run++;
          }
          appendToken(out, date, c, run - index, language);
          index = run;
        }
        default -> {
          out.append(c);
          index++;
        }
      }
    }
    return out;
  }

  private static void appendToken(
      StringBuilder out, NepaliDate date, char letter, int run, Language language) {
    switch (letter) {
      case 'y' -> {
        if (run >= 4) {
          appendNumber(out, date.year(), 4, language);
        } else {
          appendNumber(out, date.year() % 100, 2, language);
        }
      }
      case 'M' -> {
        if (run >= 4) {
          out.append(date.month().name(language));
        } else if (run == 3) {
          out.append(date.month().shortName(language));
        } else {
          appendNumber(out, date.monthNumber(), run, language);
        }
      }
      default -> appendNumber(out, date.day(), run == 2 ? 2 : 0, language);
    }
  }

  /**
   * Writes a formatted date into a fixed buffer.
   *
   * @param date the date
   * @param buffer the destination
   * @param pattern the pattern
   * @param language the names and digits to use
   * @return true when the output fits, false when the buffer is too small and is left unchanged
   */
  static boolean tryFormat(NepaliDate date, char[] buffer, String pattern, Language language) {
    StringBuilder out = new StringBuilder(LONG_CAPACITY);
    appendPattern(out, date, pattern, language);
    if (out.length() > buffer.length) {
      return false;
    }
    out.getChars(0, out.length(), buffer, 0);
    return true;
  }
}
