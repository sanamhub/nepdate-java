package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code csharp-golden/serialization.tsv}: the canonical text form and the Java hook. */
class SerializationVectorTest {
  private static final Vectors.Table SERIALIZATION =
      Vectors.table("spec/vectors/csharp-golden/serialization.tsv");

  static Stream<Arguments> rows() {
    return Vectors.arguments(SERIALIZATION.rows());
  }

  @Test
  void theFileHasTheExpectedRows() {
    assertEquals(List.of("kind", "input", "json", "result", "note"), SERIALIZATION.header());
    assertEquals(11, SERIALIZATION.rows().size());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyRowMatches(String[] row) throws Exception {
    String kind = SERIALIZATION.value(row, "kind");
    String input = SERIALIZATION.value(row, "input");
    String json = SERIALIZATION.value(row, "json");
    String result = SERIALIZATION.value(row, "result");

    switch (kind) {
      case "write" -> {
        NepaliDate date = NepaliDate.parse(input);
        assertEquals("\"" + date.toIsoString() + "\"", json);
        assertEquals(json, "\"" + serializedText(date) + "\"");
      }
      case "read" -> {
        if ("null".equals(input)) {
          // D-01: a null is not a date in this port, so the hook rejects it instead of building
          // the C# zero value. There is no Java value to read a null into.
          Vectors.assertThrows(NepDateError.INVALID_CHARACTER, () -> NepaliDate.parse("null"));
        } else if ("ERR".equals(result)) {
          String text = unquote(input);
          Vectors.assertThrows(NepDateError.INVALID_DAY, () -> NepaliDate.parse(text));
        } else {
          assertEquals(NepaliDate.parse(result), NepaliDate.parse(unquote(input)));
        }
      }
      case "roundtrip" -> {
        NepaliDate date = NepaliDate.parse(input);
        assertEquals(json, "\"" + date.toIsoString() + "\"");
        assertEquals(result, NepaliDate.parse(unquote(json)).toIsoString());
        assertEquals(date, deserialize(serialize(date)));
      }
      default -> throw new IllegalArgumentException("unknown kind " + kind);
    }
  }

  @Test
  void theStreamHoldsTheCanonicalText() throws Exception {
    byte[] stream = serialize(NepaliDate.parse("2081-04-15"));
    String bytes = new String(stream, java.nio.charset.StandardCharsets.ISO_8859_1);
    assertTrue(bytes.contains("2081-04-15"), "the stream should hold the readable canonical text");
    assertEquals(NepaliDate.parse("2081-04-15"), deserialize(stream));
  }

  @Test
  void aTamperedStreamIsRejectedByTheCanonicalParser() throws Exception {
    byte[] stream = serialize(NepaliDate.parse("2081-04-15"));
    String text = new String(stream, java.nio.charset.StandardCharsets.ISO_8859_1);
    byte[] broken =
        text.replace("2081-04-15", "2081-01-32")
            .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    try {
      deserialize(broken);
      throw new AssertionError("the stream should not read an invalid date");
    } catch (NepDateException expected) {
      assertEquals(NepDateError.INVALID_DAY, expected.error());
    } catch (java.io.InvalidObjectException expected) {
      assertInstanceOf(NepDateException.class, expected.getCause());
    }
  }

  private static String serializedText(NepaliDate date) throws Exception {
    String bytes = new String(serialize(date), java.nio.charset.StandardCharsets.ISO_8859_1);
    int start = bytes.indexOf(date.toIsoString());
    if (start < 0) {
      throw new AssertionError("the canonical text is not in the stream");
    }
    return bytes.substring(start, start + date.toIsoString().length());
  }

  private static byte[] serialize(NepaliDate date) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(date);
    }
    return bytes.toByteArray();
  }

  private static NepaliDate deserialize(byte[] bytes) throws Exception {
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return (NepaliDate) in.readObject();
    }
  }

  private static String unquote(String text) {
    return text.startsWith("\"") && text.endsWith("\"")
        ? text.substring(1, text.length() - 1)
        : text;
  }
}
