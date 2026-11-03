package io.github.sanamhub.nepdate.java.patro;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything the patro knows about one day, as the C# {@code CalendarInfo} does: a tithi, a public
 * holiday flag and the day's events in display order.
 *
 * <p>Outside the data range all five values are empty: no tithi, no holiday and no events. Nothing
 * is null. The C# type is a struct with five members; here it is an immutable value with the same
 * members plus the two-language form the patro data has.
 */
public final class CalendarInfo {
  private final Named tithi;
  private final boolean publicHoliday;
  private final List<Named> events;

  CalendarInfo(Named tithi, boolean publicHoliday, List<Named> events) {
    this.tithi = tithi;
    this.publicHoliday = publicHoliday;
    this.events = List.copyOf(events);
  }

  /**
   * The day's tithi.
   *
   * @return the tithi, or an empty {@link Optional} outside the tithi range
   */
  public Optional<Named> tithi() {
    return Optional.ofNullable(tithi);
  }

  /**
   * The tithi's Nepali name, as the C# {@code TithiNp}.
   *
   * @return the Devanagari name, or an empty string when the day has no tithi
   */
  public String tithiNp() {
    return tithi == null ? "" : tithi.nepali();
  }

  /**
   * The tithi's English name, as the C# {@code TithiEn}.
   *
   * @return the English name, or an empty string when the day has no tithi
   */
  public String tithiEn() {
    return tithi == null ? "" : tithi.english();
  }

  /**
   * Whether the day is a gazetted public holiday, as the C# {@code IsPublicHoliday}.
   *
   * @return true for a public holiday
   */
  public boolean isPublicHoliday() {
    return publicHoliday;
  }

  /**
   * The day's events in display order, as the C# {@code EventsNp} and {@code EventsEn} together.
   *
   * @return an unmodifiable list, empty when the day has no events
   */
  public List<Named> events() {
    return events;
  }

  /**
   * The Nepali names of the day's events in order, as the C# {@code EventsNp}.
   *
   * @return an unmodifiable list of Devanagari names, possibly empty
   */
  public List<String> eventsNp() {
    List<String> names = new ArrayList<>(events.size());
    for (Named event : events) {
      names.add(event.nepali());
    }
    return List.copyOf(names);
  }

  /**
   * The English names of the day's events in order, as the C# {@code EventsEn}.
   *
   * @return an unmodifiable list of English names, possibly empty
   */
  public List<String> eventsEn() {
    List<String> names = new ArrayList<>(events.size());
    for (Named event : events) {
      names.add(event.english());
    }
    return List.copyOf(names);
  }

  /**
   * Whether both values describe the same day.
   *
   * @param other the other value
   * @return true when the tithi, the holiday flag and the events are equal
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof CalendarInfo info
        && info.publicHoliday == publicHoliday
        && Objects.equals(info.tithi, tithi)
        && info.events.equals(events);
  }

  /**
   * A hash code from the three parts.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return Objects.hash(tithi, publicHoliday, events);
  }

  /**
   * The day's information as text, for debugging only; the format may change.
   *
   * @return a description of the three parts
   */
  @Override
  public String toString() {
    return "CalendarInfo[tithi="
        + (tithi == null ? "none" : tithi.english())
        + ", holiday="
        + publicHoliday
        + ", events="
        + events.size()
        + "]";
  }
}
