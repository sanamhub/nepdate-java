package io.github.sanamhub.nepdate.java;

/** The character between the numbers of a short date. */
public enum Separator {
  /** {@code /}. */
  SLASH('/'),
  /** {@code \}. */
  BACKSLASH('\\'),
  /** {@code .}. */
  DOT('.'),
  /** {@code _}. */
  UNDERSCORE('_'),
  /** {@code -}. */
  DASH('-'),
  /** A space. */
  SPACE(' ');

  private final char character;

  Separator(char character) {
    this.character = character;
  }

  /**
   * The character written between the numbers.
   *
   * @return the character
   */
  public char character() {
    return character;
  }
}
