/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package val40grp.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * FAT servlet demonstrating Jakarta Validation group sequencing and the
 * spec clarification for inheritance + default group redefinition (BVAL-711,
 * issue #261).
 *
 * <h2>What is a group sequence?</h2>
 * <p>Normally all constraints run at once and all violations are returned
 * together. A {@code @GroupSequence} on a class redefines what "Default"
 * means for that class, splitting validation into ordered phases:
 * <pre>
 *   Phase 1: BasicChecks   — null/blank checks
 *   Phase 2: BusinessChecks — business-rule checks
 *   Phase 3: Order.class   — un-grouped (Default) constraints
 * </pre>
 * <p>If phase N has any violations, phases N+1 onwards are skipped.
 *
 * <h2>Inheritance clarification (BVAL-711)</h2>
 * <p>{@link PriorityOrder} extends {@link Order} and adds a {@code @NotNull priority}
 * field with no explicit group (= Default). The spec clarification formalises
 * that this constraint runs at the {@code Order.class} slot (phase 3) in the
 * inherited sequence — meaning it is skipped if phase 1 or 2 fails.
 */
@SuppressWarnings("serial")
@WebServlet("/GroupSequenceTestServlet")
public class GroupSequenceTestServlet extends FATServlet {

    @Inject
    Validator validator;

    // -----------------------------------------------------------------------
    // Section 1 — Basic group sequencing on Order
    // -----------------------------------------------------------------------

    /**
     * A fully valid order produces no violations.
     */
    @Test
    public void testValidOrderProducesNoViolations() {
        Order order = new Order("PROD-001", 5, "Please wrap carefully");

        assertTrue("A valid order should produce no violations",
                   validator.validate(order).isEmpty());
    }

    /**
     * When productId is null (BasicChecks failure), only BasicChecks violations
     * are returned — BusinessChecks is never run.
     *
     * <p>Demonstrates phase 1 fail → phase 2 skipped.
     */
    @Test
    public void testPhase1FailStopsPhase2() {
        // productId null → BasicChecks @NotNull fails
        // quantity -1   → BusinessChecks @Min would also fail, but should NOT be reported
        Order order = new Order(null, -1, null);

        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        // Only BasicChecks violations should appear
        Set<String> paths = violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(Collectors.toSet());

        assertTrue("productId BasicChecks violation must be reported",
                   paths.contains("productId"));
        assertTrue("BusinessChecks @Min on quantity must NOT be reported when BasicChecks fails",
                   violations.stream().noneMatch(v ->
                       v.getPropertyPath().toString().equals("quantity") &&
                       v.getMessage().contains("1")));
    }

    /**
     * When BasicChecks all pass but a BusinessChecks constraint fails,
     * only BusinessChecks violations are returned.
     *
     * <p>Demonstrates phase 1 pass → phase 2 fail → phase 3 skipped.
     */
    @Test
    public void testPhase2FailStopsPhase3() {
        // productId present, quantity present but zero → BusinessChecks @Min fails
        // notes is a very long string → phase 3 @Size would fail, but should NOT be reported
        String longNotes = "x".repeat(201); // exceeds @Size(max=200)
        Order order = new Order("PROD-001", 0, longNotes);

        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        // quantity @Min violation must appear
        assertTrue("BusinessChecks @Min on quantity must be reported",
                   violations.stream().anyMatch(v ->
                       v.getPropertyPath().toString().equals("quantity")));

        // notes @Size violation must NOT appear — phase 3 was skipped
        assertTrue("Phase 3 @Size on notes must NOT be reported when BusinessChecks fails",
                   violations.stream().noneMatch(v ->
                       v.getPropertyPath().toString().equals("notes")));
    }

    /**
     * When all phases pass, phase 3 (un-grouped Default constraints) runs.
     * A notes value that exceeds the @Size limit triggers a phase-3 violation.
     */
    @Test
    public void testPhase3RunsWhenPhase1And2Pass() {
        String longNotes = "x".repeat(201); // exceeds @Size(max=200)
        Order order = new Order("PROD-001", 5, longNotes);

        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        assertEquals("Only the @Size violation on notes should be reported", 1, violations.size());
        assertEquals("notes", violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * Validates that the Order.class slot in the sequence is the third phase —
     * all three phases execute in order when all pass except the last.
     */
    @Test
    public void testSequenceOrderIsBasicThenBusinessThenDefault() {
        // Valid productId and quantity → phases 1 and 2 pass
        // Invalid notes → phase 3 fires
        Order order = new Order("PROD-001", 1, "x".repeat(201));

        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        // Phase 3 ran — notes violation present
        assertTrue("Phase 3 @Size on notes must fire when phases 1 and 2 pass",
                   violations.stream().anyMatch(v ->
                       v.getPropertyPath().toString().equals("notes")));

        // No phase 1 or 2 violations
        assertTrue("No BasicChecks violations expected",
                   violations.stream().noneMatch(v ->
                       v.getPropertyPath().toString().equals("productId")));
        assertTrue("No BusinessChecks violations expected",
                   violations.stream().noneMatch(v ->
                       v.getPropertyPath().toString().equals("quantity")));
    }

    // -----------------------------------------------------------------------
    // Section 2 — Inheritance + default group redefinition (BVAL-711 / #261)
    // -----------------------------------------------------------------------

    /**
     * A fully valid PriorityOrder produces no violations.
     * PriorityOrder inherits Order's @GroupSequence.
     */
    @Test
    public void testValidPriorityOrderProducesNoViolations() {
        PriorityOrder po = new PriorityOrder("PROD-001", 5, null, "HIGH");

        assertTrue("A valid PriorityOrder should produce no violations",
                   validator.validate(po).isEmpty());
    }

    /**
     * When BasicChecks fails on a PriorityOrder, the child's own @NotNull priority
     * constraint (which has no group = Default = phase 3) is NOT evaluated.
     *
     * <p>This is the core of the BVAL-711 clarification: the child's un-grouped
     * constraints slot into phase 3 of the INHERITED sequence, so they are
     * skipped when an earlier phase fails.
     */
    @Test
    public void testChildDefaultConstraintSkippedWhenPhase1Fails() {
        // productId null → BasicChecks (phase 1) fails
        // priority null  → would fail at phase 3, but phase 3 must be skipped
        PriorityOrder po = new PriorityOrder(null, 5, null, null);

        Set<ConstraintViolation<PriorityOrder>> violations = validator.validate(po);

        // Phase 1 violation on productId must appear
        assertTrue("BasicChecks violation on productId must be reported",
                   violations.stream().anyMatch(v ->
                       v.getPropertyPath().toString().equals("productId")));

        // Phase 3 violation on priority must NOT appear
        assertTrue("Phase 3 @NotNull on priority must NOT be reported when phase 1 fails",
                   violations.stream().noneMatch(v ->
                       v.getPropertyPath().toString().equals("priority")));
    }

    /**
     * When all inherited phases pass, the child's own Default constraint (@NotNull priority)
     * DOES run — confirming it slots into phase 3 of the inherited sequence.
     */
    @Test
    public void testChildDefaultConstraintRunsInPhase3() {
        // productId present, quantity valid → phases 1 and 2 pass
        // priority null → should fire at phase 3
        PriorityOrder po = new PriorityOrder("PROD-001", 5, null, null);

        Set<ConstraintViolation<PriorityOrder>> violations = validator.validate(po);

        assertEquals("Exactly one violation expected (@NotNull priority at phase 3)",
                     1, violations.size());
        assertEquals("priority",
                     violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * Confirms that only the child's phase-3 violation is reported when phases 1/2 pass,
     * with no spurious violations from other fields.
     */
    @Test
    public void testOnlyChildViolationReportedInPhase3() {
        PriorityOrder po = new PriorityOrder("PROD-001", 5, null, null);

        Set<ConstraintViolation<PriorityOrder>> violations = validator.validate(po);

        assertEquals("Only @NotNull priority should be reported", 1, violations.size());
        assertTrue("No productId violations expected",
                   violations.stream().noneMatch(v ->
                       v.getPropertyPath().toString().equals("productId")));
        assertTrue("No quantity violations expected",
                   violations.stream().noneMatch(v ->
                       v.getPropertyPath().toString().equals("quantity")));
    }
}
