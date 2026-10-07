# Jakarta Validation 4.0 — Array Element Annotation Placement (`val40arr`)

## The Confusion in One Sentence

When you write `@NotNull String[] tags`, Java does **not** constrain the
`String` elements — it constrains the **array reference itself**.

---

## Background — Two Kinds of Annotations on an Array Field

Java (since Java 8) distinguishes between two conceptually different annotation
targets on an array field:

| What you want to constrain | Java syntax | `@Target` element type |
|---|---|---|
| The **array reference** (is the array itself null?) | `@NotNull String[] tags` | `FIELD` |
| Each **element** inside the array (are the strings blank?) | `@NotBlank String[] tags` — **looks the same!** | `TYPE_USE` |

The problem: both placements **look identical** to a developer reading the code.
Whether an annotation applies to the reference or to the element depends entirely
on which `ElementType` values are listed in the annotation's `@Target`.

---

## Understanding `ElementType.FIELD` vs `ElementType.TYPE_USE`

### `FIELD` — attached to the field declaration

```java
@Target({ ElementType.FIELD, ElementType.METHOD, ... })
@interface NotNull { ... }
```

When you write:

```java
@NotNull String[] tags;
```

`@NotNull` attaches to the **field declaration** — the `tags` field itself.
It means: *"the `tags` array object must not be null."*
The strings inside the array are completely unaffected.

---

### `TYPE_USE` — attached to the type in the declaration

```java
@Target({ ElementType.TYPE_USE })
@interface NotBlankElement { ... }
```

When you write:

```java
@NotBlankElement String[] tags;
```

`@NotBlankElement` attaches to the **component type** of the array (`String`),
not to the field declaration. Java's type annotation rules say: if an annotation
on an array field has `TYPE_USE` but not `FIELD`, it annotates the innermost
component type.

This is what triggers **value extraction** in Jakarta Validation: HV's built-in
`Object[]` value extractor iterates the array and applies `@NotBlankElement`
to each `String` element individually.

---

## The Three Forms — Side by Side

### Form A — Constrain the array REFERENCE

```java
@NotNull(message = "tags array must not be null")
private String[] referenceConstrained;
```

| Scenario | Violation? |
|---|---|
| `tags = null` | ✅ Yes — `@NotNull` fires on the reference |
| `tags = new String[]{ "", " ", null }` | ❌ No — array is non-null; elements are ignored |
| `tags = new String[]{ "java", "ee" }` | ❌ No |

---

### Form B — Constrain each ELEMENT

```java
private @NotBlankElement String[] elementConstrained;
//       ↑ TYPE_USE — attached to String, not to the field
```

| Scenario | Violation? |
|---|---|
| `tags = null` | ❌ No — no array to iterate; TYPE_USE doesn't guard the reference |
| `tags = new String[]{ "java", "" }` | ✅ Yes — element `[1]` is blank; path = `elementConstrained[1]` |
| `tags = new String[]{ "", "  " }` | ✅ Yes — two violations, one per blank element |
| `tags = new String[]{ "java", "ee" }` | ❌ No |

---

### Form C — Constrain BOTH reference and elements

```java
@NotNull(message = "array must not be null")
private @NotBlankElement String[] bothConstrained;
```

Both constraints are active and independent:

| Scenario | Violation? |
|---|---|
| `tags = null` | ✅ Yes — `@NotNull` fires; element constraint cannot fire (no array) |
| `tags = new String[]{ "" }` | ✅ Yes — `@NotBlankElement` fires on `[0]`; `@NotNull` satisfied |
| `tags = new String[]{ "java", "ee" }` | ❌ No |

---

## Why This Is Confusing — The `@Target` Lookup Rule

`@NotNull` declares:

```java
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
```

It has **both** `FIELD` and `TYPE_USE`. When Java resolves `@NotNull String[] tags`,
it uses `FIELD` (higher priority for a field declaration) — so `@NotNull` ends up
on the reference, not the component type.

`@NotBlankElement` (our custom constraint) declares only:

```java
@Target({ TYPE_USE })
```

No `FIELD`. So `@NotBlankElement String[] tags` can only attach to the component
type — exactly what we want for per-element validation.

**This is the ambiguity Jakarta Validation 4.0 is addressing:** the built-in
constraints like `@NotNull`, `@NotBlank`, `@Size` all have both `FIELD` and
`TYPE_USE`, so placing them on an array component type is ambiguous. The spec
is clarifying what each placement means and signalling a future move toward
explicit `TYPE_USE`-only annotations for element-level constraints.

---

## The Planned Breaking Change — Who Does What

### The spec change (Jakarta Validation spec committee)

The planned change is to **remove `ElementType.FIELD` from the `@Target` of
all 20 built-in constraint annotations** in the `jakarta.validation-api` jar.

```java
// TODAY (3.1 and 4.0-M1) — FIELD present → placement on array is ambiguous
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
public @interface NotNull { ... }

// AFTER the planned change — FIELD removed → placement is unambiguous
@Target({ METHOD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
public @interface NotNull { ... }
```

This is a **source-level breaking change** in the API jar itself. No
implementation logic changes are needed for this part.

---

### Hibernate Validator (the provider)

HV already has built-in value extractors for every array type
(`ObjectArrayValueExtractor`, `IntArrayValueExtractor`, `LongArrayValueExtractor`,
`BooleanArrayValueExtractor`, etc.) — they have been present since Bean Validation
2.0 and are in HV 9.0 (the version Liberty bundles today). No new extraction
logic is needed.

What HV will add is **enforcement at validation bootstrap time**: when both a
field-level and an element-level constraint are present on the same array field
(via a custom constraint that still declares both `FIELD` and `TYPE_USE`), HV
will eventually throw `ConstraintDeclarationException` — the same treatment
planned for the double-`@Valid` case (issue #260).

---

### Liberty

Liberty's changes are in the OSGi bundle wiring layer, not in validation logic:

| File | Change required |
|---|---|
| [`bnd.overrides`](../../../../../../io.openliberty.org.hibernate.validator.9.0/bnd.overrides) | Bump all `jakarta.validation.*` import ranges from `[3.1.0,4.0.0)` → `[4.0.0,5.0.0)` so the HV bundle wires against the 4.0 API |
| `io.openliberty.beanValidation-4.0.feature` | Flip `kind=noship` → `kind=ga` once the API and HV jars are GA |
| `io.openliberty.jakarta.validation-4.0.feature` | Point to the 4.0 GA API jar (currently using 4.0.0-M1) |

No changes are needed in Liberty's own bval integration bundles
(`com.ibm.ws.beanvalidation.*`). The array extraction and constraint-declaration
enforcement is entirely the provider's (HV's) responsibility.

---

## What Happens When Both Field-Level and Element-Level Are Present

### Today (`validation-3.1`) — both work independently

```java
// Custom constraint with both FIELD and TYPE_USE in @Target
@NotNull                        // FIELD → constrains the array reference
private @NotNull String[] tags; // TYPE_USE → constrains each element
                                // Result: both enforced, no error
```

### After `FIELD` is removed from built-in constraints

The old field-level placement `@NotNull String[] tags` becomes a **Java compiler
error** immediately when upgrading the API jar:

```
error: annotation type not applicable to this kind of declaration
    @NotNull
    ^
```

There is no runtime behaviour to define — the code will not compile.

### If a custom constraint still declares both `FIELD` and `TYPE_USE`

For a user-defined constraint that deliberately keeps both element types, placing
it in both positions on the same array follows the same trajectory as
double-`@Valid` (issue #260):

| Timing | Behaviour |
|---|---|
| Immediately in 4.0 | **Undefined behaviour** — implementations decide; HV deduplicates or picks one |
| Future spec version | **`ConstraintDeclarationException`** at bootstrap — hard error |

---

## Migration Guide

```java
// ❌ BEFORE (valid today) — @NotNull guards the array reference
@NotNull
private String[] tags;

// ❌ AFTER removing FIELD — compile error
// @NotNull no longer has FIELD in @Target → cannot be placed on a field declaration

// ✅ To guard the REFERENCE after the change — use a constructor/setter parameter
public void setTags(@NotNull String[] tags) { ... }  // METHOD/PARAMETER still valid

// ✅ To guard each ELEMENT after the change — TYPE_USE wins once FIELD is gone
private @NotNull String[] tags;   // after FIELD removal: attaches to String component type

// ✅ Best practice NOW (before the change) — use a TYPE_USE-only custom constraint
// No ambiguity in any spec version, no migration needed when FIELD is removed
private @NotBlankElement String[] tags;
```

---

## Violation Paths

When an element-level constraint fires, the violation path includes the array
index, just like a `List` element violation:

```
elementConstrained[0]    ← element at index 0 is blank
elementConstrained[2]    ← element at index 2 is blank
bothConstrained[0]       ← element at index 0 of bothConstrained is blank
```

A reference-level constraint path is just the field name:

```
referenceConstrained     ← the array reference is null
```

---

## Project Structure

```
test-applications/val40arr/
├── resources/WEB-INF/
│   └── beans.xml                           ← CDI bean-discovery-mode="all"
└── src/val40arr/web/
    ├── NotBlankElement.java                ← custom constraint, TYPE_USE only
    ├── NotBlankElementValidator.java       ← rejects null or blank String
    ├── TaggedItem.java                     ← bean with Forms A, B, C fields
    └── ArrayAnnotationTestServlet.java     ← 12 FAT tests across 4 sections
```

---

## Test Cases

### Section 1 — Form A: `@NotNull` on the array reference

| Test | Scenario | Verifies |
|---|---|---|
| `testFormA_NullReference_Violation` | `null` array | `@NotNull` fires, path = `"referenceConstrained"` |
| `testFormA_NonNullArrayWithBlankElements_NoViolation` | Non-null array, blank elements | `@NotNull` does **not** fire — it only sees the reference |
| `testFormA_ValidReference_NoViolation` | Non-null array, clean elements | No violations |

### Section 2 — Form B: `@NotBlankElement` on each element (TYPE_USE)

| Test | Scenario | Verifies |
|---|---|---|
| `testFormB_BlankElement_Violation` | One blank element | Violation at `elementConstrained[1]` |
| `testFormB_MultipleBlankElements_MultipleViolations` | Two blank elements | Two violations at `[1]` and `[2]` |
| `testFormB_NullReference_NoViolation` | `null` array reference | No violation — TYPE_USE doesn't guard the reference |
| `testFormB_AllValidElements_NoViolation` | All clean elements | No violations |

### Section 3 — Form C: both reference and element constraints

| Test | Scenario | Verifies |
|---|---|---|
| `testFormC_NullReference_OnlyReferenceViolation` | `null` array | Only `@NotNull` fires |
| `testFormC_NonNullArrayWithBlankElement_OnlyElementViolation` | Non-null, blank element | Only `@NotBlankElement` fires at `[0]` |
| `testFormC_ValidArray_NoViolation` | Non-null, clean elements | No violations |

### Section 4 — The ambiguity demonstration

| Test | What it proves |
|---|---|
| `testAmbiguity_SameLookDifferentMeaning` | A non-null array with a blank element triggers the TYPE_USE constraint but NOT `@NotNull` — same visual placement, different semantics |

---

## Build and Run

```bash
cd dev
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun
```

To run only this test class:

```bash
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun \
    -Dfat.test.class=io.openliberty.jakarta.validation.v40.fat.ArrayAnnotationTest
```

---

## References

- [Jakarta Validation 4.0 specification](https://jakarta.ee/specifications/bean-validation/)
- [JLS §9.6.4.1 — Annotation `@Target`](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.1)
- [JEP 104 — Annotations on Java Types (Java 8)](https://openjdk.org/jeps/104)
- [`ValueExtractor` SPI — built-in extractors](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/valueextraction/ValueExtractor)
