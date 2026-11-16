package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The {@link String} forms fill one array and the {@link StringBuilder} forms append piece by
 * piece. Both must write the same text for every day, option and language.
 */
class TextFormsTest {
  private static String padded(int value, int width) {
    String text = Integer.toString(value);
    return "0".repeat(Math.max(0, width - text.length())) + text;
  }

  private static String nepali(String ascii) {
    StringBuilder out = new StringBuilder(ascii.length());
    for (int i = 0; i < ascii.length(); i++) {
      char c = ascii.charAt(i);
      out.append(c >= '0' && c <= '9' ? (char) ('०' + (c - '0')) : c);
    }
    return out.toString();
  }

  @Test
  void theLongFormOfEveryDayMatchesTheAppendedOne() {
    boolean[] flags = {false, true};
    for (int serial = 0; serial <= CalendarTable.MAX_SERIAL; serial++) {
      NepaliDate date = NepaliDate.ofSerial(serial);
      for (Language language : Language.values()) {
        for (boolean weekday : flags) {
          for (boolean year : flags) {
            for (boolean pad : flags) {
              LongDateOptions options = new LongDateOptions(weekday, year, pad);
              String expected =
                  Text.appendLong(new StringBuilder(), date, options, language).toString();
              assertEquals(expected, date.toLongString(options, language));
            }
          }
        }
      }
    }
  }

  @Test
  void theShortFormOfEveryDayMatchesTheAppendedOne() {
    boolean[] flags = {false, true};
    for (int serial = 0; serial <= CalendarTable.MAX_SERIAL; serial += 3) {
      NepaliDate date = NepaliDate.ofSerial(serial);
      for (Language language : Language.values()) {
        for (DateOrder order : DateOrder.values()) {
          for (boolean pad : flags) {
            Separator separator = Separator.values()[serial % Separator.values().length];
            String expected =
                Text.appendShort(new StringBuilder(), date, order, separator, pad, language)
                    .toString();
            assertEquals(expected, date.toShortString(order, separator, pad, language));
          }
        }
      }
    }
  }

  @Test
  void everyDayPrintsItsPaddedNumbers() {
    for (int serial = 0; serial <= CalendarTable.MAX_SERIAL; serial++) {
      NepaliDate date = NepaliDate.ofSerial(serial);
      String year = padded(date.year(), 4);
      String month = padded(date.monthNumber(), 2);
      String day = padded(date.day(), 2);
      assertEquals(year + "/" + month + "/" + day, date.toString());
      assertEquals(year + "-" + month + "-" + day, date.toIsoString());
      assertEquals(
          nepali(day + "." + month + "." + year),
          date.toShortString(DateOrder.DMY, Separator.DOT, true, Language.NEPALI));
      assertEquals(
          date.day() + " " + date.monthNumber() + " " + date.year(),
          date.toShortString(DateOrder.DMY, Separator.SPACE, false));
      assertEquals(
          date.weekday().name(Language.NEPALI)
              + ", "
              + date.month().name(Language.NEPALI)
              + " "
              + nepali(day)
              + ", "
              + nepali(year),
          date.toLongString(new LongDateOptions(true, true, true), Language.NEPALI));
      assertEquals(
          date.month().name(Language.ENGLISH) + " " + date.day(),
          date.toLongString(new LongDateOptions(false, false, false), Language.ENGLISH));
    }
  }

  @Test
  void everyOrderPlacesTheThreeFields() {
    NepaliDate date = NepaliDate.of(2081, 4, 5);
    assertEquals("2081.04.05", date.toShortString(DateOrder.YMD, Separator.DOT, true));
    assertEquals("2081.05.04", date.toShortString(DateOrder.YDM, Separator.DOT, true));
    assertEquals("04.2081.05", date.toShortString(DateOrder.MYD, Separator.DOT, true));
    assertEquals("04.05.2081", date.toShortString(DateOrder.MDY, Separator.DOT, true));
    assertEquals("05.2081.04", date.toShortString(DateOrder.DYM, Separator.DOT, true));
    assertEquals("05.04.2081", date.toShortString(DateOrder.DMY, Separator.DOT, true));
    assertEquals(
        "२०८१/४/५", date.toShortString(DateOrder.YMD, Separator.SLASH, false, Language.NEPALI));
    assertEquals(
        "1901/9/10",
        NepaliDate.of(1901, 9, 10).toShortString(DateOrder.YMD, Separator.SLASH, false));
    assertEquals(
        "2199/10/9",
        NepaliDate.of(2199, 10, 9).toShortString(DateOrder.YMD, Separator.SLASH, false));
  }
}
