# Jakarta Validation 4.0 — `ConstraintValidator` Registration via Service Loader (Issue #257)

## Overview

Jakarta Validation 4.0 issue #257 proposes a third way to register
`ConstraintValidator` implementations: via **`java.util.ServiceLoader`**. Before
this change, validators could only be declared in two ways:

1. `@Constraint(validatedBy = { MyValidator.class })` — on the annotation
2. `<constraint-definition>` in a constraint-mappings XML file

The `val40sl` test application demonstrates all three mechanisms, compares them
side by side, and tests the **spec-defined precedence rules** when all three are
active simultaneously on the same constraint.

---

## Background — Why a Third Registration Path?

The traditional `validatedBy` attribute creates a hard **compile-time dependency**
between the constraint annotation and its validators. This works well when the
annotation author controls all validators, but breaks down in library scenarios:

```java
// A third-party library wants to validate @NotBlank on MyCustomType.
// To do this today it must either:
//   (a) fork and modify the @NotBlank annotation source — not possible
//   (b) ship a custom constraint that duplicates @NotBlank's semantics
```

Issue #257 removes this restriction by letting a jar declare its validators in a
standard service-loader file:

```
META-INF/services/jakarta.validation.ConstraintValidator
```

The runtime reads this file at bootstrap, inspects each entry's generic type
parameters `<AnnotationType, TargetType>`, and includes them in validator
resolution exactly as if they had been declared in `validatedBy`.

---

## The Three Registration Mechanisms

### Mechanism 1 — `validatedBy` (traditional, annotation-based)

```java
@Constraint(validatedBy = { ValidUuidAnnotationValidator.class })
public @interface ValidUuid3 { ... }
```

The validator is hard-wired at compile time. Any jar that uses this annotation
automatically picks up the listed validators with no extra configuration.

---

### Mechanism 2 — `<constraint-definition>` in constraint-mappings XML

Declared in `WEB-INF/validation.xml` (or `META-INF/validation.xml`):

```xml
<validation-config ...>
    <constraint-mapping>WEB-INF/constraints-uuid-xml.xml</constraint-mapping>
</validation-config>
```

And in the referenced mapping file:

```xml
<constraint-mappings ...>
    <constraint-definition annotation="val40sl.web.ValidUuid2">
        <validated-by include-existing-validators="false">
            <value>val40sl.web.ValidUuidXmlValidator</value>
        </validated-by>
    </constraint-definition>
</constraint-mappings>
```

The `annotation` attribute identifies the constraint. The `<value>` elements list
validator class names. The `include-existing-validators` attribute controls
whether the XML list **replaces** or **appends to** the annotation's `validatedBy`
list (and, with issue #257, also the service-loader list).

| `include-existing-validators` | Effect |
|---|---|
| `"false"` | XML replaces `validatedBy` + service-loader entirely — only XML validators used |
| `"true"` | XML validators are added to the `validatedBy` + service-loader set |

---

### Mechanism 3 — Service Loader (new in issue #257)

```
META-INF/services/jakarta.validation.ConstraintValidator
```

```
val40sl.web.ValidUuidValidator
val40sl.web.ValidUuid3ServiceLoaderValidator
```

The constraint annotation declares `validatedBy = {}` (empty). The runtime
discovers the validators via `ServiceLoader`, inspects the generic type
parameters `ConstraintValidator<ValidUuid, String>`, and binds them to the
correct constraint and target type automatically.

---

## Precedence Rules (Spec-Defined)

When all three mechanisms are present for the same constraint + target type:

```
┌──────────────────────────────────────────────────────────────────────┐
│  XML  include-existing-validators="false"                            │
│  ──────────────────────────────────────                              │
│  XML list replaces BOTH the annotation validatedBy list              │
│  AND the service-loader list. Only the XML validators are used.      │
├──────────────────────────────────────────────────────────────────────┤
│  XML  include-existing-validators="true"                             │
│  ─────────────────────────────────────                               │
│  All three sets are merged. The runtime's type-resolution algorithm  │
│  then picks the single most-specific validator for the target type.  │
├──────────────────────────────────────────────────────────────────────┤
│  No XML present                                                      │
│  ──────────────                                                      │
│  annotation validatedBy list + service-loader list are merged.       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
test-applications/val40sl/
├── resources/
│   ├── META-INF/services/
│   │   └── jakarta.validation.ConstraintValidator    ← service-loader entries
│   └── WEB-INF/
│       ├── beans.xml                                 ← CDI bean-discovery-mode="all"
│       ├── validation.xml                            ← references both mapping files
│       ├── constraints-uuid-xml.xml                  ← binds ValidUuidXmlValidator → @ValidUuid2
│       └── constraints-uuid-precedence.xml           ← binds ValidUuid3XmlValidator → @ValidUuid3
└── src/val40sl/web/
    │
    │  ── Section 1: Service-Loader only ──────────────────────────────────
    ├── ValidUuid.java                    @Constraint(validatedBy={})
    ├── ValidUuidValidator.java           registered in META-INF/services only
    ├── Order.java                        @ValidUuid String orderId
    │
    │  ── Section 2: XML only ─────────────────────────────────────────────
    ├── ValidUuid2.java                   @Constraint(validatedBy={})
    ├── ValidUuidXmlValidator.java        registered via constraints-uuid-xml.xml
    ├── Order2.java                       @ValidUuid2 String subscriptionId
    │
    │  ── Section 3: All three present (precedence) ────────────────────────
    ├── ValidUuid3.java                   @Constraint(validatedBy={ValidUuidAnnotationValidator.class})
    ├── ValidUuidAnnotationValidator.java registered via validatedBy
    ├── ValidUuid3ServiceLoaderValidator.java  registered via META-INF/services
    ├── ValidUuid3XmlValidator.java       registered via constraints-uuid-precedence.xml
    ├── Order3.java                       @ValidUuid3 String paymentRef
    │
    └── ServiceLoaderValidatorTestServlet.java  ← 13 FAT tests across 3 sections
```

---

## The Constraint Annotations

### `ValidUuid` — service-loader path

```java
@Constraint(validatedBy = {})   // empty — validator comes from ServiceLoader
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUuid {
    String message() default "must be a valid UUID";
    // ...
}
```

`META-INF/services/jakarta.validation.ConstraintValidator`:
```
val40sl.web.ValidUuidValidator
```

---

### `ValidUuid2` — XML path

```java
@Constraint(validatedBy = {})   // empty — validator comes from XML
public @interface ValidUuid2 { ... }
```

`WEB-INF/constraints-uuid-xml.xml`:
```xml
<constraint-definition annotation="val40sl.web.ValidUuid2">
    <validated-by include-existing-validators="false">
        <value>val40sl.web.ValidUuidXmlValidator</value>
    </validated-by>
</constraint-definition>
```

---

### `ValidUuid3` — all three paths (precedence test)

```java
@Constraint(validatedBy = { ValidUuidAnnotationValidator.class })  // annotation path
public @interface ValidUuid3 { ... }
```

`META-INF/services/jakarta.validation.ConstraintValidator`:
```
val40sl.web.ValidUuid3ServiceLoaderValidator   ← service-loader path
```

`WEB-INF/constraints-uuid-precedence.xml`:
```xml
<constraint-definition annotation="val40sl.web.ValidUuid3">
    <validated-by include-existing-validators="false">  <!-- XML WINS -->
        <value>val40sl.web.ValidUuid3XmlValidator</value>
    </validated-by>
</constraint-definition>
```

**Result**: only `ValidUuid3XmlValidator` is invoked. The annotation and
service-loader validators are discarded because `include-existing-validators="false"`.

---

## Test Cases

### Section 1 — Service-Loader Registration

| Test | What it verifies |
|---|---|
| `testServiceLoader_ValidUuidProducesNoViolations` | Well-formed UUID → no violations; no `UnexpectedTypeException` |
| `testServiceLoader_ValidatorWasInvoked` | `ValidUuidValidator.wasInvoked` is `true` — discovery confirmed |
| `testServiceLoader_MalformedUuidProducesViolation` | Invalid string → exactly 1 violation |
| `testServiceLoader_NullProducesNoViolations` | `null` is skipped — null handling is `@NotNull`'s responsibility |

### Section 2 — XML Registration

| Test | What it verifies |
|---|---|
| `testXml_ValidUuidProducesNoViolations` | Well-formed UUID → no violations (XML-bound validator works) |
| `testXml_ValidatorWasInvoked` | `ValidUuidXmlValidator.wasInvoked` is `true` — XML binding confirmed |
| `testXml_MalformedUuidProducesViolation` | Invalid string → exactly 1 violation |
| `testXml_ViolationMessageMatchesConstraintDefault` | Message matches `@ValidUuid2` default |

### Section 3 — All Three Present (Precedence)

| Test | What it verifies |
|---|---|
| `testPrecedence_ValidUuidProducesNoViolations` | Well-formed UUID → no violations with all 3 active |
| `testPrecedence_XmlValidatorWinsOverAnnotationAndServiceLoader` | **Core precedence test** — only XML validator is invoked; annotation and service-loader validators have `wasInvoked == false` |
| `testPrecedence_MalformedUuidProducesViolation` | Invalid string → exactly 1 violation (XML validator enforces constraint) |
| `testPrecedence_ViolationMessageMatchesConstraintDefault` | Message matches `@ValidUuid3` default |
| `testPrecedence_ViolationPropertyPath` | Violation property path = `"paymentRef"` |

---

## Impact on Open Liberty

| Area | Impact |
|---|---|
| **Runtime today** | ⚠️ Service-loader discovery (`testServiceLoader_*`) requires implementing issue #257 in the runtime. If not yet implemented, those tests throw `UnexpectedTypeException`. |
| **XML registration** | ✅ Works today — `<constraint-definition>` has been part of the spec since BV 1.1 |
| **Annotation `validatedBy`** | ✅ Works today — traditional path, no changes needed |
| **Precedence (XML overrides)** | ✅ Works today — `include-existing-validators="false"` is existing spec behaviour |
| **New API** | None — issue #257 is a runtime discovery change, not an API change |
| **Hibernate Validator** | Service-loader support may require a minimum HV version; check release notes when Jakarta Validation 4.0 GA ships |

---

## Build and Run

```bash
cd dev

# Run the full FAT suite (all validation 4.0 apps)
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun

# Run only the service-loader/XML/precedence tests
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun \
    -Dfat.test.class=io.openliberty.jakarta.validation.v40.fat.ServiceLoaderValidatorTest
```

---

## References

- [jakartaee/validation issue #257](https://github.com/jakartaee/validation/issues/257) — ConstraintValidator declaration via service loader
- [Jakarta Validation 4.0 specification](https://jakarta.ee/specifications/bean-validation/)
- [Jakarta Validation constraint mapping XML schema](https://jakarta.ee/xml/ns/validation/mapping/)
