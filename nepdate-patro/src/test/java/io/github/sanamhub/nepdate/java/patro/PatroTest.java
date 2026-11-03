package io.github.sanamhub.nepdate.java.patro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.sanamhub.nepdate.java.Language;
import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.NepaliDateRange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The anchors of PATRO section 4 and the counts of the hub's summary. */
class PatroTest {
  private static final Map<String, String> SUMMARY = SharedData.summary();

  @Test
  void newYear2081() {
    CalendarInfo info = Patro.info(NepaliDate.parse("2081-01-01"));
    assertEquals("Panchami", info.tithiEn());
    assertEquals("पञ्चमी", info.tithiNp());
    assertEquals(true, info.isPublicHoliday());
    assertEquals(List.of("New Year", "Mesh Sankranti", "Bisket Jatra"), info.eventsEn());
    assertEquals(List.of("नयाँ वर्ष", "मेष संक्रान्ति", "बिस्का: जात्रा"), info.eventsNp());
    assertEquals("Panchami", info.tithi().orElseThrow().english());
    assertEquals("पञ्चमी", info.tithi().orElseThrow().name(Language.NEPALI));
  }

  @Test
  void theNextDayIsQuiet() {
    CalendarInfo info = Patro.info(NepaliDate.parse("2081-01-02"));
    assertEquals("Shashthi", info.tithiEn());
    assertEquals(false, info.isPublicHoliday());
    assertEquals(List.of(), info.events());
    assertEquals(List.of(), info.eventsNp());
    assertEquals(List.of(), info.eventsEn());
    assertEquals("", Patro.info(NepaliDate.parse("2070-06-15")).tithiEn());
  }

  @Test
  void republicDay() {
    CalendarInfo info = Patro.info(NepaliDate.parse("2081-02-15"));
    assertEquals("Panchami", info.tithiEn());
    assertEquals(true, info.isPublicHoliday());
    assertEquals(List.of("Republic Day"), info.eventsEn());
  }

  @Test
  void aDayWithFiveEventsKeepsTheirOrder() {
    CalendarInfo info = Patro.info(NepaliDate.parse("2080-11-01"));
    assertEquals("Chaturthi", info.tithiEn());
    assertEquals(false, info.isPublicHoliday());
    assertEquals(
        List.of(
            "Mangala Chauthi Brata",
            "People's War Day",
            "Tilakund Chauthi",
            "Kumbha Sankranti",
            "World Radio Day"),
        info.eventsEn());
    assertEquals(5, info.events().size());
  }

  @Test
  void anEventDayThatIsNotAHoliday() {
    CalendarInfo info = Patro.info(NepaliDate.parse("2081-04-15"));
    assertEquals("Dashami", info.tithiEn());
    assertEquals(false, info.isPublicHoliday());
    assertEquals(
        List.of(
            "Kheer Khane Din",
            "World Day Against Human Trafficking",
            "International Friendship Day"),
        info.eventsEn());
    assertEquals(3, info.events().size());
  }

  @Test
  void daysWithoutDataAreEmpty() {
    for (String text : List.of("2070-06-15", "2090-01-01", "1901-01-01", "2199-12-30")) {
      CalendarInfo info = Patro.info(NepaliDate.parse(text));
      assertEquals(false, info.tithi().isPresent(), text);
      assertEquals("", info.tithiNp(), text);
      assertEquals("", info.tithiEn(), text);
      assertEquals(false, info.isPublicHoliday(), text);
      assertEquals(List.of(), info.events(), text);
    }
  }

  @Test
  void theCoverageIsTheHubRange() {
    NepaliDateRange coverage = Patro.coverage();
    assertEquals(NepaliDate.parse(SUMMARY.get("first_day")), coverage.start());
    assertEquals(NepaliDate.parse(SUMMARY.get("last_day")), coverage.end());
    assertEquals(
        SharedData.number(SUMMARY, "first_serial"),
        (int) coverage.start().daysSince(NepaliDate.MIN));
    assertEquals(
        SharedData.number(SUMMARY, "last_serial"), (int) coverage.end().daysSince(NepaliDate.MIN));
    assertEquals(SharedData.number(SUMMARY, "days"), coverage.length());
  }

  @Test
  void theCountsMatchTheSummary() {
    int tithiDays = 0;
    int holidays = 0;
    int eventDays = 0;
    int events = 0;
    java.util.Set<Named> tithiNames = new java.util.HashSet<>();
    java.util.Set<Named> eventNames = new java.util.HashSet<>();
    for (NepaliDate date : Patro.coverage()) {
      CalendarInfo info = Patro.info(date);
      if (info.tithi().isPresent()) {
        tithiDays++;
        tithiNames.add(info.tithi().orElseThrow());
      }
      if (info.isPublicHoliday()) {
        holidays++;
      }
      if (!info.events().isEmpty()) {
        eventDays++;
        events += info.events().size();
        eventNames.addAll(info.events());
      }
    }
    assertEquals(SharedData.number(SUMMARY, "tithi_days"), tithiDays);
    assertEquals(SharedData.number(SUMMARY, "tithi_names"), tithiNames.size());
    assertEquals(SharedData.number(SUMMARY, "holidays"), holidays);
    assertEquals(SharedData.number(SUMMARY, "event_days"), eventDays);
    assertEquals(SharedData.number(SUMMARY, "events"), events);
    assertEquals(SharedData.number(SUMMARY, "event_names"), eventNames.size());
    assertEquals(SharedData.number(SUMMARY, "names"), tithiNames.size() + eventNames.size());
  }

  @Test
  void thePublicHolidaysMatchTheFlag() {
    List<NepaliDate> holidays = Patro.publicHolidays();
    assertEquals(SharedData.number(SUMMARY, "holidays"), holidays.size());
    int previous = -1;
    for (NepaliDate date : holidays) {
      int serial = (int) date.daysSince(NepaliDate.MIN);
      assertTrue(serial > previous, "holidays are ascending and distinct");
      previous = serial;
      assertEquals(true, Patro.info(date).isPublicHoliday(), "flag of " + date);
      assertEquals(true, Patro.coverage().contains(date), "inside coverage: " + date);
    }
    assertEquals(holidays, Patro.publicHolidays());
  }

  @Test
  void theShapesNeverReturnNull() {
    for (NepaliDate date :
        List.of(NepaliDate.MIN, NepaliDate.MAX, NepaliDate.parse("2081-01-01"))) {
      CalendarInfo info = Patro.info(date);
      assertFalse(info.events() == null);
      assertFalse(info.tithi() == null);
      assertEquals(info.tithi().isPresent() ? info.tithiEn() : "", info.tithiEn());
    }
  }
}
