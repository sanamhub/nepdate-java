package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** The remaining paths: iterator ends, every clock overload and the serialization guard. */
class CoverageEdgesTest {
  @Test
  void aRangeIteratesWithTheForEachForm() {
    NepaliDateRange range = Vectors.range("2081-04-10..2081-04-12");
    int count = 0;
    for (NepaliDate date : range) {
      count++;
      assertEquals(true, range.contains(date));
    }
    assertEquals(3, count);
    Iterator<NepaliDate> iterator = range.iterator();
    iterator.next();
    iterator.next();
    iterator.next();
    assertThrows(NoSuchElementException.class, iterator::next);
  }

  @Test
  void rangesHashFromTheirBounds() {
    NepaliDateRange first = Vectors.range("2081-04-10..2081-04-20");
    NepaliDateRange same = Vectors.range("2081-04-10..2081-04-20");
    assertEquals(first, same);
    assertEquals(first.hashCode(), same.hashCode());
    assertEquals(false, first.equals(Vectors.range("2081-04-10..2081-04-21")));
    assertEquals(false, first.equals("2081-04-10..2081-04-20"));
  }

  @Test
  void theYearOverloads() {
    NepaliDate date = NepaliDate.parse("2081-04-32");
    assertEquals(NepaliDate.parse("2082-04-31"), date.plusYears(1));
    assertEquals(NepaliDate.parse("2081-06-30"), date.plusYears(0).plusMonths(2));
    assertEquals(
        NepaliDate.parse("2081-07-02"), date.plusYears(0).plusMonths(2, MonthOverflow.SPILL));
    assertEquals(NepaliDate.parse("2080-04-32"), date.plusYears(-1));
    assertEquals(date, date.plusYears(0));
  }

  @Test
  void spillingPastTheLastMonthIsRefused() {
    // 2081-04-32 plus enough months to reach 2199-12 leaves two days past that month's end.
    NepaliDate date = NepaliDate.parse("2081-04-32");
    int months = (2199 - 2081) * 12 + (12 - 4);
    assertEquals(NepaliDate.parse("2199-12-30"), date.plusMonths(months));
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> date.plusMonths(months, MonthOverflow.SPILL));
  }

  @Test
  void everyRelativePredicateAndBothArgumentForms() {
    Clock clock = Clock.fixed(Instant.parse("2024-07-30T18:30:00Z"), NepaliDate.KATHMANDU);
    NepaliDate today = NepaliDate.parse("2081-04-16");
    assertEquals(true, today.isToday(clock));
    assertEquals(true, today.minusDays(1).isYesterday(clock));
    assertEquals(true, today.plusDays(1).isTomorrow(clock));
    assertEquals(false, today.plusDays(2).isTomorrow(clock));
    // The ZoneId forms read the system clock, so they are checked against today in that zone.
    NepaliDate now = NepaliDate.today(NepaliDate.KATHMANDU);
    assertEquals(true, now.isToday(NepaliDate.KATHMANDU));
    assertEquals(true, now.minusDays(1).isYesterday(NepaliDate.KATHMANDU));
    assertEquals(true, now.plusDays(1).isTomorrow(NepaliDate.KATHMANDU));
  }

  @Test
  void theGregorianLeapRuleAndJanuary() {
    assertEquals(NepaliDate.parse("2080-11-17"), NepaliDate.ofGregorian(2024, 2, 29));
    assertEquals(NepaliDate.parse("2056-11-17"), NepaliDate.ofGregorian(2000, 2, 29));
    Vectors.assertThrows(NepDateError.INVALID_GREGORIAN, () -> NepaliDate.ofGregorian(1900, 2, 29));
    assertEquals(NepaliDate.parse("2080-09-16"), NepaliDate.ofGregorian(2024, 1, 1));
    assertEquals(NepaliDate.parse("2080-09-17"), NepaliDate.ofGregorian(2024, 1, 2));
  }

  @Test
  void thePatternHandlesAnUnterminatedQuote() {
    assertEquals("abc", NepaliDate.parse("2081-04-15").format("'abc"));
    assertEquals("2081abc", NepaliDate.parse("2081-04-15").format("yyyy'abc"));
  }

  @Test
  void theFiscalYearGuardForTheLastQuarter() {
    Vectors.assertThrows(
        NepDateError.OUT_OF_RANGE, () -> FiscalYear.of(2199).quarterRange(Quarter.Q4));
    assertEquals(
        NepaliDateRange.of(NepaliDate.parse("2198-04-01"), NepaliDate.parse("2198-06-31")),
        FiscalYear.of(2198).quarterRange(Quarter.Q1));
  }

  @Test
  void theFiscalYearLabelHandlesAnyStartYear() {
    assertEquals("-1/00", FiscalYear.of(-1).label());
    assertEquals("-१/००", FiscalYear.of(-1).label(Language.NEPALI));
    assertEquals("2081/82", FiscalYear.of(2081).label());
    assertEquals("२०८१/८२", FiscalYear.of(2081).label(Language.NEPALI));
  }

  @Test
  void theSerializationGuardRefusesAClassOnlyStream() throws Exception {
    // A stream that names the class directly would bypass writeReplace, so readObject must refuse
    // it: an uninitialized NepaliDate would be an invalid date.
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bytes)) {
      out.writeShort(0xACED);
      out.writeShort(5);
      out.writeByte(0x73); // TC_OBJECT
      out.writeByte(0x72); // TC_CLASSDESC
      out.writeUTF("io.github.sanamhub.nepdate.java.NepaliDate");
      out.writeLong(1L);
      out.writeByte(0x02); // SC_SERIALIZABLE
      out.writeShort(0); // no fields
      out.writeByte(0x78); // TC_ENDBLOCKDATA, end of the class annotation
      out.writeByte(0x70); // TC_NULL, no super class descriptor
    }
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      java.io.InvalidObjectException failure =
          assertThrows(java.io.InvalidObjectException.class, in::readObject);
      assertEquals(true, failure.getMessage().contains("serialization proxy"));
    }
  }

  @Test
  void aClockWithoutAZoneStillWorks() {
    Clock clock = Clock.fixed(Instant.parse("2024-07-30T18:30:00Z"), ZoneOffset.UTC);
    assertEquals(NepaliDate.parse("2081-04-15"), NepaliDate.today(clock));
  }
}
