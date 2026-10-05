# Jakarta Validation 4.0 — Cascade `@Valid`: Legacy vs. Type-Argument Form (Issue #260)

## Overview

Jakarta Validation 4.0 (issue #260) clarifies the behaviour when both a
**legacy field-level `@Valid`** and a **type-argument `@Valid`** are present on
the same container field. The combination is declared to have **undefined behaviour**.

---

## Background — Two Ways to Cascade Validation

`@Valid` is the cascade annotation in Jakarta Validation. It tells the runtime:
*"when validating this element, also recursively validate the object it refers to."*

Since Java 8 / Bean Validation 1.1, `@Valid` can be placed in two distinct locations
on a container field.

---

### Form 1 — Legacy field-level placement (Bean Validation 1.0)

```java
@Valid
private List<Address> addresses;
```

`@Valid` sits on the **field itself**. The runtime detects that `List` is a container
(via a registered `ValueExtractor`), and automatically cascades into its element type
(`Address`). This works, but it is implicit — the annotation doesn't explicitly say
*which* type argument to cascade into.

---

### Form 2 — Type-argument placement (since BV 1.1 / Java 8)

```java
private List<@Valid Address> addresses;
```

`@Valid` sits directly on the **type argument**. This is explicit — it says precisely
"validate elements of this list." It also composes cleanly with more complex types:

```java
private Map<String, @Valid Address> byName;     // cascade into map values
private Optional<@Valid Address> primary;        // cascade into Optional's value
```

---

### For simple `List<Address>` both forms produce identical results today

In Hibernate Validator (the provider Liberty bundles), both forms cascade into
`Address` elements the same way. There is no practical difference for `List`.

---

## The Undefined-Behaviour Case — Both On The Same Field

```java
@Valid                               // ← legacy form
private List<@Valid Address> addresses;  // ← AND modern form
```

Before 4.0, the spec said nothing about this combination. Questions left unanswered:

- Does each `Address` get validated twice?
- Does the legacy `@Valid` cascade into the `List` object itself (not elements)?
- Is it a `ConstraintDeclarationException`?
- Is it silently valid?

---

## What Hibernate Validator Does Today (Validation 3.1)

HV builds cascading metadata by **merging** both sources
(`CascadingMetaDataBuilder.merge()`). Because both annotations resolve to the same
cascade target — elements of the `List` — the merge deduplicates them.

**Result: each `Address` element is validated exactly once. No double validation.**

However, this is HV's private implementation choice. Another compliant provider could:

| Interpretation | Outcome |
|---|---|
| A — merge (HV today) | Each element validated once ✓ |
| B — treat field `@Valid` as cascading into `List` object | Both `List` and `Address` elements validated (extra work) |
| C — reject as ambiguity | `ConstraintDeclarationException` thrown |

All three were technically permissible before 4.0 because the spec was silent.

---

## What 4.0 Changes

### 1. Declares the combination as undefined behaviour

Jakarta Validation 4.0 adds a single statement to the spec: if both a
legacy field-level `@Valid` and a type-argument `@Valid` are present on the
same element, the behaviour is **undefined**.

This is an honest acknowledgement that enforcing a specific outcome now —
when implementations have already diverged — would be a breaking change.


### 2. Signals future deprecation of the legacy form

The 4.0 spec explicitly states that a **future version** plans to:
- No longer support field-level `@Valid` on container fields
- Require the type-argument form exclusively
- Make the double-`@Valid` combination a `ConstraintDeclarationException`

---

## Migration Guide

```java
// ❌ Legacy — works today, will be removed in a future spec version
@Valid
private List<Address> addresses;

// ❌ Both — undefined behaviour (spec prose only, no API change), future hard error
@Valid
private List<@Valid Address> addresses;

// ✅ Modern — correct form, explicit, preferred
private List<@Valid Address> addresses;

// ✅ Modern — other container types
private Map<String, @Valid Address> byName;
private Optional<@Valid Address>    primary;
```

---

## Sample Application

The `val40cascade` test application in `io.openliberty.jakarta.validation.v40_fat`
demonstrates all three forms end-to-end on Liberty.

### Project structure

```
test-applications/val40cascade/
├── resources/WEB-INF/
│   └── beans.xml                          ← CDI bean-discovery-mode="all"
└── src/val40cascade/web/
    ├── Address.java                       ← cascaded bean (@NotBlank, @Email)
    ├── Customer.java                      ← three List<Address> fields, one per form
    └── CascadeValidTestServlet.java       ← 9 FAT tests
```

### The three fields in `Customer.java`

```java
// Form 1 — legacy
@Valid
private final List<Address> legacyAddresses;

// Form 2 — modern (preferred)
private final List<@Valid Address> modernAddresses;

// Form 3 — both (undefined behaviour, issue #260)
@Valid
private final List<@Valid Address> bothAddresses;
```

---

## Test Cases

### Section 1 — Legacy field-level `@Valid`

| Test | What it verifies |
|---|---|
| `testLegacyCascadeValidAddresses` | Valid addresses produce no violations |
| `testLegacyCascadeDetectsInvalidAddress` | Invalid Address inside list is caught |
| `testLegacyCascadeDetectsMultipleInvalidAddresses` | Multiple invalid addresses each produce a violation |

### Section 2 — Modern type-argument `@Valid`

| Test | What it verifies |
|---|---|
| `testModernCascadeValidAddresses` | Valid addresses produce no violations |
| `testModernCascadeDetectsInvalidAddress` | Invalid Address inside list is caught |

### Section 3 — Both `@Valid` present (issue #260 scenario)

| Test | What it verifies |
|---|---|
| `testBothCascadeValidAddresses` | Valid addresses produce no violations |
| `testBothCascadeDoesNotDoubleValidate` | **Each element produces exactly ONE violation** — HV deduplicates |
| `testBothCascadeViolationPathIsCorrect` | Violation path correctly points inside `bothAddresses` |

### Section 4 — Comparison

| Test | What it verifies |
|---|---|
| `testLegacyAndModernProduceSameViolationCount` | Legacy and modern forms produce identical violation counts |

---

## Impact on Open Liberty

| Area | Impact |
|---|---|
| **Runtime today** | ✅ None — HV deduplicates double `@Valid`, elements validated once |
description only, not normative spec text |
| **Future spec version** | ⚠️ Double `@Valid` will become `ConstraintDeclarationException` — apps must migrate |
| **New API** | ✅ None — `@Valid` annotation is unchanged |
| **Customer migration** | Remove field-level `@Valid` from container fields; keep only type-argument form |

---

## Build and Run

```bash
cd dev
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun
```

All 9 `CascadeValidTestServlet` tests pass today against `validation-3.1`.

---

## References

- [jakartaee/validation issue #260](https://github.com/jakartaee/validation/issues/260)
- [Jakarta Validation 4.0 specification](https://jakarta.ee/specifications/bean-validation/)
- [`@Valid` Javadoc](https://jakarta.ee/specifications/bean-validation/4.0/apidocs/jakarta/validation/valid)
