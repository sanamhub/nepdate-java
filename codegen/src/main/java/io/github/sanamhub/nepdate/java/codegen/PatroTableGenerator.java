package io.github.sanamhub.nepdate.java.codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Renders the patro tables from the hub's per-year files and checks them against the summary. */
final class PatroTableGenerator {
  /** Assignments per generated helper method: a whole table in one initializer exceeds 64 KB. */
  private static final int CHUNK = 512;

  /** Name pairs per generated helper method. */
  private static final int NAME_CHUNK = 128;

  private PatroTableGenerator() {}

  record Named(String nepali, String english) {}

  record Day(String date, int serial, Named tithi, boolean holiday, List<Named> events) {}

  static String render(Path patroDirectory, Path summaryFile, CalendarData calendar)
      throws IOException {
    List<Day> days = readDays(patroDirectory, calendar);
    CalendarData.require(!days.isEmpty(), "the patro data has no days");

    Day first = days.get(0);
    Day last = days.get(days.size() - 1);
    int firstSerial = first.serial();
    int lastSerial = last.serial();

    List<Day> tithiDays = new ArrayList<>();
    List<Integer> holidays = new ArrayList<>();
    List<Day> eventDays = new ArrayList<>();
    for (Day day : days) {
      if (day.tithi() != null) {
        tithiDays.add(day);
      }
      if (day.holiday()) {
        holidays.add(day.serial());
      }
      if (!day.events().isEmpty()) {
        eventDays.add(day);
      }
    }
    CalendarData.require(!tithiDays.isEmpty(), "the patro data has no tithi");
    for (int i = 1; i < tithiDays.size(); i++) {
      CalendarData.require(
          tithiDays.get(i).serial() == tithiDays.get(i - 1).serial() + 1,
          "a gap in the tithi range before " + tithiDays.get(i).date());
    }

    // One name pool for tithi and events, in first-seen order, so a data update changes only the
    // entries it touches. The order is part of the generated file, not of the API.
    Map<Named, Integer> pool = new LinkedHashMap<>();
    for (Day day : days) {
      if (day.tithi() != null) {
        pool.putIfAbsent(day.tithi(), pool.size());
      }
      for (Named event : day.events()) {
        pool.putIfAbsent(event, pool.size());
      }
    }
    List<Named> names = new ArrayList<>(pool.keySet());

    int events = 0;
    for (Day day : eventDays) {
      events += day.events().size();
    }
    long nameBytes = 0;
    for (Named name : names) {
      nameBytes +=
          name.nepali().getBytes(StandardCharsets.UTF_8).length
              + name.english().getBytes(StandardCharsets.UTF_8).length;
    }

    Map<String, Object> expected = readSummary(summaryFile);
    Map<String, Object> actual = new LinkedHashMap<>();
    actual.put("schema", 1L);
    actual.put("first_year", (long) yearOf(first.date()));
    actual.put("last_year", (long) yearOf(last.date()));
    actual.put("first_day", first.date());
    actual.put("last_day", last.date());
    actual.put("first_serial", (long) firstSerial);
    actual.put("last_serial", (long) lastSerial);
    // The summary's `days` is the coverage span, not the number of days with metadata: a day
    // without a tithi, a holiday or an event is absent from the files (PATRO section 1).
    actual.put("days", (long) (lastSerial - firstSerial + 1));
    actual.put("tithi_first_day", tithiDays.get(0).date());
    actual.put("tithi_last_day", tithiDays.get(tithiDays.size() - 1).date());
    actual.put("tithi_first_serial", (long) tithiDays.get(0).serial());
    actual.put("tithi_days", (long) tithiDays.size());
    actual.put("tithi_names", tithiDays.stream().map(Day::tithi).distinct().count());
    actual.put("holidays", (long) holidays.size());
    actual.put("event_days", (long) eventDays.size());
    actual.put("events", (long) events);
    actual.put(
        "event_names", eventDays.stream().flatMap(day -> day.events().stream()).distinct().count());
    actual.put("names", (long) names.size());
    actual.put("name_bytes", nameBytes);
    compareSummary(expected, actual, summaryFile);

    short[] tithiIndex = new short[tithiDays.size()];
    int at = 0;
    for (Day day : tithiDays) {
      tithiIndex[at++] = (short) (int) pool.get(day.tithi());
    }
    int[] holidaySerials = new int[holidays.size()];
    for (int i = 0; i < holidays.size(); i++) {
      holidaySerials[i] = holidays.get(i);
    }
    int[] eventSerials = new int[eventDays.size()];
    int[] eventStart = new int[eventDays.size() + 1];
    short[] eventIndex = new short[events];
    int eventAt = 0;
    for (int i = 0; i < eventDays.size(); i++) {
      Day day = eventDays.get(i);
      eventSerials[i] = day.serial();
      eventStart[i] = eventAt;
      for (Named event : day.events()) {
        eventIndex[eventAt++] = (short) (int) pool.get(event);
      }
    }
    eventStart[eventDays.size()] = eventAt;

    StringBuilder out = new StringBuilder(160_000);
    out.append("// Generated by :codegen from shared/data/patro/*.json and\n")
        .append("// shared/spec/vectors/patro-summary.json. DO NOT EDIT.\n")
        .append("//\n")
        .append(
            "// The counts are checked against the hub summary by :codegen --check and by the\n")
        .append("// count tests. Change the data in the hub, then run ./gradlew :codegen:run.\n")
        .append("\n")
        .append("package io.github.sanamhub.nepdate.java.patro;\n")
        .append("\n")
        .append("/**\n")
        .append(
            " * The compiled patro (PATRO sections 1 to 3): one name pool, one index per day of the\n")
        .append(
            " * contiguous tithi range, and sorted sparse tables for holidays and event days.\n")
        .append(" */\n")
        .append("final class PatroTable {\n")
        .append("  /** Serial of 2077-01-01 and 2084-08-20, the days the patro covers. */\n")
        .append("  static final int FIRST_SERIAL = ")
        .append(firstSerial)
        .append(";\n")
        .append("  static final int LAST_SERIAL = ")
        .append(lastSerial)
        .append(";\n")
        .append("\n")
        .append("  /** Serial of the first day with a tithi, and the number of tithi days. */\n")
        .append("  static final int TITHI_FIRST_SERIAL = ")
        .append(tithiDays.get(0).serial())
        .append(";\n")
        .append("  static final int TITHI_DAYS = ")
        .append(tithiDays.size())
        .append(";\n")
        .append("\n")
        .append("  /** The counts shared/spec/vectors/patro-summary.json records. */\n")
        .append("  static final int TITHI_NAMES = ")
        .append(actual.get("tithi_names"))
        .append(";\n")
        .append("  static final int HOLIDAYS = ")
        .append(holidays.size())
        .append(";\n")
        .append("  static final int EVENT_DAYS = ")
        .append(eventDays.size())
        .append(";\n")
        .append("  static final int EVENTS = ")
        .append(events)
        .append(";\n")
        .append("  static final int EVENT_NAMES = ")
        .append(actual.get("event_names"))
        .append(";\n")
        .append("  static final int NAMES = ")
        .append(names.size())
        .append(";\n")
        .append("  static final int NAME_BYTES = ")
        .append(nameBytes)
        .append(";\n")
        .append("\n");
    appendNames(out, names);
    appendIntArray(
        out,
        "TITHI_INDEX",
        "TITHI_DAYS",
        "Pair index of each day from TITHI_FIRST_SERIAL on.",
        toInts(tithiIndex),
        "short");
    appendIntArray(
        out,
        "HOLIDAY_SERIALS",
        "HOLIDAYS",
        "Sorted serials of the public holidays.",
        holidaySerials,
        "int");
    appendIntArray(
        out,
        "EVENT_DAY_SERIALS",
        "EVENT_DAYS",
        "Sorted serials of the days with at least one event.",
        eventSerials,
        "int");
    appendIntArray(
        out,
        "EVENT_START",
        "EVENT_DAYS + 1",
        "Start in EVENT_INDEX for each entry of EVENT_DAY_SERIALS, plus the end.",
        eventStart,
        "int");
    appendIntArray(
        out,
        "EVENT_INDEX",
        "EVENTS",
        "Pair indices of every event, day by day in file order.",
        toInts(eventIndex),
        "short");
    out.append("  private PatroTable() {}\n}\n");
    return out.toString();
  }

  private static int[] toInts(short[] values) {
    int[] out = new int[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = values[i];
    }
    return out;
  }

  private static int yearOf(String date) {
    return Integer.parseInt(date.substring(0, 4));
  }

  private static List<Day> readDays(Path patroDirectory, CalendarData calendar) throws IOException {
    List<Path> files;
    try (Stream<Path> listing = Files.list(patroDirectory)) {
      files =
          listing
              .filter(path -> path.getFileName().toString().matches("\\d{4}\\.json"))
              .sorted()
              .toList();
    }
    CalendarData.require(!files.isEmpty(), "no patro files in " + patroDirectory);

    TreeMap<Integer, Day> days = new TreeMap<>();
    for (Path file : files) {
      Map<String, Object> root =
          CalendarData.object(Json.parse(Files.readString(file, StandardCharsets.UTF_8)));
      CalendarData.require(
          CalendarData.longValue(root, "schema") == 1L, file + ": schema is not 1");
      int year = CalendarData.intValue(root, "year");
      CalendarData.require(
          file.getFileName().toString().equals(year + ".json"), file + ": year is " + year);
      Map<String, Object> entries = CalendarData.object(root.get("days"));
      for (Map.Entry<String, Object> entry : entries.entrySet()) {
        String date = entry.getKey();
        String[] parts = date.split("-");
        CalendarData.require(parts.length == 3, file + ": bad date " + date);
        int y = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        int d = Integer.parseInt(parts[2]);
        CalendarData.require(y == year, file + ": " + date + " is not in " + year);
        CalendarData.require(calendar.valid(y, m, d), file + ": " + date + " is not a BS date");
        Map<String, Object> value = CalendarData.object(entry.getValue());
        Named tithi = null;
        if (value.get("tithi") != null) {
          tithi = named(CalendarData.object(value.get("tithi")), file, date);
        }
        boolean holiday = Boolean.TRUE.equals(value.get("holiday"));
        List<Named> events = new ArrayList<>();
        if (value.get("events") != null) {
          for (Object item : CalendarData.array(value.get("events"))) {
            events.add(named(CalendarData.object(item), file, date));
          }
        }
        int serial = calendar.serialOf(y, m, d);
        CalendarData.require(
            days.put(serial, new Day(date, serial, tithi, holiday, List.copyOf(events))) == null,
            "two entries for " + date);
      }
    }
    return List.copyOf(days.values());
  }

  private static Named named(Map<String, Object> value, Path file, String date) {
    String nepali = CalendarData.text(value, "np");
    String english = CalendarData.text(value, "en");
    CalendarData.require(!nepali.isEmpty(), file + ": " + date + " has an empty np name");
    CalendarData.require(!english.isEmpty(), file + ": " + date + " has an empty en name");
    return new Named(nepali, english);
  }

  private static Map<String, Object> readSummary(Path summaryFile) throws IOException {
    return CalendarData.object(Json.parse(Files.readString(summaryFile, StandardCharsets.UTF_8)));
  }

  private static void compareSummary(
      Map<String, Object> expected, Map<String, Object> actual, Path file) {
    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, Object> entry : expected.entrySet()) {
      String key = entry.getKey();
      Object want = entry.getValue();
      if (!actual.containsKey(key)) {
        problems.add(key + ": not computed by the generator");
        continue;
      }
      Object got = actual.get(key);
      boolean same =
          want instanceof Number number && got instanceof Number other
              ? number.longValue() == other.longValue()
              : want.equals(got);
      if (!same) {
        problems.add(key + ": summary says " + want + ", data says " + got);
      }
    }
    for (String key : actual.keySet()) {
      if (!expected.containsKey(key)) {
        problems.add(key + ": computed by the generator, not in the summary");
      }
    }
    if (!problems.isEmpty()) {
      throw new IllegalStateException(
          "codegen: "
              + file
              + " disagrees with the patro data:\n  "
              + String.join("\n  ", problems));
    }
  }

  private static void appendNames(StringBuilder out, List<Named> names) {
    out.append("  /** The name pool: NAMES (np, en) pairs, np at even indices and en at odd. */\n");
    out.append("  static final String[] NAMES_POOL = buildNamesPool();\n\n");
    out.append("  private static String[] buildNamesPool() {\n");
    out.append("    String[] names = new String[NAMES * 2];\n");
    int chunks = Math.max(1, (names.size() + NAME_CHUNK - 1) / NAME_CHUNK);
    for (int chunk = 0; chunk < chunks; chunk++) {
      out.append("    fillNames").append(chunk).append("(names);\n");
    }
    out.append("    return names;\n");
    out.append("  }\n\n");
    for (int chunk = 0; chunk < chunks; chunk++) {
      out.append("  private static void fillNames").append(chunk).append("(String[] names) {\n");
      int from = chunk * NAME_CHUNK;
      int to = Math.min(names.size(), from + NAME_CHUNK);
      for (int i = from; i < to; i++) {
        Named name = names.get(i);
        out.append("    names[")
            .append(2 * i)
            .append("] = ")
            .append(literal(name.nepali()))
            .append(";\n");
        out.append("    names[")
            .append(2 * i + 1)
            .append("] = ")
            .append(literal(name.english()))
            .append(";\n");
      }
      out.append("  }\n\n");
    }
  }

  private static void appendIntArray(
      StringBuilder out, String name, String size, String comment, int[] values, String type) {
    out.append("  /** ").append(comment).append(" */\n");
    out.append("  static final ")
        .append(type)
        .append("[] ")
        .append(name)
        .append(" = build")
        .append(name)
        .append("();\n\n");
    out.append("  private static ").append(type).append("[] build").append(name).append("() {\n");
    out.append("    ")
        .append(type)
        .append("[] values = new ")
        .append(type)
        .append('[')
        .append(size)
        .append("];\n");
    int chunks = Math.max(1, (values.length + CHUNK - 1) / CHUNK);
    for (int chunk = 0; chunk < chunks; chunk++) {
      out.append("    fill").append(name).append(chunk).append("(values);\n");
    }
    out.append("    return values;\n");
    out.append("  }\n\n");
    for (int chunk = 0; chunk < chunks; chunk++) {
      out.append("  private static void fill")
          .append(name)
          .append(chunk)
          .append('(')
          .append(type)
          .append("[] values) {\n");
      int from = chunk * CHUNK;
      int to = Math.min(values.length, from + CHUNK);
      for (int i = from; i < to; i++) {
        if ((i - from) % 8 == 0) {
          out.append("    ");
        }
        out.append("values[").append(i).append("] = ").append(values[i]).append(';');
        boolean endOfRow = (i - from) % 8 == 7 || i == to - 1;
        out.append(endOfRow ? "\n" : " ");
      }
      out.append("  }\n\n");
    }
  }

  static String literal(String value) {
    StringBuilder out = new StringBuilder(value.length() + 2);
    out.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> out.append(c);
      }
    }
    return out.append('"').toString();
  }
}
