package io.github.sanamhub.nepdate.java.patro;

import io.github.sanamhub.nepdate.java.Language;
import java.util.Objects;

/**
 * A name in both languages, as the patro data stores every tithi and event.
 *
 * @param nepali the Nepali name in Devanagari
 * @param english the English name
 */
public record Named(String nepali, String english) {
  /**
   * Checks that both names are present.
   *
   * @param nepali the Nepali name in Devanagari
   * @param english the English name
   * @throws NullPointerException when a name is null
   */
  public Named {
    Objects.requireNonNull(nepali, "nepali");
    Objects.requireNonNull(english, "english");
  }

  /**
   * The name in one language.
   *
   * @param language English or Nepali
   * @return {@link #nepali()} for Nepali, {@link #english()} otherwise
   */
  public String name(Language language) {
    return language == Language.NEPALI ? nepali : english;
  }
}
