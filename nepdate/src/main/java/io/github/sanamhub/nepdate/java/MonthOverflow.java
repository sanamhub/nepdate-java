package io.github.sanamhub.nepdate.java;

/**
 * What month arithmetic does when the day does not exist in the target month.
 *
 * <p>2081-04-32 plus two months lands in Ashoj 2081, which has 30 days: {@link #CLAMP} gives
 * 2081-06-30 and {@link #SPILL} carries the two extra days into the next month, 2081-07-02.
 */
public enum MonthOverflow {
  /** Use the last day of the target month. The default. */
  CLAMP,

  /** Carry the days past the target month's end into the next month. */
  SPILL,
}
