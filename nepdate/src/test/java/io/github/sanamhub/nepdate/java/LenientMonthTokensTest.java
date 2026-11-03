package io.github.sanamhub.nepdate.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every accepted month spelling of PARSING section 3, read from the specification table. */
class LenientMonthTokensTest {
  @Test
  void everySpellingInTheSpecificationParses() throws IOException {
    List<String> lines =
        Files.readAllLines(Vectors.SHARED.resolve("spec/PARSING.md"), StandardCharsets.UTF_8);
    assertEquals(
        true,
        lines.stream().anyMatch(line -> line.contains("Accepted tokens")),
        "no month table in "
            + Vectors.SHARED.resolve("spec/PARSING.md")
            + " ("
            + lines.size()
            + " lines)");
    int checked = 0;
    boolean inTable = false;
    for (String line : lines) {
      if (line.contains("Accepted tokens")) {
        inTable = true;
        continue;
      }
      if (!inTable) {
        continue;
      }
      if (!line.startsWith("|")) {
        break;
      }
      String[] cells = line.split("\\|", -1);
      if (cells.length < 4) {
        continue;
      }
      String monthText = cells[1].trim();
      if (!monthText.matches("\\d+")) {
        continue;
      }
      int month = Integer.parseInt(monthText);
      for (String token : cells[2].split(",")) {
        String spelling = token.trim();
        if (spelling.isEmpty()) {
          continue;
        }
        NepaliDate date = NepaliDate.parseLenient("15 " + spelling + " 2080");
        assertEquals(month, date.monthNumber(), "\"" + spelling + "\" should be month " + month);
        assertEquals(15, date.day(), spelling);
        assertEquals(2080, date.year(), spelling);
        checked++;
      }
    }
    assertEquals(true, checked >= 70, "the table lists about 80 spellings, found " + checked);
  }

  @Test
  void aDotOnlyInputIsUnrecognized() {
    Vectors.assertThrows(NepDateError.UNRECOGNIZED, () -> NepaliDate.parseLenient("."));
    Vectors.assertThrows(NepDateError.UNRECOGNIZED, () -> NepaliDate.parseLenient(".."));
    Vectors.assertThrows(NepDateError.UNRECOGNIZED, () -> NepaliDate.parseLenient(".,"));
  }

  @Test
  void everyLenientInputIsDeterministic() {
    for (String input : List.of("15 Shrawan 2080", "2080/05/15", "not a date", "")) {
      boolean first = NepaliDate.tryParseLenient(input).isPresent();
      for (int i = 0; i < 3; i++) {
        assertEquals(first, NepaliDate.tryParseLenient(input).isPresent());
      }
    }
  }
}
