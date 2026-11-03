package io.github.sanamhub.nepdate.java.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The generator's own tests: the JSON reader, the renderers and the drift detector. */
class MainTest {
  private static final Path ROOT = Path.of(System.getProperty("nepdate.repoRoot", "."));

  @TempDir Path temp;

  @Test
  void theJsonReaderReadsEveryShape() {
    Object parsed =
        Json.parse(
            "{\"text\":\"a\\\"b\\\\c\\u0964\",\"number\":42,\"negative\":-7,\"real\":1.5,"
                + "\"true\":true,\"false\":false,\"none\":null,\"list\":[1,\"two\",null]}");
    Map<String, Object> object = CalendarData.object(parsed);
    assertEquals("a\"b\\c।", object.get("text"));
    assertEquals(42L, object.get("number"));
    assertEquals(-7L, object.get("negative"));
    assertEquals(1.5, object.get("real"));
    assertEquals(true, object.get("true"));
    assertEquals(false, object.get("false"));
    assertEquals(null, object.get("none"));
    List<Object> list = CalendarData.array(object.get("list"));
    assertEquals(3, list.size());
    assertEquals("two", list.get(1));
  }

  @Test
  void theJsonReaderRejectsBadInput() {
    assertThrows(IllegalStateException.class, () -> Json.parse("{} trailing"));
    assertThrows(IllegalStateException.class, () -> Json.parse("{\"a\":1,\"a\":2}"));
    assertThrows(IllegalStateException.class, () -> Json.parse("{\"a\":"));
    assertThrows(IllegalStateException.class, () -> Json.parse("[1,2"));
    assertThrows(IllegalStateException.class, () -> Json.parse("nope"));
  }

  @Test
  void theLiteralEscapesTheJavasSourceCharacters() {
    assertEquals("\"plain\"", PatroTableGenerator.literal("plain"));
    assertEquals("\"a\\\"b\"", PatroTableGenerator.literal("a\"b"));
    assertEquals("\"a\\\\b\"", PatroTableGenerator.literal("a\\b"));
    assertEquals("\"a\\nb\"", PatroTableGenerator.literal("a\nb"));
    assertEquals("\"नयाँ वर्ष\"", PatroTableGenerator.literal("नयाँ वर्ष"));
  }

  @Test
  void theCommittedTablesAreCurrent() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int status =
        Main.run(
            new String[] {"--check", "--root=" + ROOT.toAbsolutePath()},
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));
    assertEquals(0, status, err.toString(StandardCharsets.UTF_8));
    assertTrue(out.toString(StandardCharsets.UTF_8).contains("the tables are current"));
  }

  @Test
  void aTamperedTableFailsTheCheck() throws IOException {
    Path root = copyRepository();
    Path patro =
        root.resolve(
            "nepdate-patro/src/main/java/io/github/sanamhub/nepdate/java/patro/PatroTable.java");
    Files.writeString(
        patro,
        Files.readString(patro, StandardCharsets.UTF_8) + "\n// tampered\n",
        StandardCharsets.UTF_8);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int status =
        Main.run(
            new String[] {"--check", "--root=" + root},
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));
    assertEquals(1, status);
    assertTrue(err.toString(StandardCharsets.UTF_8).contains("is out of date"));
  }

  @Test
  void aSummaryThatDisagreesWithTheDataIsRefused() throws IOException {
    Path root = copyRepository();
    Path summary = root.resolve("shared/spec/vectors/patro-summary.json");
    String text = Files.readString(summary, StandardCharsets.UTF_8);
    Files.writeString(
        summary, text.replace("\"holidays\": 294", "\"holidays\": 295"), StandardCharsets.UTF_8);
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int status =
        Main.run(
            new String[] {"--check", "--root=" + root},
            new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));
    assertEquals(1, status);
    assertTrue(
        err.toString(StandardCharsets.UTF_8).contains("disagrees with the patro data"),
        err.toString(StandardCharsets.UTF_8));
  }

  @Test
  void anUnknownArgumentIsRefused() {
    assertEquals(
        2,
        Main.run(
            new String[] {"--wat"},
            new PrintStream(new ByteArrayOutputStream()),
            new PrintStream(new ByteArrayOutputStream())));
  }

  /** A temporary copy of the data and the two generated files, for the drift tests. */
  private Path copyRepository() throws IOException {
    Path root = temp.resolve("repo");
    copyTree(ROOT.resolve("shared"), root.resolve("shared"));
    for (String target :
        List.of(
            "nepdate/src/main/java/io/github/sanamhub/nepdate/java/CalendarTable.java",
            "nepdate-patro/src/main/java/io/github/sanamhub/nepdate/java/patro/PatroTable.java")) {
      Path destination = root.resolve(target);
      Files.createDirectories(destination.getParent());
      Files.copy(ROOT.resolve(target), destination);
    }
    return root;
  }

  private static void copyTree(Path source, Path target) throws IOException {
    try (Stream<Path> paths = Files.walk(source)) {
      for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
        Path destination = target.resolve(source.relativize(path).toString());
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
        } else {
          Files.createDirectories(destination.getParent());
          Files.copy(path, destination);
        }
      }
    }
  }
}
