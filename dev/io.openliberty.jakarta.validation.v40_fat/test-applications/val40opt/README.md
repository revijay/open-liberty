# Jakarta Validation 4.0 — `Optional` Violation Path Change (Section 4.3)

## Overview

Jakarta Validation 4.0 (section 4.3) removes the requirement for the built-in
`Optional` value extractor to pass `null` as the node name when reporting a
constraint violation on the value inside an `Optional` field.

The practical effect: the violation path for an `Optional`-wrapped field changes
from the bare field name to a container-element path that exposes the `Optional`
wrapper — matching how `List`, `Map`, and other container types are already handled.

---

## Background — Value Extractors and Violation Paths

Jakarta Validation supports **value extractors**: pluggable components that tell
the validator how to "unwrap" a container type and apply constraints to the value
inside it. The spec ships a set of built-in extractors for `Optional`, `List`,
`Map`, `Iterable`, and others.

When a constraint violation is reported for a value that was extracted from a
container, the violation **path** includes a node for the container element.
For a `List<String> tags` field with a `@NotBlank` violation on element 0,
the path is `"tags[0]"`. The `[0]` part comes from the extractor declaring the
index as part of the container node.

---

## The Pre-4.0 `Optional` Special Case

Before 4.0, the spec required the built-in `Optional` extractor to pass
**`null` as the node name** to its extraction callback. That `null` instructed
the path builder to suppress the container node entirely, making `Optional`
invisible in the resulting path.

```
field:   Optional<String> nickname  (annotated @NotBlank)
value:   Optional.empty()  →  @NotBlank fails

Pre-4.0 violation path:  "nickname"
```

The `Optional` wrapper did not appear. The path looked identical to what you
would get from a plain `String nickname` field.

---

## What 4.0 Changes

The `null`-node requirement is **removed**. The `Optional` extractor is now
treated like any other container extractor. The container node appears in the
path using the standard `<optional value>` node name defined by the spec.

```
Post-4.0 violation path:  "nickname.<optional value>"
```

This aligns `Optional` with the rest of the container types and makes the path
unambiguous — a reader of the violation can tell at a glance that the failing
value was inside an `Optional`.

---

## Migration Impact

This is a **breaking change** for code that compares or parses violation paths
programmatically (e.g. UI frameworks that map path strings to form field names,
or test assertions that check exact path values).

```java
// Before 4.0 — violation path was the bare field name
assertEquals("nickname", violation.getPropertyPath().toString());

// After 4.0 — violation path includes the container node
assertEquals("nickname.<optional value>", violation.getPropertyPath().toString());
```

### What does NOT change

- A constraint placed on the **`Optional` reference itself** (e.g. `@NotNull` to
  forbid a `null` reference) does not involve value extraction. The path is always
  the bare field name in both spec versions.
- `Optional.empty()` satisfies `@NotNull` on the reference because `Optional.empty()`
  is a valid, non-null object.
- Constraints on non-`Optional` fields are completely unaffected.

---

## Sample Application

The `val40opt` test application in `io.openliberty.jakarta.validation.v40_fat`
demonstrates the path change end-to-end on Liberty.

### Project structure

```
test-applications/val40opt/
├── resources/WEB-INF/
│   └── beans.xml                              ← CDI bean-discovery-mode="all"
└── src/val40opt/web/
    ├── Person.java                            ← bean under test (4 Optional fields)
    └── OptionalViolationPathTestServlet.java  ← 9 FAT tests across 3 sections
```

### The `Person` bean

```java
// Core scenario — path changes between spec versions
@NotBlank
private final Optional<String> nickname;

// Same path change with a different constraint
@Email
private final Optional<String> email;

// Optional IS present but inner value fails @Size
@Size(min = 10, max = 200)
private final Optional<String> bio;

// @NotNull on the wrapper itself — path is ALWAYS the bare field name
@NotNull
private final Optional<String> mandatoryField;
```

---

## Test Cases

### Section 1 — Pre-4.0 behaviour (`testPre40_*`)

These tests assert the **old path format** — field name only, no `<optional value>` node.
They **pass today** against `validation-3.1`.

| Test | Scenario | Expected path |
|---|---|---|
| `testPre40_EmptyOptional_PathIsFieldName` | `nickname` is `Optional.empty()` → `@NotBlank` fails | `"nickname"` |
| `testPre40_InvalidEmail_PathIsFieldName` | `email` contains an invalid string → `@Email` fails | `"email"` |
| `testPre40_SizeViolation_PathIsFieldName` | `bio` value is too short → `@Size` fails | `"bio"` |

### Section 2 — Post-4.0 behaviour (`testPost40_*`)

These tests assert the **new path format** with the container node exposed.
They **fail today** against `validation-3.1` and will **pass** once the 4.0
runtime is active.

| Test | Scenario | Expected path |
|---|---|---|
| `testPost40_EmptyOptional_PathIncludesOptionalNode` | Same as Section 1 row 1 | `"nickname.<optional value>"` |
| `testPost40_InvalidEmail_PathIncludesOptionalNode` | Same as Section 1 row 2 | `"email.<optional value>"` |
| `testPost40_SizeViolation_PathIncludesOptionalNode` | Same as Section 1 row 3 | `"bio.<optional value>"` |

> **Note:** Section 1 and Section 2 tests use **identical inputs**. Only the
> asserted path string differs. This makes the set of tests self-documenting:
> Section 1 shows what the runtime produces today; Section 2 shows what it must
> produce after the 4.0 change.

### Section 3 — Edge cases (identical in both spec versions)

| Test | What it verifies |
|---|---|
| `testNoViolations_ValidPerson` | A fully valid `Person` produces zero violations |
| `testNotNullOnOptionalReference_PathIsAlwaysFieldName` | `@NotNull` on the `Optional` reference → path is always `"mandatoryField"`, never includes `<optional value>` |
| `testOptionalEmpty_NotNullOnReference_NoViolation` | `Optional.empty()` satisfies `@NotNull` on the reference — it is not null |
| `testMultipleViolations_EachFieldReportsOwnPath` | Two failing fields → two violations, each pointing to its own field |

---

## Current Status

| Section | `validation-3.1` (today) | `validation-4.0` (future) |
|---|---|---|
| Section 1 (`testPre40_*`) | ✅ Pass | ❌ Fail — path changed |
| Section 2 (`testPost40_*`) | ❌ Fail — path not yet changed | ✅ Pass |
| Section 3 (edge cases) | ✅ Pass | ✅ Pass |

The Section 2 failures are **intentional**. They act as a regression gate that
flips green once Hibernate Validator ships the updated `Optional` extractor and
the `validation-4.0` feature is activated in the server configuration.

---

## Build and Run

```bash
cd dev
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun
```

To run only this test class in isolation:

```bash
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun \
    -Dfat.test.class=io.openliberty.jakarta.validation.v40.fat.OptionalViolationPathTest
```

---

## References

- [Jakarta Validation 4.0 specification, section 4.3](https://jakarta.ee/specifications/bean-validation/)
- [`ExtractedValue` Javadoc — `addContainerElementNode` / node name](https://jakarta.ee/specifications/bean-validation/4.0/apidocs/)
- [`ValueExtractor` SPI](https://jakarta.ee/specifications/bean-validation/4.0/apidocs/jakarta/validation/valueextraction/ValueExtractor)
