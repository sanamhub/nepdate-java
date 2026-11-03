package io.github.sanamhub.nepdate.java;

/** The order of the year, month and day in a short date. */
public enum DateOrder {
  /** Year, month, day: {@code 2080/05/15}. */
  YMD,
  /** Year, day, month: {@code 2080/15/05}. */
  YDM,
  /** Month, year, day: {@code 05/2080/15}. */
  MYD,
  /** Month, day, year: {@code 05/15/2080}. */
  MDY,
  /** Day, year, month: {@code 15/2080/05}. */
  DYM,
  /** Day, month, year: {@code 15/05/2080}. */
  DMY,
}
