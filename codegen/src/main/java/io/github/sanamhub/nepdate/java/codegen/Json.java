package io.github.sanamhub.nepdate.java.codegen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader for the shared data files. The generator reads files whose shape is fixed
 * by the hub, so it supports exactly the JSON those files use: objects, arrays, strings, numbers,
 * booleans and null. Objects keep their key order, which makes the generated name pool stable.
 */
final class Json {
  private final String text;
  private int index;

  private Json(String text) {
    this.text = text;
  }

  static Object parse(String text) {
    Json parser = new Json(text);
    parser.skipWhitespace();
    Object value = parser.value();
    parser.skipWhitespace();
    if (parser.index != text.length()) {
      throw parser.error("unexpected text after the value");
    }
    return value;
  }

  private IllegalStateException error(String message) {
    int line = 1;
    int column = 1;
    for (int i = 0; i < index && i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        line++;
        column = 1;
      } else {
        column++;
      }
    }
    return new IllegalStateException(message + " at line " + line + ", column " + column);
  }

  private void skipWhitespace() {
    while (index < text.length()) {
      char c = text.charAt(index);
      if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
        index++;
      } else {
        break;
      }
    }
  }

  private Object value() {
    if (index >= text.length()) {
      throw error("unexpected end of input");
    }
    char c = text.charAt(index);
    return switch (c) {
      case '{' -> object();
      case '[' -> array();
      case '"' -> string();
      case 't' -> literal("true", Boolean.TRUE);
      case 'f' -> literal("false", Boolean.FALSE);
      case 'n' -> literal("null", null);
      default -> number();
    };
  }

  private Object literal(String word, Object value) {
    if (!text.startsWith(word, index)) {
      throw error("expected " + word);
    }
    index += word.length();
    return value;
  }

  private Map<String, Object> object() {
    expect('{');
    Map<String, Object> result = new LinkedHashMap<>();
    skipWhitespace();
    if (peek() == '}') {
      index++;
      return result;
    }
    while (true) {
      skipWhitespace();
      if (peek() != '"') {
        throw error("expected a key");
      }
      String key = string();
      skipWhitespace();
      expect(':');
      skipWhitespace();
      Object value = value();
      if (result.put(key, value) != null) {
        throw error("duplicate key " + key);
      }
      skipWhitespace();
      char next = peek();
      if (next == ',') {
        index++;
      } else if (next == '}') {
        index++;
        return result;
      } else {
        throw error("expected , or }");
      }
    }
  }

  private List<Object> array() {
    expect('[');
    List<Object> result = new ArrayList<>();
    skipWhitespace();
    if (peek() == ']') {
      index++;
      return result;
    }
    while (true) {
      skipWhitespace();
      result.add(value());
      skipWhitespace();
      char next = peek();
      if (next == ',') {
        index++;
      } else if (next == ']') {
        index++;
        return result;
      } else {
        throw error("expected , or ]");
      }
    }
  }

  private String string() {
    expect('"');
    StringBuilder out = new StringBuilder();
    while (true) {
      if (index >= text.length()) {
        throw error("unterminated string");
      }
      char c = text.charAt(index++);
      if (c == '"') {
        return out.toString();
      }
      if (c != '\\') {
        out.append(c);
        continue;
      }
      if (index >= text.length()) {
        throw error("unterminated escape");
      }
      char escaped = text.charAt(index++);
      switch (escaped) {
        case '"' -> out.append('"');
        case '\\' -> out.append('\\');
        case '/' -> out.append('/');
        case 'b' -> out.append('\b');
        case 'f' -> out.append('\f');
        case 'n' -> out.append('\n');
        case 'r' -> out.append('\r');
        case 't' -> out.append('\t');
        case 'u' -> {
          if (index + 4 > text.length()) {
            throw error("short unicode escape");
          }
          out.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
          index += 4;
        }
        default -> throw error("unknown escape \\" + escaped);
      }
    }
  }

  private Number number() {
    int start = index;
    if (peek() == '-') {
      index++;
    }
    while (index < text.length() && Character.isDigit(text.charAt(index))) {
      index++;
    }
    boolean fractional = false;
    if (index < text.length() && text.charAt(index) == '.') {
      fractional = true;
      index++;
      while (index < text.length() && Character.isDigit(text.charAt(index))) {
        index++;
      }
    }
    if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
      fractional = true;
      index++;
      if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
        index++;
      }
      while (index < text.length() && Character.isDigit(text.charAt(index))) {
        index++;
      }
    }
    String literal = text.substring(start, index);
    if (literal.isEmpty() || literal.equals("-")) {
      throw error("expected a value");
    }
    if (fractional) {
      return Double.valueOf(literal);
    }
    try {
      return Long.valueOf(literal);
    } catch (NumberFormatException overflow) {
      return Double.valueOf(literal);
    }
  }

  private char peek() {
    if (index >= text.length()) {
      throw error("unexpected end of input");
    }
    return text.charAt(index);
  }

  private void expect(char expected) {
    if (peek() != expected) {
      throw error("expected " + expected);
    }
    index++;
  }
}
