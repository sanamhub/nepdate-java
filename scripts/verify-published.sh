#!/usr/bin/env bash
# Installs one published version of both artifacts from Maven Central the way a user does, and
# checks it. release.yml calls it after publishing; a schedule calls it on Linux, macOS and Windows.
#
#   scripts/verify-published.sh 1.0.0-rc.1             download, compile and run a check
#   scripts/verify-published.sh 1.0.0-rc.1 --metadata   HEAD requests only, no download
set -euo pipefail

version="${1:-}"
mode="${2:-}"
if [ -z "$version" ]; then
  echo "usage: verify-published.sh VERSION [--metadata]"
  exit 1
fi

base="https://repo1.maven.org/maven2/io/github/sanamhub"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

for artifact in nepdate-java nepdate-java-patro; do
  for suffix in .pom .jar .jar.asc; do
    url="$base/$artifact/$version/$artifact-$version$suffix"
    code="$(curl -sS -L -o /dev/null -w '%{http_code}' "$url")"
    if [ "$code" != "200" ]; then
      echo "verify-published: $url returned $code"
      exit 1
    fi
  done
  curl -sSL -o "$tmp/$artifact.jar" "$base/$artifact/$version/$artifact-$version.jar"
  curl -sSL -o "$tmp/$artifact.pom" "$base/$artifact/$version/$artifact-$version.pom"
done

if [ "$mode" = "--metadata" ]; then
  echo "verify-published: $version is listed with both artifacts and both signatures"
  exit 0
fi

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    sep=';'
    # Git Bash hands javac an MSYS path in -cp, which Windows Java reads as C:\tmp and cannot find.
    # Convert the temp directory once, and pass every classpath entry as a Windows path.
    win_tmp="$(cygpath -w "$tmp")"
    ;;
  *)
    sep=':'
    win_tmp="$tmp"
    ;;
esac
classpath="$win_tmp/nepdate-java.jar${sep}$win_tmp/nepdate-java-patro.jar"

cat > "$tmp/Check.java" <<'JAVA'
import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.patro.CalendarInfo;
import io.github.sanamhub.nepdate.java.patro.Patro;
import java.time.LocalDate;
import java.util.List;

public final class Check {
  public static void main(String[] args) {
    NepaliDate date = NepaliDate.of(2081, 4, 15);
    if (!date.toLocalDate().equals(LocalDate.of(2024, 7, 30))) {
      throw new AssertionError("conversion: " + date.toLocalDate());
    }
    if (!date.toLongString().equals("Shrawan 15, 2081")) {
      throw new AssertionError("long form: " + date.toLongString());
    }
    if (!NepaliDate.parse("2081-04-15").equals(date)) {
      throw new AssertionError("parse round trip");
    }
    CalendarInfo info = Patro.info(date);
    List<String> events = info.eventsEn();
    if (!events.contains("Kheer Khane Din") || info.isPublicHoliday()) {
      throw new AssertionError("patro: " + events);
    }
    System.out.println("verify-published: converted a date and read a patro event");
  }
}
JAVA

javac -cp "$classpath" -d "$win_tmp" "$win_tmp/Check.java"
java -cp "$win_tmp${sep}$classpath" Check
echo "verify-published: $version ok"
