# Jakarta Validation 4.0 — Group Sequencing and Inheritance Clarification (BVAL-711)

## Overview

Jakarta Validation 4.0 (issue #261) clarifies the spec language around what happens
when a subclass inherits a parent class that has redefined its default validation group
using `@GroupSequence`. This is a **spec text clarification only** — no new API is
introduced and no runtime behaviour changes. Hibernate Validator has always implemented
the intended behaviour; the 4.0 spec formalises it.

---

## Part 1 — What is a Group Sequence?

### The problem without groups

Without any groups, all constraints on a bean run at once:

```java
public class Order {
    @NotNull  @NotBlank  private String productId;
    @NotNull  @Min(1)    private Integer quantity;
}
```

`validator.validate(order)` runs all four constraints simultaneously and returns all
violations together. There is no staging — a `null` productId and a negative quantity
both appear in the same violation set, even though checking `@Min` on a null value
makes no sense.

---

### Step 1 — Introduce groups (labels for phases)

Groups are empty marker interfaces used as labels:

```java
public interface BasicChecks {}    // phase 1: null/format checks
public interface BusinessChecks {} // phase 2: business-rule checks
```

Assign constraints to groups:

```java
public class Order {
    @NotNull(groups = BasicChecks.class)           // phase 1
    @NotBlank(groups = BasicChecks.class)          // phase 1
    private String productId;

    @NotNull(groups = BasicChecks.class)           // phase 1
    @Min(value = 1, groups = BusinessChecks.class) // phase 2
    private Integer quantity;
}
```

You can now validate one phase at a time — but the caller must sequence them manually,
which is verbose and fragile.

---

### Step 2 — `@GroupSequence` automates the phases

Put `@GroupSequence` on the class to redefine what `validator.validate(order)` does.
**The class itself must appear in its own sequence** to cover un-grouped constraints:

```java
@GroupSequence({ BasicChecks.class, BusinessChecks.class, Order.class })
public class Order {

    @NotNull(groups = BasicChecks.class)
    @NotBlank(groups = BasicChecks.class)
    private String productId;

    @NotNull(groups = BasicChecks.class)
    @Min(value = 1, groups = BusinessChecks.class)
    private Integer quantity;

    @Size(max = 200)   // ← no group = Default = runs at the Order.class slot
    private String notes;
}
```

Now `validator.validate(order)` runs three phases automatically:

```
Phase 1 — BasicChecks:   @NotNull/@NotBlank on productId, @NotNull on quantity
          ↓ any violation? → STOP, return those violations
Phase 2 — BusinessChecks: @Min on quantity
          ↓ any violation? → STOP, return those violations
Phase 3 — Order.class:   @Size on notes  (un-grouped = Default = runs here)
```

**If phase 1 fails, phases 2 and 3 never run.** This is the key benefit — fail-fast
staged validation without boilerplate.

---

### Why the class must appear in its own sequence

`Order.class` in the sequence means: *"run all constraints that belong to Default
on Order at this position."* A constraint with no explicit group automatically belongs
to Default. If `Order.class` were omitted from the sequence, un-grouped constraints
would silently never run.

```java
// ❌ Wrong — Order.class missing, @Size on notes never runs
@GroupSequence({ BasicChecks.class, BusinessChecks.class })
public class Order { ... }

// ✅ Correct — Order.class included, @Size on notes runs at phase 3
@GroupSequence({ BasicChecks.class, BusinessChecks.class, Order.class })
public class Order { ... }
```

---

## Part 2 — Inheritance and the BVAL-711 Clarification

### The scenario

```java
@GroupSequence({ BasicChecks.class, BusinessChecks.class, Order.class })
public class Order {
    @NotNull(groups = BasicChecks.class)           String productId;
    @Min(value=1, groups = BusinessChecks.class)   Integer quantity;
}

public class PriorityOrder extends Order {
    @NotNull          // ← no group = Default
    private String priority;
}
```

`PriorityOrder` has no `@GroupSequence` of its own. It **inherits** `Order`'s sequence.

**Question the spec didn't clearly answer:** Where does `@NotNull priority` run?
`priority` is in Default — but the sequence was defined on `Order`, not `PriorityOrder`.

---

### What the clarification says

The child's own Default constraints slot in **at the position where the parent class
appears** in the inherited sequence — i.e. at the `Order.class` slot, which is phase 3.

```
validator.validate(priorityOrder):

Phase 1 — BasicChecks:
    @NotNull on productId      ← from Order
    ↓ violation? → STOP

Phase 2 — BusinessChecks:
    @Min on quantity           ← from Order
    ↓ violation? → STOP

Phase 3 — Order.class slot:
    @NotNull on priority       ← from PriorityOrder's own Default constraints
```

**Consequence:** If `productId` is null (phase 1 fails), `@NotNull priority` is
**never checked** — even if `priority` is also null. The child's constraints are
subject to the same fail-fast rules as the inherited sequence.

---

### What changed between Validation 3.1 and 4.0

#### Validation 3.1 spec — vague

The 3.1 spec said (paraphrased):

> *"The Default group of a child class contains the constraints of the redefined
> default group of the parent class."*

That sentence tells you **what** is included but not **when** it runs. A developer
reading it could reasonably ask:

- Does `@NotNull priority` run at phase 3 (Order.class slot)?
- Or does it run after the entire inherited sequence completes as a separate step?
- Or is it merged into phase 1 somehow?
- Or does it cause a `GroupDefinitionException` because the child has no `@GroupSequence`?

The spec did not answer any of these. The behaviour was genuinely ambiguous — only
Hibernate Validator's implementation gave the de-facto answer.

#### Validation 4.0 spec — explicit

The 4.0 spec adds a precise statement (paraphrased):

> *"The Default group of a subclass that inherits a redefined default group sequence
> is resolved by substituting the subclass's own Default constraints at the position
> where the parent class appears in the inherited sequence."*

That one sentence closes all four questions above:

| Question | 4.0 Answer |
|---|---|
| Where do child Default constraints run? | At the parent class slot (phase 3 in this example) |
| Are they skipped if an earlier phase fails? | Yes — same fail-fast rule as every other phase |
| Do they merge into phase 1? | No |
| Does it throw `GroupDefinitionException`? | No |

#### Why this matters even though HV was always correct

Hibernate Validator implemented the intended behaviour from the start. But:

1. **TCK tests** covering this scenario had no formal spec backing — they were
   testing HV's convention, not a normative requirement. Another implementation
   could have behaved differently and not be technically non-compliant.
2. **Developers** had no authoritative answer to point to — only "HV does X,
   therefore X must be right."
3. **Other validation providers** had no normative guidance.

The 4.0 change makes this a **spec requirement**. Any compliant implementation —
including the one Liberty ships — must now behave exactly as the `val40grp` tests
demonstrate.

#### Summary table

| | Spec 3.1 | Spec 4.0 |
|---|---|---|
| **Where do child Default constraints run?** | Unclear / implementation-defined | Explicitly at the parent class slot |
| **Are child constraints skipped if earlier phase fails?** | Implied but not stated | Explicitly yes |
| **Can cause `GroupDefinitionException`?** | Unclear | Explicitly no |
| **Runtime behaviour (HV)** | Always correct | Still correct, now formally backed |
| **Normative requirement?** | No | Yes |

---

## Sample Application

The `val40grp` test application in `io.openliberty.jakarta.validation.v40_fat`
demonstrates all of the above end-to-end on Liberty.

### Project structure

```
test-applications/val40grp/
├── resources/WEB-INF/
│   └── beans.xml                         ← CDI bean-discovery-mode="all"
└── src/val40grp/web/
    ├── BasicChecks.java                  ← group interface (phase 1 label)
    ├── BusinessChecks.java               ← group interface (phase 2 label)
    ├── Order.java                        ← bean with @GroupSequence
    ├── PriorityOrder.java                ← subclass with un-grouped @NotNull
    └── GroupSequenceTestServlet.java     ← 9 FAT tests
```

### The sequence in code

```
Order.java
    @GroupSequence({ BasicChecks.class, BusinessChecks.class, Order.class })
    ├── productId  @NotNull @NotBlank (BasicChecks)
    ├── quantity   @NotNull (BasicChecks) + @Min(1) (BusinessChecks)
    └── notes      @Size(max=200)         (no group = Default = phase 3)

PriorityOrder extends Order
    └── priority   @NotNull              (no group = Default = phase 3 inherited)
```

---

## Test Cases

### Section 1 — Basic group sequencing

| Test | What it verifies |
|---|---|
| `testValidOrderProducesNoViolations` | Baseline — fully valid order produces zero violations |
| `testPhase1FailStopsPhase2` | Phase 1 (`@NotNull` null productId) fails → phase 2 (`@Min` quantity) never runs |
| `testPhase2FailStopsPhase3` | Phase 2 (`@Min` quantity=0) fails → phase 3 (`@Size` notes) never runs |
| `testPhase3RunsWhenPhase1And2Pass` | Phases 1 and 2 pass → phase 3 fires for oversized notes |
| `testSequenceOrderIsBasicThenBusinessThenDefault` | Confirms only phase 3 violation reported when phases 1/2 pass |

### Section 2 — Inheritance + BVAL-711 clarification

| Test | What it verifies |
|---|---|
| `testValidPriorityOrderProducesNoViolations` | Baseline — valid PriorityOrder produces zero violations |
| `testChildDefaultConstraintSkippedWhenPhase1Fails` | Phase 1 fails → child's `@NotNull priority` (phase 3) is never evaluated |
| `testChildDefaultConstraintRunsInPhase3` | Phases 1/2 pass → child's `@NotNull priority` fires at phase 3 |
| `testOnlyChildViolationReportedInPhase3` | Only the child's violation appears; no spurious parent violations |

---

## Impact on Open Liberty

| Area | Impact |
|---|---|
| **Open Liberty runtime** | ✅ None — HV always implemented the clarified behaviour |
| **New API** | ✅ None — `@GroupSequence` and `Default` are unchanged |
| **Liberty implementation** | ✅ None — clarification formalises existing behaviour |
| **Customer applications** | ✅ No behaviour change; developers gain unambiguous spec language |
| **FAT test suite** | ✅ Covered by `val40grp` in `io.openliberty.jakarta.validation.v40_fat` |

---

## Build and Run

```bash
cd dev
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun
```

All 9 `GroupSequenceTestServlet` tests pass today against `validation-3.1` —
confirming the runtime has always been correct.

---

## References

- [jakartaee/validation issue #261 — BVAL-711](https://github.com/jakartaee/validation/issues/261)
- [BVAL-711 on Hibernate JIRA](https://hibernate.atlassian.net/browse/BVAL-711)
- [Jakarta Validation 4.0 specification](https://jakarta.ee/specifications/bean-validation/)
- [Original ML thread (2018)](http://lists.jboss.org/pipermail/beanvalidation-dev/2018-April/001491.html)
