package io.github.sanamhub.nepdate.java;

import java.io.Serial;

/**
 * Thrown when a date cannot be built or text cannot be parsed.
 *
 * <p>It extends {@link IllegalArgumentException}, which callers of {@link
 * java.time.LocalDate#of(int, int, int)} already handle: a date outside the calendar, an invalid
 * month, a day past the end of the month and unparseable text are all argument errors. The library
 * uses exceptions for the one-result forms and {@link java.util.Optional} for the {@code tryParse}
 * forms, so a caller chooses once and the choice is the same everywhere.
 *
 * <p>{@link #error()} is the machine-readable reason; the message is for people.
 */
public final class NepDateException extends IllegalArgumentException {
  @Serial private static final long serialVersionUID = 1L;

  /** What went wrong. */
  private final NepDateError error;

  /** The length of the month for an invalid day, 0 otherwise. */
  private final int monthLength;

  /**
   * Creates an exception.
   *
   * @param error what went wrong
   * @param monthLength the length of the month for {@link NepDateError#INVALID_DAY}, 0 otherwise
   * @param message the message for people
   */
  public NepDateException(NepDateError error, int monthLength, String message) {
    super(message);
    this.error = error;
    this.monthLength = monthLength;
  }

  /**
   * The machine-readable reason.
   *
   * @return the error, never null
   */
  public NepDateError error() {
    return error;
  }

  /**
   * The length of the month that made a day invalid.
   *
   * @return 29 to 32 for {@link NepDateError#INVALID_DAY}, 0 for every other error
   */
  public int monthLength() {
    return monthLength;
  }
}
