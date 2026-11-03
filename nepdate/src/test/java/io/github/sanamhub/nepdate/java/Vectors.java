package io.github.sanamhub.nepdate.java;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads the hub's test vectors from {@code shared/}, the read-only copy of the specification and
 * data that every port keeps.
 *
 * <p>A file is tab separated (the CSV file is comma separated). A line starting with {@code #} is a
 * comment. Nothing is unquoted: {@code serialization.tsv} holds literal JSON text, quotes included.
 */
final class Vectors {
  private Vectors() {}

  /** The directory the build passes as {@code -Dnepdate.shared}. */
  static final Path SHARED =
      Path.of(System.getProperty("nepdate.shared", Path.of("..", "shared").toString()));

  /** The repository root the build passes as {@code -Dnepdate.repoRoot}. */
  static final Path ROOT = Path.of(System.getProperty("nepdate.repoRoot", Path.of(".").toString()));

  /** One vector file: its header and its data rows. */
  record Table(String name, List<String> header, List<String[]> rows) {
    int column(String column) {
      int index = header.indexOf(column);
      if (index < 0) {
        throw new IllegalArgumentException(name + " has no column " + column + ": " + header);
      }
      return index;
    }

    String value(String[] row, String column) {
      return row[column(column)];
    }
  }

  /**
   * Reads a tab-separated vector.
   *
   * @param relativePath the path under {@code shared/}
   * @return the header and the data rows
   */
  static Table table(String relativePath) {
    return read(relativePath, '\t');
  }

  /**
   * Reads {@code month-boundaries.csv}.
   *
   * @return the header and the 7176 month-boundary rows
   */
  static Table monthBoundaries() {
    return read("spec/vectors/month-boundaries.csv", ',');
  }

  private static Table read(String relativePath, char separator) {
    Path path = SHARED.resolve(relativePath);
    List<String> lines;
    try {
      lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    } catch (IOException error) {
      throw new UncheckedIOException("cannot read " + path, error);
    }
    List<String> header = null;
    List<String[]> rows = new ArrayList<>();
    for (String line : lines) {
      if (line.startsWith("#") || line.isBlank()) {
        continue;
      }
      String[] fields = split(line, separator);
      if (header == null) {
        header = Arrays.asList(fields);
      } else {
        rows.add(fields);
      }
    }
    if (header == null) {
      throw new IllegalArgumentException(path + " is empty");
    }
    return new Table(relativePath, header, List.copyOf(rows));
  }

  private static String[] split(String line, char separator) {
    List<String> fields = new ArrayList<>();
    int start = 0;
    for (int i = 0; i < line.length(); i++) {
      if (line.charAt(i) == separator) {
        fields.add(line.substring(start, i));
        start = i + 1;
      }
    }
    fields.add(line.substring(start));
    return fields.toArray(new String[0]);
  }

  /**
   * The rows of a vector as parameterized-test arguments, one row per invocation.
   *
   * @param rows the data rows
   * @return the arguments
   */
  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> arguments(
      List<String[]> rows) {
    return rows.stream().map(row -> org.junit.jupiter.params.provider.Arguments.of((Object) row));
  }

  /**
   * A BS date from a vector field, or null for the {@code ERR} marker.
   *
   * @param field the field
   * @return the date, or null
   */
  static NepaliDate dateOrNull(String field) {
    return "ERR".equals(field) ? null : NepaliDate.parse(field);
  }

  /**
   * Asserts that an operation fails with a date exception.
   *
   * @param error the expected kind
   * @param operation the operation
   */
  static void assertThrows(NepDateError error, Runnable operation) {
    try {
      operation.run();
    } catch (NepDateException failure) {
      if (failure.error() != error) {
        throw new AssertionError(
            "expected " + error + ", got " + failure.error() + ": " + failure.getMessage(),
            failure);
      }
      return;
    }
    throw new AssertionError("expected NepDateException " + error + ", but nothing was thrown");
  }

  /**
   * A range written {@code first..last} in a vector.
   *
   * @param text the field
   * @return the range
   */
  static NepaliDateRange range(String text) {
    String[] parts = text.split("\\.\\.");
    if (parts.length != 2) {
      throw new IllegalArgumentException("not a range: " + text);
    }
    return NepaliDateRange.of(NepaliDate.parse(parts[0]), NepaliDate.parse(parts[1]));
  }

  /**
   * The vector's text for a range: {@code first..last}.
   *
   * @param range the range
   * @return the text
   */
  static String describe(NepaliDateRange range) {
    return range.start().toIsoString() + ".." + range.end().toIsoString();
  }

  /**
   * The vector's text for a list of dates: semicolon separated, or {@code none} when empty.
   *
   * @param dates the dates
   * @return the text
   */
  static String describe(java.util.List<NepaliDate> dates) {
    if (dates.isEmpty()) {
      return "none";
    }
    StringBuilder out = new StringBuilder();
    for (NepaliDate date : dates) {
      if (!out.isEmpty()) {
        out.append(';');
      }
      out.append(date.toIsoString());
    }
    return out.toString();
  }

  /**
   * The vector's text for a list of ranges: semicolon separated, or {@code none} when empty.
   *
   * @param ranges the ranges
   * @return the text
   */
  static String describeRanges(java.util.List<NepaliDateRange> ranges) {
    if (ranges.isEmpty()) {
      return "none";
    }
    StringBuilder out = new StringBuilder();
    for (NepaliDateRange range : ranges) {
      if (!out.isEmpty()) {
        out.append(';');
      }
      out.append(describe(range));
    }
    return out.toString();
  }

  /**
   * The vector's {@code DateOrder} name, for example {@code Ymd}.
   *
   * @param name the field
   * @return the order
   */
  static DateOrder order(String name) {
    return DateOrder.valueOf(name.toUpperCase(java.util.Locale.ROOT));
  }

  /**
   * The vector's separator name, for example {@code Dash}.
   *
   * @param name the field
   * @return the separator
   */
  static Separator separator(String name) {
    return Separator.valueOf(name.toUpperCase(java.util.Locale.ROOT));
  }

  /**
   * The vector's language name, {@code en} or {@code ne}.
   *
   * @param name the field
   * @return the language
   */
  static Language language(String name) {
    return switch (name) {
      case "en" -> Language.ENGLISH;
      case "ne" -> Language.NEPALI;
      default -> throw new IllegalArgumentException("not a language: " + name);
    };
  }

  /**
   * The vector's error kind name, for example {@code OutOfRange}.
   *
   * @param name the field
   * @return the error
   */
  static NepDateError error(String name) {
    return switch (name) {
      case "OutOfRange" -> NepDateError.OUT_OF_RANGE;
      case "InvalidMonth" -> NepDateError.INVALID_MONTH;
      case "InvalidDay" -> NepDateError.INVALID_DAY;
      case "InvalidGregorian" -> NepDateError.INVALID_GREGORIAN;
      case "InvalidCharacter" -> NepDateError.INVALID_CHARACTER;
      case "WrongGroupCount" -> NepDateError.WRONG_GROUP_COUNT;
      case "NumberTooLong" -> NepDateError.NUMBER_TOO_LONG;
      case "Ambiguous" -> NepDateError.AMBIGUOUS;
      case "Unrecognized" -> NepDateError.UNRECOGNIZED;
      default -> throw new IllegalArgumentException("not an error kind: " + name);
    };
  }
}
