# Jakarta Validation 4.0 — Spec Correction: Type-Validator Selection (BVAL-698)

## Overview

Jakarta Validation 4.0 corrects an incorrect example in the specification's
type-validator resolution rules (BVAL-698). The correction brings the spec text into
alignment with what all compliant implementations — including Hibernate Validator —
have always done. **No runtime behaviour changed**; only the documentation was wrong.

---

## Background — Type-Validator Selection

When a constraint is applied to a field, the runtime must decide which
`ConstraintValidator` to use. A constraint can declare validators for multiple
types:

```java
@Constraint(validatedBy = {
    SizeValidatorForCharSequence.class,   // handles CharSequence
    SizeValidatorForCollection.class,     // handles Collection
    SizeValidatorForSerializable.class    // handles Serializable
})
public @interface Size { ... }
```

The spec's resolution algorithm picks the **most specific** applicable validator
based on the declared type of the annotated element. If more than one validator
applies and neither is more specific than the other, the result is ambiguous and
an `UnexpectedTypeException` is thrown.

---

## The Bug — Table 5.1 / BVAL-698

The spec contained a table (historically "Table 5.1") illustrating the resolution
algorithm with worked examples. The **last row** showed:

| Annotated field type | Registered validators | Expected outcome (old, wrong spec) |
|---|---|---|
| `String` | `CharSequence`, `Serializable` | `UnexpectedTypeException` ❌ |

This was **wrong**. `String` implements both `CharSequence` and `Serializable`,
so both validators are applicable. The algorithm then looks for the most specific
type. `CharSequence` is a direct supertype of `String` in the type hierarchy, and is
more specific than `Serializable` (a marker interface), so
`SizeValidatorForCharSequence` should be selected cleanly — no exception.

The corrected table row:

| Annotated field type | Registered validators | Correct outcome (4.0 spec) |
|---|---|---|
| `String` | `CharSequence`, `Serializable` | `CharSequenceValidator` selected ✅ |

---

## What 4.0 Changes

**Nothing at runtime.** The resolution algorithm itself is unchanged. Only the
spec example table is corrected to accurately reflect what the algorithm produces.

Implementations (Hibernate Validator across all versions) never implemented the
wrong behaviour — HV never even registered a `SizeValidatorForSerializable`
(confirmed: all versions use only `SizeValidatorForCharSequence`, `SizeValidatorForCollection`,
and `SizeValidatorForMap`). The bug existed solely in the spec text.

---

## Sample Application

The `val40tsr` test application in `io.openliberty.jakarta.validation.v40_fat`
demonstrates the corrected behaviour by directly reproducing the Table 5.1 scenario.

### Project structure

```
test-applications/val40tsr/
├── resources/WEB-INF/
│   └── beans.xml                               ← CDI bean-discovery-mode="all"
└── src/val40tsr/web/
    ├── HasContent.java                         ← @HasContent constraint with two validators
    ├── HasContentValidatorForCharSequence.java ← handles CharSequence (more specific)
    ├── HasContentValidatorForSerializable.java ← handles Serializable (less specific)
    ├── Message.java                            ← bean with @HasContent String field
    └── TypeValidatorSelectionTestServlet.java  ← 6 FAT tests
```

### The constraint — `HasContent.java`

Two validators registered — exactly matching the Table 5.1 scenario:

```java
@Constraint(validatedBy = {
    HasContentValidatorForCharSequence.class,   // handles CharSequence
    HasContentValidatorForSerializable.class    // handles Serializable
})
public @interface HasContent {
    String message() default "value must not be empty";
    // ...
}
```

### The bean — `Message.java`

```java
public class Message {

    @HasContent          // String satisfies both CharSequence and Serializable
    private final String text;
}
```

### The validators

```java
// More specific — String IS-A CharSequence
public class HasContentValidatorForCharSequence
        implements ConstraintValidator<HasContent, CharSequence> {

    static volatile boolean wasInvoked = false;

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext ctx) {
        wasInvoked = true;
        return value == null || value.length() > 0;
    }
}

// Less specific — String IS-A Serializable, but CharSequence wins
public class HasContentValidatorForSerializable
        implements ConstraintValidator<HasContent, Serializable> {

    static volatile boolean wasInvoked = false;   // tests assert this stays false

    @Override
    public boolean isValid(Serializable value, ConstraintValidatorContext ctx) {
        wasInvoked = true;
        return value != null;
    }
}
```

---

## Test Cases

| Test | What it verifies |
|---|---|
| `testNoUnexpectedTypeExceptionForStringField` | **Core test** — no `UnexpectedTypeException` is thrown (refutes the old wrong spec entry) |
| `testCharSequenceValidatorSelectedForStringField` | `CharSequence` validator is invoked; `Serializable` validator is **not** |
| `testNonEmptyStringProducesNoViolations` | Non-empty string passes `@HasContent` |
| `testEmptyStringProducesViolation` | Empty string fails `@HasContent` with one violation |
| `testNullStringProducesNoViolations` | Null is valid — null handling is `@NotNull`'s job |
| `testViolationMessageMatchesDefaultTemplate` | Violation message matches the default on `@HasContent` |

---

## Impact on Open Liberty

| Area | Impact |
|---|---|
| **Open Liberty runtime** | ✅ None — implementation was always correct |
| **Spec API bundle** | ✅ None — no new interfaces or methods |
| **Hibernate Validator** | ✅ None — HV never registered a `Serializable` validator for `@Size` |
| **Customer applications** | ✅ None — no existing behaviour changes |
| **FAT test suite** | ✅ Covered by `val40tsr` app in `io.openliberty.jakarta.validation.v40_fat` |

---

## Build and Run

```bash
cd dev
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun
```

All four apps (`val40`, `val40attr`, `val40init`, `val40tsr`) run in the same
Liberty server instance during a single FAT execution.

---

## References

- [BVAL-698 — Fix incorrect example in table 5.1](https://hibernate.atlassian.net/browse/BVAL-698)
- [Jakarta Validation 4.0 spec PR #259](https://github.com/jakartaee/validation-spec/pull/259)
- [Jakarta Validation 4.0 specification](https://jakarta.ee/specifications/bean-validation/)
