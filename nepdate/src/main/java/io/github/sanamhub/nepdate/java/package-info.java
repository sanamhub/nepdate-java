/**
 * Bikram Sambat (the Nepali calendar) dates for Java, and the operations on them.
 *
 * <p>The entry point is {@link io.github.sanamhub.nepdate.java.NepaliDate}: an immutable date from
 * BS 1901-01-01 to 2199-12-30 (AD 1844-04-11 to 2143-04-15) that converts to and from {@link
 * java.time.LocalDate}, does arithmetic with an explicit overflow mode, formats in English and
 * Nepali, parses strictly or leniently, and forms inclusive ranges. Tithi, public holidays and
 * events are the separate {@code nepdate-java-patro} artifact.
 *
 * <p>Every clock function takes a {@link java.time.ZoneId} or a {@link java.time.Clock}; nothing
 * reads a hidden local zone. Invalid dates cannot be constructed, so {@code null} is never a date
 * value and no method returns one.
 */
@NullMarked
package io.github.sanamhub.nepdate.java;

import org.jspecify.annotations.NullMarked;
