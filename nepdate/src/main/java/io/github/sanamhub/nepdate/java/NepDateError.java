package io.github.sanamhub.nepdate.java;

/**
 * What went wrong, in the order the specification checks it.
 *
 * <p>{@link NepDateException#error()} carries one of these values; check it rather than the
 * message, which is written for people and may change.
 */
public enum NepDateError {
  /** The year, AD date or epoch day is outside the supported range. */
  OUT_OF_RANGE,

  /** The month is outside 1 to 12. */
  INVALID_MONTH,

  /** The day is outside 1 to the length of the month. */
  INVALID_DAY,

  /** The AD date does not exist in the Gregorian calendar, for example 2023-02-29. */
  INVALID_GREGORIAN,

  /** Strict parsing found a character that is neither a digit nor a separator. */
  INVALID_CHARACTER,

  /** Strict parsing did not find exactly three groups of digits. */
  WRONG_GROUP_COUNT,

  /** A numeric group has more than four digits, or a lenient numeric token does. */
  NUMBER_TOO_LONG,

  /** Lenient parsing cannot tell which number is the year. */
  AMBIGUOUS,

  /** Lenient parsing matches no accepted form. */
  UNRECOGNIZED,
}
