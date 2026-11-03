# ADR-0011: JPMS names and the package suffix

- **Status:** accepted
- **Date:** 2026-11-03

## Context

The Kotlin port publishes `io.github.sanamhub:nepdate` with package `io.github.sanamhub.nepdate`.
This port must be usable from the same application, so the two cannot share a package name, and a
Java 17 library is expected to declare its modules. The owner's decision recorded the Maven
coordinates and module names before this repository existed.

## Decision

1. **Artifacts** `io.github.sanamhub:nepdate-java` and `io.github.sanamhub:nepdate-java-patro`.
2. **Packages** `io.github.sanamhub.nepdate.java` and `io.github.sanamhub.nepdate.java.patro`. The
   suffix is `.java`, so no package is split with the Kotlin port.
3. **JPMS modules** with the same names, declared in a real `module-info.java` in each project. The
   core exports one package and `requires static org.jspecify`; the patro `requires transitive` the
   core and exports its package. The module names are not the artifact names, which is allowed and
   documented in the README.
4. **Automatic-Module-Name is not needed,** because each jar has a module descriptor. A classpath
   user is unaffected; a module-path user gets the two modules by name.
5. **Javadoc runs in module mode** so the descriptor is checked by `-Xdoclint:all -Werror` and the
   Javadoc jar describes the same modules.

## Consequences

- Both ports coexist on a classpath and on a module path.
- An Android build without `java.time` uses the Kotlin port; this one requires Java 17.
- A new public package would need a module export and a README sentence, which is a deliberate cost.

## Alternatives considered

- **The Kotlin names:** a split package, which the module system rejects and which breaks a JPMS
  application that reads both.
- **`io.github.sanamhub.nepdate.javaport`:** longer with no gain; `.java` is what the owner chose.
- **No `module-info.java`:** the task requires a real descriptor, and an automatic module name is
  less useful to a module-path user.
