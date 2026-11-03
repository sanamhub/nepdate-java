package io.github.sanamhub.nepdate.java.patro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.sanamhub.nepdate.java.Language;
import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.NepaliDateRange;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The data invariants of PATRO section 3: contiguity, coverage and value semantics. */
class PatroDataTest {
  private static final Map<String, String> SUMMARY = SharedData.summary();

  private static CalendarInfo info(int serial) {
    return Patro.info(NepaliDate.MIN.plusDays(serial));
  }

  private static boolean hasData(CalendarInfo info) {
    return info.tithi().isPresent() || info.isPublicHoliday() || !info.events().isEmpty();
  }

  @Test
  void theTithiRangeIsContiguous() {
    int first = SharedData.number(SUMMARY, "tithi_first_serial");
    int days = SharedData.number(SUMMARY, "tithi_days");
    for (int serial = first; serial < first + days; serial++) {
      assertTrue(info(serial).tithi().isPresent(), "tithi at serial " + serial);
    }
    assertFalse(info(first - 1).tithi().isPresent(), "no tithi before the range");
    assertFalse(info(first + days).tithi().isPresent(), "no tithi after the range");
  }

  @Test
  void theCoverageIsTheSmallestRangeWithData() {
    NepaliDateRange coverage = Patro.coverage();
    assertTrue(hasData(Patro.info(coverage.start())));
    assertTrue(hasData(Patro.info(coverage.end())));
    assertFalse(hasData(Patro.info(coverage.start().minusDays(1))));
    assertFalse(hasData(Patro.info(coverage.end().plusDays(1))));
  }

  @Test
  void everyDayOfTheCoverageHasAStableShape() {
    Set<String> tithiNames = new HashSet<>();
    for (NepaliDate date : Patro.coverage()) {
      CalendarInfo info = Patro.info(date);
      assertEquals(info, Patro.info(date), "deterministic for " + date);
      assertEquals(info.hashCode(), Patro.info(date).hashCode());
      assertFalse(info.events() == null);
      assertEquals(info.tithi().isPresent(), !info.tithiNp().isEmpty());
      assertEquals(info.tithi().isPresent(), !info.tithiEn().isEmpty());
      assertEquals(info.events().size(), info.eventsEn().size());
      assertEquals(info.events().size(), info.eventsNp().size());
      for (int i = 0; i < info.events().size(); i++) {
        assertEquals(info.events().get(i).english(), info.eventsEn().get(i));
        assertEquals(info.events().get(i).nepali(), info.eventsNp().get(i));
      }
      info.tithi().ifPresent(tithi -> tithiNames.add(tithi.english()));
      assertEquals(false, info.events().isEmpty() && info.isPublicHoliday() && !hasData(info));
    }
    assertEquals(SharedData.number(SUMMARY, "tithi_names"), tithiNames.size());
  }

  @Test
  void theSharedEmptyValueIsUsedOutsideTheData() {
    CalendarInfo before = Patro.info(NepaliDate.parse("1901-01-01"));
    CalendarInfo after = Patro.info(NepaliDate.parse("2199-12-30"));
    assertEquals(before, after);
    assertEquals(before.hashCode(), after.hashCode());
    assertEquals("CalendarInfo[tithi=none, holiday=false, events=0]", before.toString());
  }

  @Test
  void calendarInfoIsAValue() {
    CalendarInfo first = Patro.info(NepaliDate.parse("2081-01-01"));
    CalendarInfo same = Patro.info(NepaliDate.parse("2081-01-01"));
    CalendarInfo other = Patro.info(NepaliDate.parse("2081-01-02"));
    assertEquals(first, same);
    assertEquals(first.hashCode(), same.hashCode());
    assertNotEquals(first, other);
    assertEquals(false, first.equals(null));
    assertEquals(false, first.equals("2081-01-01"));
    assertTrue(first.toString().contains("Panchami"));
  }

  @Test
  void namedCarriesBothLanguagesAndRefusesNull() {
    Named name = new Named("नयाँ वर्ष", "New Year");
    assertEquals("नयाँ वर्ष", name.name(Language.NEPALI));
    assertEquals("New Year", name.name(Language.ENGLISH));
    assertEquals("नयाँ वर्ष", name.nepali());
    assertEquals("New Year", name.english());
    assertThrows(NullPointerException.class, () -> new Named(null, "New Year"));
    assertThrows(NullPointerException.class, () -> new Named("नयाँ वर्ष", null));
    assertEquals(name, new Named("नयाँ वर्ष", "New Year"));
    assertEquals(name.hashCode(), new Named("नयाँ वर्ष", "New Year").hashCode());
  }

  @Test
  void everyPublicHolidayIsInTheCoverageAndFlagged() {
    NepaliDateRange coverage = Patro.coverage();
    for (NepaliDate date : Patro.publicHolidays()) {
      assertTrue(coverage.contains(date));
      assertTrue(Patro.info(date).isPublicHoliday());
    }
  }

  @Test
  void theEventsOfADayKeepFileOrder() {
    CalendarInfo info = Patro.info(NepaliDate.parse("2080-11-01"));
    assertEquals("Mangala Chauthi Brata", info.eventsEn().get(0));
    assertEquals("World Radio Day", info.eventsEn().get(info.eventsEn().size() - 1));
  }
}
