package io.github.sanamhub.nepdate.java;

import java.util.Optional;

/**
 * The lenient parser (PARSING sections 2 and 3).
 *
 * <p>It tries the strict parser first, then reads tokens: numbers, month names, era markers and the
 * words {@code गते} and {@code मिति}. The case folding is the reference's, ASCII letters and the
 * Kelvin sign only, so the long s and the dotless i do not match, and there is no Unicode
 * normalisation. It rejects rather than guesses.
 */
final class LenientParser {
  private static final int END_SHIFT = 31;
  private static final long NUMBER_FLAG = 1L << 62;
  private static final long INDEX_MASK = 0x7FFFFFFFL;
  private static final int MAX_DIGITS = 4;

  // PARSING section 3, in the order the table lists the spellings. One entry per accepted
  // spelling, so a spelling and its month cannot drift apart; a test pins every row of the table.
  private record MonthToken(String text, int month) {}

  private static final MonthToken[] MONTH_TOKENS = {
    new MonthToken("baishakh", 1),
    new MonthToken("baisakh", 1),
    new MonthToken("baishak", 1),
    new MonthToken("vaishakh", 1),
    new MonthToken("vaisakh", 1),
    new MonthToken("bai", 1),
    new MonthToken("बैशाख", 1),
    new MonthToken("वैशाख", 1),
    new MonthToken("बैसाख", 1),
    new MonthToken("jestha", 2),
    new MonthToken("jeth", 2),
    new MonthToken("jyestha", 2),
    new MonthToken("jes", 2),
    new MonthToken("जेठ", 2),
    new MonthToken("जेष्ठ", 2),
    new MonthToken("ज्येष्ठ", 2),
    new MonthToken("ashad", 3),
    new MonthToken("ashadh", 3),
    new MonthToken("asar", 3),
    new MonthToken("asadh", 3),
    new MonthToken("asa", 3),
    new MonthToken("असार", 3),
    new MonthToken("आषाढ", 3),
    new MonthToken("असाढ", 3),
    new MonthToken("shrawan", 4),
    new MonthToken("shravan", 4),
    new MonthToken("srawan", 4),
    new MonthToken("sawan", 4),
    new MonthToken("saun", 4),
    new MonthToken("shr", 4),
    new MonthToken("साउन", 4),
    new MonthToken("श्रावण", 4),
    new MonthToken("सावन", 4),
    new MonthToken("bhadra", 5),
    new MonthToken("bhadau", 5),
    new MonthToken("bha", 5),
    new MonthToken("भदौ", 5),
    new MonthToken("भाद्र", 5),
    new MonthToken("भाद्रपद", 5),
    new MonthToken("ashoj", 6),
    new MonthToken("asoj", 6),
    new MonthToken("ashwin", 6),
    new MonthToken("aso", 6),
    new MonthToken("असोज", 6),
    new MonthToken("आश्विन", 6),
    new MonthToken("असौज", 6),
    new MonthToken("kartik", 7),
    new MonthToken("kattik", 7),
    new MonthToken("kar", 7),
    new MonthToken("कार्तिक", 7),
    new MonthToken("कात्तिक", 7),
    new MonthToken("mangsir", 8),
    new MonthToken("mansir", 8),
    new MonthToken("margashirsha", 8),
    new MonthToken("man", 8),
    new MonthToken("मंसिर", 8),
    new MonthToken("मङ्सिर", 8),
    new MonthToken("मार्गशीर्ष", 8),
    new MonthToken("poush", 9),
    new MonthToken("paush", 9),
    new MonthToken("push", 9),
    new MonthToken("pus", 9),
    new MonthToken("pou", 9),
    new MonthToken("पुष", 9),
    new MonthToken("पौष", 9),
    new MonthToken("पुस", 9),
    new MonthToken("magh", 10),
    new MonthToken("mag", 10),
    new MonthToken("माघ", 10),
    new MonthToken("falgun", 11),
    new MonthToken("phalgun", 11),
    new MonthToken("fagun", 11),
    new MonthToken("fal", 11),
    new MonthToken("फागुन", 11),
    new MonthToken("फाल्गुन", 11),
    new MonthToken("chaitra", 12),
    new MonthToken("chait", 12),
    new MonthToken("cha", 12),
    new MonthToken("चैत", 12),
    new MonthToken("चैत्र", 12)
  };

  // Era markers, folded, and the two Nepali words, matched exactly.
  private static final String[] DROPPED_TOKENS = {
    "bs", "b.s.", "b.s", "vs", "v.s.", "v.s",
    "बि.सं.", "वि.सं.", "बि.सं", "वि.सं", "गते", "मिति"
  };

  private LenientParser() {}

  private static long token(int start, int end, boolean number) {
    return start | ((long) end << END_SHIFT) | (number ? NUMBER_FLAG : 0L);
  }

  /**
   * Whether a character separates lenient tokens: the strict separators except the dot, plus a
   * comma. A dot is kept because it is part of {@code B.S.} and of {@code 2080.05.15}.
   *
   * @param c the character
   * @return true for a separator
   */
  static boolean isSeparator(char c) {
    return c != '.' && (Parsing.isSeparator(c) || c == ',');
  }

  private static boolean isNumericRun(CharSequence text, int start) {
    int i = start;
    while (i < text.length() && !isSeparator(text.charAt(i))) {
      char c = text.charAt(i);
      if (c != '.' && Parsing.digitValue(c) < 0) {
        return false;
      }
      i++;
    }
    return true;
  }

  private static long nextNumber(CharSequence text, int from) {
    int i = from;
    while (i < text.length() && text.charAt(i) == '.') {
      i++;
    }
    if (i == text.length() || isSeparator(text.charAt(i))) {
      return -1L;
    }
    int end = i;
    while (end < text.length() && Parsing.digitValue(text.charAt(end)) >= 0) {
      end++;
    }
    return token(i, end, true);
  }

  private static long nextToken(CharSequence text, int from) {
    if (from > 0 && from < text.length() && !isSeparator(text.charAt(from - 1))) {
      long number = nextNumber(text, from);
      if (number >= 0) {
        return number;
      }
    }
    int i = from;
    while (i < text.length()) {
      if (isSeparator(text.charAt(i)) || (i > 0 && !isSeparator(text.charAt(i - 1)))) {
        i++;
        continue;
      }
      if (isNumericRun(text, i)) {
        long number = nextNumber(text, i);
        if (number >= 0) {
          return number;
        }
        i++;
        continue;
      }
      int end = i;
      while (end < text.length() && !isSeparator(text.charAt(end))) {
        end++;
      }
      return token(i, end, false);
    }
    return -1L;
  }

  /**
   * Reads a lenient date (PARSING section 2).
   *
   * @param text the input
   * @return the packed fields, or a negative error code
   */
  static long scanLenient(CharSequence text) {
    long strict = Parsing.scanStrictValid(text);
    if (strict >= 0) {
      return strict;
    }
    int numbers = 0;
    int words = 0;
    int month = 0;
    boolean tooLong = false;
    int value0 = 0;
    int value1 = 0;
    int value2 = 0;
    int length0 = 0;
    int length1 = 0;
    int length2 = 0;
    int position = 0;
    while (true) {
      long found = nextToken(text, position);
      if (found < 0) {
        break;
      }
      int start = (int) (found & INDEX_MASK);
      int end = (int) ((found >>> END_SHIFT) & INDEX_MASK);
      position = end;
      if ((found & NUMBER_FLAG) != 0) {
        int length = end - start;
        if (length > MAX_DIGITS) {
          tooLong = true;
        }
        int value = length > MAX_DIGITS ? 0 : numberValue(text, start, end);
        switch (numbers) {
          case 0 -> {
            value0 = value;
            length0 = length;
          }
          case 1 -> {
            value1 = value;
            length1 = length;
          }
          case 2 -> {
            value2 = value;
            length2 = length;
          }
          default -> {
            // More than three numbers is UNRECOGNIZED below.
          }
        }
        numbers++;
      } else if (!isDropped(text, start, end)) {
        words++;
        if (words == 1) {
          month = monthOfWord(text, start, end);
        }
      }
    }
    if (tooLong) {
      return Parsing.scanError(NepDateError.NUMBER_TOO_LONG);
    }
    if (words == 1 && month > 0 && numbers == 2) {
      return monthNameForm(value0, length0, value1, length1, month);
    }
    if (words == 0 && numbers == 3) {
      return numericForm(value0, length0, value1, value2, length2);
    }
    return Parsing.scanError(NepDateError.UNRECOGNIZED);
  }

  /**
   * Parses leniently (PARSING section 2).
   *
   * @param text the input
   * @return the date
   * @throws NepDateException with a parse error or a validation error
   */
  static NepaliDate parse(CharSequence text) {
    long value = scanLenient(text);
    if (value < 0) {
      throw Parsing.parseException(text, Parsing.scanErrorOf(value));
    }
    return NepaliDate.of(
        Parsing.scannedYear(value), Parsing.scannedMonth(value), Parsing.scannedDay(value));
  }

  /**
   * Parses leniently without throwing.
   *
   * @param text the input
   * @return the date, or empty
   */
  static Optional<NepaliDate> tryParse(CharSequence text) {
    long value = scanLenient(text);
    if (value < 0) {
      return Optional.empty();
    }
    int year = Parsing.scannedYear(value);
    int month = Parsing.scannedMonth(value);
    int day = Parsing.scannedDay(value);
    if (NepaliDate.validationError(year, month, day) != null) {
      return Optional.empty();
    }
    return Optional.of(NepaliDate.of(year, month, day));
  }

  private static int numberValue(CharSequence text, int start, int end) {
    int value = 0;
    for (int i = start; i < end; i++) {
      value = value * 10 + Parsing.digitValue(text.charAt(i));
    }
    return value;
  }

  private static long monthNameForm(int value0, int length0, int value1, int length1, int month) {
    boolean firstIsYear = length0 == MAX_DIGITS;
    boolean secondIsYear = length1 == MAX_DIGITS;
    int dayLength = firstIsYear ? length1 : length0;
    if (firstIsYear == secondIsYear || dayLength > 2) {
      return Parsing.scanError(NepDateError.AMBIGUOUS);
    }
    return firstIsYear
        ? Parsing.scanned(value0, month, value1)
        : Parsing.scanned(value1, month, value0);
  }

  private static long numericForm(int value0, int length0, int value1, int value2, int length2) {
    if (length0 == MAX_DIGITS) {
      return Parsing.scanned(value0, value1, value2);
    }
    if (length2 == MAX_DIGITS) {
      return Parsing.scanned(value2, value1, value0);
    }
    return Parsing.scanError(NepDateError.AMBIGUOUS);
  }

  private static char foldCase(char c) {
    if (c >= 'A' && c <= 'Z') {
      return (char) (c + ('a' - 'A'));
    }
    if (c == '\u212A') {
      return 'k';
    }
    return c;
  }

  private static boolean tokenEquals(CharSequence text, int start, int end, String token) {
    if (end - start != token.length()) {
      return false;
    }
    for (int i = 0; i < token.length(); i++) {
      if (foldCase(text.charAt(start + i)) != token.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private static int monthOfWord(CharSequence text, int start, int end) {
    for (MonthToken token : MONTH_TOKENS) {
      if (tokenEquals(text, start, end, token.text())) {
        return token.month();
      }
    }
    return 0;
  }

  private static boolean isDropped(CharSequence text, int start, int end) {
    for (String token : DROPPED_TOKENS) {
      if (tokenEquals(text, start, end, token)) {
        return true;
      }
    }
    return false;
  }
}
