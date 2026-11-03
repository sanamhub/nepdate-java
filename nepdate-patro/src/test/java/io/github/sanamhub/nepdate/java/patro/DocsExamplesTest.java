package io.github.sanamhub.nepdate.java.patro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs every {@code java} sample in the README and {@code docs/API.md}.
 *
 * <p>A sample is a fragment: the {@code import} lines are hoisted and the rest becomes the body of
 * a {@code main}. A line ending in {@code // => value} states what that line prints, and the sample
 * fails when stdout says something else. A sample without a marker must compile and run, which is
 * what the installation and type-only examples need.
 */
class DocsExamplesTest {
  private static final List<String> DOCS = List.of("README.md", "docs/API.md");

  @TempDir Path temp;

  @Test
  void everyJavaSampleCompilesRunsAndPrintsWhatItClaims() throws Exception {
    List<Sample> samples = new ArrayList<>();
    for (String document : DOCS) {
      samples.addAll(
          samples(
              Path.of(System.getProperty("nepdate.repoRoot", ".")).resolve(document), document));
    }
    assertTrue(
        samples.size() >= 12,
        "expected the documentation to hold samples, found " + samples.size());

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    List<Path> sources = new ArrayList<>();
    for (int i = 0; i < samples.size(); i++) {
      Path source = temp.resolve("Sample" + i + ".java");
      Files.writeString(source, samples.get(i).source(i), StandardCharsets.UTF_8);
      sources.add(source);
    }
    Path classes = temp.resolve("classes");
    Files.createDirectories(classes);
    List<String> arguments =
        new ArrayList<>(
            List.of("-d", classes.toString(), "-classpath", System.getProperty("java.class.path")));
    for (Path source : sources) {
      arguments.add(source.toString());
    }
    ByteArrayOutputStream errors = new ByteArrayOutputStream();
    int status =
        compiler.run(
            null,
            null,
            new PrintStream(errors, true, StandardCharsets.UTF_8),
            arguments.toArray(new String[0]));
    assertEquals(
        0,
        status,
        "a documentation sample does not compile:\n" + errors.toString(StandardCharsets.UTF_8));

    try (URLClassLoader loader =
        new URLClassLoader(new URL[] {classes.toUri().toURL()}, getClass().getClassLoader())) {
      for (int i = 0; i < samples.size(); i++) {
        Sample sample = samples.get(i);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        Throwable failure;
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
          System.setOut(capture);
          try {
            loader
                .loadClass("Sample" + i)
                .getMethod("main", String[].class)
                .invoke(null, (Object) new String[0]);
            failure = null;
          } catch (java.lang.reflect.InvocationTargetException error) {
            failure = error.getCause();
          } finally {
            System.setOut(previous);
          }
        }
        if (failure != null) {
          throw new AssertionError(sample.label() + " threw " + failure, failure);
        }
        List<String> printed = output.toString(StandardCharsets.UTF_8).lines().toList();
        assertEquals(
            sample.expected(),
            printed,
            sample.label() + " printed something else than its // => comments say");
      }
    }
  }

  private static List<Sample> samples(Path path, String label) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    List<Sample> samples = new ArrayList<>();
    int start = -1;
    List<String> block = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (start < 0 && line.trim().equals("```java")) {
        start = i + 1;
        block.clear();
      } else if (start >= 0 && line.trim().equals("```")) {
        samples.add(new Sample(label + ":" + start, block));
        start = -1;
      } else if (start >= 0) {
        block.add(line);
      }
    }
    if (start >= 0) {
      throw new IllegalStateException(label + ": unclosed ```java block at line " + start);
    }
    return samples;
  }

  /** One sample: its label, its code and what each {@code // =>} line claims. */
  private record Sample(String label, List<String> lines) {
    String source(int index) {
      StringBuilder out = new StringBuilder();
      out.append("import java.util.*;\n").append("import java.time.*;\n");
      out.append("import io.github.sanamhub.nepdate.java.*;\n");
      out.append("import io.github.sanamhub.nepdate.java.patro.*;\n");
      StringBuilder body = new StringBuilder();
      for (String line : lines) {
        String trimmed = line.trim();
        if (trimmed.startsWith("import ")) {
          out.append(trimmed).append('\n');
        } else {
          body.append(line).append('\n');
        }
      }
      out.append("public final class Sample").append(index).append(" {\n");
      out.append("  public static void main(String[] args) throws Exception {\n");
      out.append(body);
      out.append("  }\n}\n");
      return out.toString();
    }

    List<String> expected() {
      List<String> values = new ArrayList<>();
      for (String line : lines) {
        int marker = line.indexOf("// =>");
        if (marker >= 0) {
          values.add(line.substring(marker + 5).trim());
        }
      }
      return values;
    }
  }
}
