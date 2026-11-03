package io.github.sanamhub.nepdate.java;

/**
 * The twelve months of the Bikram Sambat year, Baishakh (1) to Chaitra (12).
 *
 * <p>The number is a field rather than the enum's position, so a constant can never change meaning
 * because another one was inserted before it.
 */
public enum Month {
  /** Month 1, Baishakh. */
  BAISHAKH(1, "Baishakh", "Bai", "बैशाख"),
  /** Month 2, Jestha. */
  JESTHA(2, "Jestha", "Jes", "जेठ"),
  /** Month 3, Ashad. */
  ASHAD(3, "Ashad", "Asa", "असार"),
  /** Month 4, Shrawan. */
  SHRAWAN(4, "Shrawan", "Shr", "साउन"),
  /** Month 5, Bhadra. */
  BHADRA(5, "Bhadra", "Bha", "भदौ"),
  /** Month 6, Ashoj. */
  ASHOJ(6, "Ashoj", "Aso", "असोज"),
  /** Month 7, Kartik. */
  KARTIK(7, "Kartik", "Kar", "कार्तिक"),
  /** Month 8, Mangsir. */
  MANGSIR(8, "Mangsir", "Man", "मंसिर"),
  /** Month 9, Poush. */
  POUSH(9, "Poush", "Pou", "पुष"),
  /** Month 10, Magh. */
  MAGH(10, "Magh", "Mag", "माघ"),
  /** Month 11, Falgun. */
  FALGUN(11, "Falgun", "Fal", "फागुन"),
  /** Month 12, Chaitra. */
  CHAITRA(12, "Chaitra", "Cha", "चैत");

  private final int number;
  private final String english;
  private final String shortEnglish;
  private final String nepali;

  Month(int number, String english, String shortEnglish, String nepali) {
    this.number = number;
    this.english = english;
    this.shortEnglish = shortEnglish;
    this.nepali = nepali;
  }

  /**
   * The month number, 1 for Baishakh to 12 for Chaitra.
   *
   * @return the number
   */
  public int number() {
    return number;
  }

  /**
   * The full English name.
   *
   * @return for example {@code Shrawan}
   */
  public String english() {
    return english;
  }

  /**
   * The three-letter English abbreviation.
   *
   * <p>Ashad is {@code Asa} and Ashoj is {@code Aso}: the C# library writes {@code Ash} for both,
   * which cannot be told apart.
   *
   * @return for example {@code Shr}
   */
  public String shortEnglish() {
    return shortEnglish;
  }

  /**
   * The full Nepali name in Devanagari.
   *
   * @return for example {@code साउन}
   */
  public String nepali() {
    return nepali;
  }

  /**
   * The month's name in one language.
   *
   * @param language the language of the name
   * @return the English or the Nepali name
   */
  public String name(Language language) {
    return language == Language.NEPALI ? nepali : english;
  }

  /**
   * The month's short name in one language.
   *
   * <p>Nepali has no customary abbreviation, so the Nepali form is the full name.
   *
   * @param language the language of the name
   * @return the English abbreviation or the Nepali full name
   */
  public String shortName(Language language) {
    return language == Language.NEPALI ? nepali : shortEnglish;
  }

  /**
   * The month with a number.
   *
   * @param number 1 to 12
   * @return the month
   * @throws NepDateException with {@link NepDateError#INVALID_MONTH} outside 1 to 12
   */
  public static Month of(int number) {
    if (number < 1 || number > 12) {
      throw new NepDateException(
          NepDateError.INVALID_MONTH, 0, "month " + number + " is outside 1..12");
    }
    return values()[number - 1];
  }
}
