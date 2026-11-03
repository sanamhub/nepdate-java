package consumer;

import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.patro.CalendarInfo;
import io.github.sanamhub.nepdate.java.patro.Patro;
import java.time.LocalDate;

/** Resolves the published coordinates and uses the README's first example. */
public final class ConsumerCheck {
  private ConsumerCheck() {}

  public static void main(String[] args) {
    NepaliDate date = NepaliDate.of(2081, 4, 15);
    if (!date.toLocalDate().equals(LocalDate.of(2024, 7, 30))) {
      throw new AssertionError("conversion: " + date.toLocalDate());
    }
    if (!date.toIsoString().equals("2081-04-15")) {
      throw new AssertionError("canonical text: " + date.toIsoString());
    }
    if (!NepaliDate.parse("2081/04/15").equals(date)) {
      throw new AssertionError("parse round trip");
    }
    CalendarInfo info = Patro.info(date);
    if (info.eventsEn().isEmpty() || info.isPublicHoliday()) {
      throw new AssertionError("patro: " + info.eventsEn());
    }
    System.out.println("consumer-check: resolved both coordinates and read " + info.tithiEn());
  }
}
