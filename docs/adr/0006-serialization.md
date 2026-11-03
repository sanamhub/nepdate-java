# ADR-0006: Serialization is the canonical text

- **Status:** accepted
- **Date:** 2026-11-03

## Context

Hub `spec/PARITY.md` D-11 removes the C# object form, the Newtonsoft and XML adapters and the
`TypeConverter` type, and keeps two things: the canonical `YYYY-MM-DD` text and the `YYYYMMDD`
integer. Each port uses its language's standard serialization hook. Java has no JSON library in the
JDK, and adding one would break the no-runtime-dependency rule; `java.io.Serializable` is the
language's own hook.

## Decision

1. **`toString()` stays `YYYY/MM/DD`,** the C# display form that the shared vectors record, and
   `toIsoString()` is the canonical `YYYY-MM-DD`. `parse` accepts both, so a round trip through
   either form is exact.
2. **`NepaliDate implements Serializable` through a serialization proxy.** `writeReplace` returns a
   private proxy holding the canonical text, and `readResolve` calls `parse`. The stream therefore
   contains a readable `2081-04-15`, not an internal serial or packed integer, and a stream from
   another version of the library reads correctly.
3. **`readObject` refuses a stream that names the class directly.** Without the guard a hand-built
   stream could create an uninitialized `NepaliDate` whose fields are zero, which is exactly the
   invalid default hub D-01 forbids. `CoverageEdgesTest` builds that stream and asserts the refusal.
4. **The integer form is `toYmd()` and `ofYmd(int)`.** Every `int` is accepted and none wraps: the
   value is split into year, month and day and then validated.
5. **No JSON adapter is shipped.** A user who needs JSON writes `toIsoString()` and reads with
   `parse`, or uses a library with a custom serializer in their own code. The canonical text is the
   interchange format, and a JSON adapter would be an API to keep compatible for no cross-language
   gain.

## Consequences

- A serialized date is portable between this port and any other that reads the canonical text.
- The serialized form is stable across releases and auditable.
- `toString` and the canonical form differ, which `docs/API.md` states next to both.

## Alternatives considered

- **Serializing the fields:** smaller streams, but the form depends on the internal representation
  and cannot be read by another port.
- **Java's `Externalizable`:** more code for the same text and no benefit.
- **Shipping a JSON adapter for Jackson or Gson:** a runtime dependency or a compile-only adapter
  that users still have to register; the canonical text plus `parse` is the smaller contract.
