/**
 * Bikram Sambat (the Nepali calendar) dates for Java.
 *
 * <p>The module exports one package and has no runtime dependency: {@code org.jspecify} is a
 * compile-only annotation library, so nothing is needed on the module path at run time.
 */
module io.github.sanamhub.nepdate.java {
  requires static org.jspecify;

  exports io.github.sanamhub.nepdate.java;
}
