package io.github.sanamhub.nepdate.java.patro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the hub's patro summary and locates the shared directory under test. */
final class SharedData {
  private SharedData() {}

  static final Path SHARED =
      Path.of(System.getProperty("nepdate.shared", Path.of("..", "shared").toString()));

  static final Path ROOT = Path.of(System.getProperty("nepdate.repoRoot", Path.of(".").toString()));

  private static final Pattern ENTRY =
      Pattern.compile("\"([a-z_]+)\"\\s*:\\s*(?:\"([^\"]*)\"|(-?\\d+))");

  /**
   * The fields of {@code spec/vectors/patro-summary.json}.
   *
   * @return the values as text, numbers included
   */
  static Map<String, String> summary() {
    Path path = SHARED.resolve("spec/vectors/patro-summary.json");
    String text;
    try {
      text = Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException error) {
      throw new UncheckedIOException("cannot read " + path, error);
    }
    Map<String, String> values = new LinkedHashMap<>();
    Matcher matcher = ENTRY.matcher(text);
    while (matcher.find()) {
      values.put(matcher.group(1), matcher.group(2) != null ? matcher.group(2) : matcher.group(3));
    }
    if (values.isEmpty()) {
      throw new IllegalStateException(path + " has no fields");
    }
    return values;
  }

  /**
   * A summary value as an integer.
   *
   * @param summary the summary
   * @param key the field
   * @return the value
   */
  static int number(Map<String, String> summary, String key) {
    String value = summary.get(key);
    if (value == null) {
      throw new IllegalArgumentException("the summary has no " + key);
    }
    return Integer.parseInt(value);
  }
}
