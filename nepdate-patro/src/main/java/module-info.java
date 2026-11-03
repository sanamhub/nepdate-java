/**
 * Tithi, public holidays and events keyed by Bikram Sambat day.
 *
 * <p>The module requires the core module, which the core never requires back.
 */
module io.github.sanamhub.nepdate.java.patro {
  requires transitive io.github.sanamhub.nepdate.java;
  requires static org.jspecify;

  exports io.github.sanamhub.nepdate.java.patro;
}
