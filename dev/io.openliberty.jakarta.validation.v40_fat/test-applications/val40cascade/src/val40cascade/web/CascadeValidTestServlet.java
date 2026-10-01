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
package val40cascade.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * FAT servlet demonstrating Jakarta Validation 4.0 spec change (issue #260):
 * clarification of behaviour when legacy {@code @Valid} on a field and
 * type-argument {@code @Valid} are both present on the same element.
 *
 * <h2>Two styles of cascading</h2>
 *
 * <p><b>Legacy (Bean Validation 1.0):</b>
 * <pre>
 *   {@literal @}Valid
 *   private List&lt;Address&gt; addresses;
 * </pre>
 * The runtime detects that {@code List} is a container and cascades
 * into its elements automatically.
 *
 * <p><b>Modern (since Bean Validation 1.1 / Java 8):</b>
 * <pre>
 *   private List&lt;{@literal @}Valid Address&gt; addresses;
 * </pre>
 * Explicitly targets the type argument. Preferred form.
 *
 * <h2>The undefined-behaviour case (issue #260)</h2>
 * <p>Using both on the same field:
 * <pre>
 *   {@literal @}Valid
 *   private List&lt;{@literal @}Valid Address&gt; addresses;
 * </pre>
 * Jakarta Validation 4.0 declares this combination as having
 * <em>undefined behaviour</em> and requires implementations to warn the user.
 * In Hibernate Validator, the two annotations are merged and each element is
 * validated exactly once — but this is HV's private implementation choice,
 * not a spec guarantee. A future spec version will make this a hard error.
 *
 * <h2>Tests</h2>
 * <p>The tests below verify that cascading works correctly for all three
 * forms, and that the double-{@code @Valid} form does not cause double
 * validation in the current HV implementation.
 */
@SuppressWarnings("serial")
@WebServlet("/CascadeValidTestServlet")
public class CascadeValidTestServlet extends FATServlet {

    @Inject
    Validator validator;

    // -----------------------------------------------------------------------
    // Helper — valid and invalid Address instances
    // -----------------------------------------------------------------------

    private static Address validAddress() {
        return new Address("123 Main St", "Springfield", "user@example.com");
    }

    private static Address blankStreetAddress() {
        return new Address("", "Springfield", "user@example.com");
    }

    private static Address invalidEmailAddress() {
        return new Address("123 Main St", "Springfield", "not-an-email");
    }

    // -----------------------------------------------------------------------
    // Section 1 — Legacy @Valid (field-level placement, BV 1.0 style)
    // -----------------------------------------------------------------------

    /**
     * Legacy cascading — valid addresses produce no violations.
     */
    @Test
    public void testLegacyCascadeValidAddresses() {
        Customer c = new Customer("Alice",
                                  List.of(validAddress()),
                                  List.of(),
                                  List.of());

        assertTrue("Valid legacy addresses should produce no violations",
                   validator.validate(c).isEmpty());
    }

    /**
     * Legacy cascading — invalid Address inside the list is caught.
     * Demonstrates that field-level @Valid cascades into List elements.
     */
    @Test
    public void testLegacyCascadeDetectsInvalidAddress() {
        Customer c = new Customer("Alice",
                                  List.of(blankStreetAddress()),
                                  List.of(),
                                  List.of());

        Set<ConstraintViolation<Customer>> violations = validator.validate(c);

        assertEquals("One violation expected for blank street", 1, violations.size());
        assertTrue("Violation path must point inside legacyAddresses",
                   violations.iterator().next().getPropertyPath().toString()
                       .contains("legacyAddresses"));
    }

    /**
     * Legacy cascading — multiple invalid addresses each produce a violation.
     */
    @Test
    public void testLegacyCascadeDetectsMultipleInvalidAddresses() {
        Customer c = new Customer("Alice",
                                  List.of(blankStreetAddress(), invalidEmailAddress()),
                                  List.of(),
                                  List.of());

        Set<ConstraintViolation<Customer>> violations = validator.validate(c);

        assertEquals("Two violations expected — one per invalid address", 2, violations.size());
    }

    // -----------------------------------------------------------------------
    // Section 2 — Modern @Valid on type argument (BV 1.1+ / Java 8 style)
    // -----------------------------------------------------------------------

    /**
     * Modern type-argument cascading — valid addresses produce no violations.
     */
    @Test
    public void testModernCascadeValidAddresses() {
        Customer c = new Customer("Alice",
                                  List.of(),
                                  List.of(validAddress()),
                                  List.of());

        assertTrue("Valid modern addresses should produce no violations",
                   validator.validate(c).isEmpty());
    }

    /**
     * Modern type-argument cascading — invalid Address is caught.
     * Demonstrates that type-argument @Valid cascades into List elements.
     */
    @Test
    public void testModernCascadeDetectsInvalidAddress() {
        Customer c = new Customer("Alice",
                                  List.of(),
                                  List.of(blankStreetAddress()),
                                  List.of());

        Set<ConstraintViolation<Customer>> violations = validator.validate(c);

        assertEquals("One violation expected for blank street", 1, violations.size());
        assertTrue("Violation path must point inside modernAddresses",
                   violations.iterator().next().getPropertyPath().toString()
                       .contains("modernAddresses"));
    }

    // -----------------------------------------------------------------------
    // Section 3 — Both @Valid present (undefined behaviour / issue #260)
    // -----------------------------------------------------------------------

    /**
     * Double-@Valid — valid addresses produce no violations.
     * Works correctly in HV today even with both annotations present.
     */
    @Test
    public void testBothCascadeValidAddresses() {
        Customer c = new Customer("Alice",
                                  List.of(),
                                  List.of(),
                                  List.of(validAddress()));

        assertTrue("Valid addresses should produce no violations even with double @Valid",
                   validator.validate(c).isEmpty());
    }

    /**
     * Double-@Valid — each invalid address produces exactly ONE violation,
     * not two. In Hibernate Validator, the two @Valid annotations are merged
     * during metadata building, so cascade happens only once per element.
     *
     * <p>This is HV's implementation choice — the 4.0 spec declares this
     * combination as undefined behaviour, so other implementations may differ.
     */
    @Test
    public void testBothCascadeDoesNotDoubleValidate() {
        Customer c = new Customer("Alice",
                                  List.of(),
                                  List.of(),
                                  List.of(blankStreetAddress()));

        Set<ConstraintViolation<Customer>> violations = validator.validate(c);

        // If double-validation occurred, we would see 2 violations for the
        // same constraint on the same element. HV deduplicates → exactly 1.
        assertEquals("Each invalid element must produce exactly ONE violation " +
                     "(HV deduplicates double @Valid)",
                     1, violations.size());

        assertTrue("Violation path must point inside bothAddresses",
                   violations.iterator().next().getPropertyPath().toString()
                       .contains("bothAddresses"));
    }

    /**
     * Double-@Valid — both legacy and modern forms cascade into the same
     * elements, confirmed by checking violation property paths.
     */
    @Test
    public void testBothCascadeViolationPathIsCorrect() {
        Customer c = new Customer("Alice",
                                  List.of(),
                                  List.of(),
                                  List.of(invalidEmailAddress()));

        Set<ConstraintViolation<Customer>> violations = validator.validate(c);

        Set<String> paths = violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(Collectors.toSet());

        assertEquals("Exactly one violation expected", 1, violations.size());
        assertTrue("Violation must be inside bothAddresses[0].contact",
                   paths.stream().anyMatch(p -> p.contains("bothAddresses") && p.contains("contact")));
    }

    // -----------------------------------------------------------------------
    // Section 4 — Comparing legacy vs. modern behaviour side by side
    // -----------------------------------------------------------------------

    /**
     * Both legacy and modern cascading produce the same number of violations
     * for the same invalid input — confirming they are functionally equivalent
     * in HV today.
     */
    @Test
    public void testLegacyAndModernProduceSameViolationCount() {
        Address invalid = blankStreetAddress();

        Customer legacy = new Customer("Alice", List.of(invalid), List.of(), List.of());
        Customer modern = new Customer("Alice", List.of(), List.of(invalid), List.of());

        int legacyCount = validator.validate(legacy).size();
        int modernCount = validator.validate(modern).size();

        assertEquals("Legacy and modern cascading must produce the same violation count",
                     legacyCount, modernCount);
    }
}
