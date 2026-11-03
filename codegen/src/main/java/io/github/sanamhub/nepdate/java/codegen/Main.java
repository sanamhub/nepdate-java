package io.github.sanamhub.nepdate.java.codegen;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Writes the calendar and patro tables from the hub's shared data.
 *
 * <pre>{@code
 * ./gradlew :codegen:run             # write both files
 * ./gradlew :codegen:run --args=--check   # fail when a file is out of date
 * }</pre>
 */
public final class Main {
  private static final String CALENDAR_TABLE =
      "nepdate/src/main/java/io/github/sanamhub/nepdate/java/CalendarTable.java";
  private static final String PATRO_TABLE =
      "nepdate-patro/src/main/java/io/github/sanamhub/nepdate/java/patro/PatroTable.java";

  private Main() {}

  public static void main(String[] args) {
    int code = run(args, System.out, System.err);
    if (code != 0) {
      System.exit(code);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    boolean check = false;
    Path root = Path.of("").toAbsolutePath();
    for (String arg : args) {
      if ("--check".equals(arg)) {
        check = true;
      } else if (arg.startsWith("--root=")) {
        root = Path.of(arg.substring("--root=".length())).toAbsolutePath();
      } else {
        err.println("codegen: unknown argument " + arg);
        return 2;
      }
    }
    try {
      CalendarData calendar =
          CalendarData.read(root.resolve("shared/data/calendar/bs-calendar.json"));
      String calendarTable = CalendarTableGenerator.render(calendar);
      String patroTable =
          PatroTableGenerator.render(
              root.resolve("shared/data/patro"),
              root.resolve("shared/spec/vectors/patro-summary.json"),
              calendar);
      boolean current = true;
      current &= publish(root, root.resolve(CALENDAR_TABLE), calendarTable, check, out, err);
      current &= publish(root, root.resolve(PATRO_TABLE), patroTable, check, out, err);
      if (!current) {
        return 1;
      }
      out.println(check ? "codegen: the tables are current" : "codegen: the tables are written");
      return 0;
    } catch (IOException | RuntimeException error) {
      err.println(error.getMessage() == null ? error.toString() : error.getMessage());
      return 1;
    }
  }

  private static boolean publish(
      Path root, Path target, String content, boolean check, PrintStream out, PrintStream err)
      throws IOException {
    byte[] expected = content.getBytes(StandardCharsets.UTF_8);
    String shown = root.relativize(target).toString().replace('\\', '/');
    if (Files.exists(target) && Arrays.equals(Files.readAllBytes(target), expected)) {
      out.println("codegen: " + shown + " is current");
      return true;
    }
    if (check) {
      err.println("codegen: " + shown + " is out of date; run ./gradlew :codegen:run");
      if (Files.exists(target)) {
        err.println("codegen: first difference: " + firstDifference(target, content));
      }
      return false;
    }
    Files.createDirectories(target.getParent());
    Files.write(target, expected);
    out.println("codegen: wrote " + shown);
    return true;
  }

  private static String firstDifference(Path target, String expected) throws IOException {
    String[] actual = Files.readString(target, StandardCharsets.UTF_8).split("\n", -1);
    String[] wanted = expected.split("\n", -1);
    for (int line = 0; line < Math.min(actual.length, wanted.length); line++) {
      if (!actual[line].equals(wanted[line])) {
        return "line "
            + (line + 1)
            + ": expected \""
            + wanted[line]
            + "\", found \""
            + actual[line]
            + "\"";
      }
    }
    return "the file ends after " + Math.min(actual.length, wanted.length) + " lines";
  }
}
