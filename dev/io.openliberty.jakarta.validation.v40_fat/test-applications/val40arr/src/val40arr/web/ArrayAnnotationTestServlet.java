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
package val40arr.web;

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
 * FAT servlet demonstrating array element annotation placement — the core
 * ambiguity being clarified in Jakarta Validation 4.0.
 *
 * <h2>The three forms</h2>
 *
 * <p><b>Form A — {@code @NotNull} on the array REFERENCE (FIELD annotation)</b>
 * <pre>
 *   {@literal @}NotNull
 *   private String[] referenceConstrained;
 * </pre>
 * {@code @NotNull} has {@code FIELD} in its {@code @Target} — it attaches to
 * the field declaration and asks: "is this array object null?" Elements inside
 * a non-null array are completely invisible to it.
 *
 * <p><b>Form B — {@code @NotBlankElement} on each ELEMENT (TYPE_USE annotation)</b>
 * <pre>
 *   private List&lt;{@literal @}NotBlankElement String&gt; elementConstrained;
 * </pre>
 * {@code @NotBlankElement} has only {@code TYPE_USE} — placed on the List type
 * argument, HV's {@code ListValueExtractor} iterates each element and validates
 * it independently. A null List reference does not fire this constraint.
 *
 * <p><b>Form C — both reference and element constraints together</b>
 * <pre>
 *   {@literal @}NotNull
 *   private List&lt;{@literal @}NotBlankElement String&gt; bothConstrained;
 * </pre>
 * Both constraints are active and independent of each other.
 *
 * <h2>Note on arrays vs List</h2>
 * <p>Raw Java arrays ({@code String[]}) don't have generic type parameters,
 * so Hibernate Validator's container element resolver cannot bind a custom
 * {@code TYPE_USE} constraint to a raw array component type. This is exactly
 * the spec gap Jakarta Validation 4.0 is addressing. {@code List<String>} is
 * used here to demonstrate the concept correctly today.
 */
@SuppressWarnings("serial")
@WebServlet("/ArrayAnnotationTestServlet")
public class ArrayAnnotationTestServlet extends FATServlet {

    @Inject
    Validator validator;

    // =========================================================================
    // Section 1 — Form A: @NotNull on the array REFERENCE
    // =========================================================================

    /**
     * Null array reference → @NotNull fires.
     */
    @Test
    public void testFormA_NullReference_Violation() {
        TaggedItem item = new TaggedItem(
                null,                         // referenceConstrained = null → @NotNull fires
                List.of("java", "ee"),        // elementConstrained — valid
                List.of("open"));             // bothConstrained — valid

        Set<ConstraintViolation<TaggedItem>> violations = validator.validate(item);

        assertEquals("Null array reference must produce exactly one @NotNull violation",
                     1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        assertEquals("Violation path must point to the referenceConstrained field",
                     "referenceConstrained", path);
    }

    /**
     * Non-null array reference with blank elements → @NotNull does NOT fire.
     * This is the key insight: Form A constrains the reference, not the elements.
     */
    @Test
    public void testFormA_NonNullArrayWithBlankElements_NoViolation() {
        TaggedItem item = new TaggedItem(
                new String[]{ "", "  ", null }, // non-null array, but blank/null elements
                List.of("java", "ee"),
                List.of("open"));

        assertTrue("@NotNull on the array reference must NOT catch blank/null elements inside a non-null array " +
                   "— it only checks whether the array object itself is null",
                   validator.validate(item).isEmpty());
    }

    /**
     * Valid non-null array with clean elements → no violation.
     */
    @Test
    public void testFormA_ValidReference_NoViolation() {
        TaggedItem item = new TaggedItem(
                new String[]{ "java", "ee" },
                List.of("java", "ee"),
                List.of("open"));

        assertTrue("A non-null array with valid elements must produce no violations",
                   validator.validate(item).isEmpty());
    }

    // =========================================================================
    // Section 2 — Form B: @NotBlankElement on each ELEMENT (TYPE_USE)
    // =========================================================================

    /**
     * List with a blank element → @NotBlankElement fires on that element.
     * HV's ListValueExtractor iterates each element independently.
     */
    @Test
    public void testFormB_BlankElement_Violation() {
        TaggedItem item = new TaggedItem(
                new String[]{ "java" },
                List.of("java", ""),   // element [1] is blank → violation
                List.of("open"));

        Set<ConstraintViolation<TaggedItem>> violations = validator.validate(item);

        assertEquals("One blank element must produce exactly one violation", 1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        System.out.println("TYPE_USE element violation path: " + path);

        assertTrue("Violation path must reference the elementConstrained field",
                   path.startsWith("elementConstrained"));
        assertTrue("Violation path must include the list index [1]",
                   path.contains("[1]"));
    }

    /**
     * List with multiple blank elements → one violation per bad element.
     */
    @Test
    public void testFormB_MultipleBlankElements_MultipleViolations() {
        TaggedItem item = new TaggedItem(
                new String[]{ "java" },
                List.of("good", "", "  ", "also"),   // [1] and [2] blank
                List.of("open"));

        Set<ConstraintViolation<TaggedItem>> violations = validator.validate(item);

        assertEquals("Two blank elements must produce exactly two violations", 2, violations.size());

        Set<String> paths = violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(Collectors.toSet());

        assertTrue("Violation must reference index [1]", paths.stream().anyMatch(p -> p.contains("[1]")));
        assertTrue("Violation must reference index [2]", paths.stream().anyMatch(p -> p.contains("[2]")));
    }

    /**
     * Null List reference with TYPE_USE element constraint → no violation.
     * TYPE_USE constrains elements, not the reference — null List = no elements to check.
     */
    @Test
    public void testFormB_NullReference_NoViolation() {
        TaggedItem item = new TaggedItem(
                new String[]{ "java" }, // satisfies @NotNull
                null,                   // elementConstrained = null → TYPE_USE does NOT fire
                List.of("open"));

        assertTrue("A null List with only a TYPE_USE element constraint must produce no violations " +
                   "— TYPE_USE constrains elements, not the reference",
                   validator.validate(item).isEmpty());
    }

    /**
     * Valid list with non-blank elements → no violation.
     */
    @Test
    public void testFormB_AllValidElements_NoViolation() {
        TaggedItem item = new TaggedItem(
                new String[]{ "java" },
                List.of("java", "ee", "liberty"),
                List.of("open"));

        assertTrue("All valid elements must produce no violations",
                   validator.validate(item).isEmpty());
    }

    // =========================================================================
    // Section 3 — Form C: BOTH reference and element constraints
    // =========================================================================

    /**
     * Null reference → @NotNull fires; element constraint cannot fire (no list to iterate).
     */
    @Test
    public void testFormC_NullReference_OnlyReferenceViolation() {
        TaggedItem item = new TaggedItem(
                new String[]{ "java" },
                List.of("ee"),
                null);   // bothConstrained = null → @NotNull fires

        Set<ConstraintViolation<TaggedItem>> violations = validator.validate(item);

        assertEquals("Null reference with both constraints must produce exactly one violation (the reference constraint)",
                     1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        assertEquals("Violation must point to bothConstrained",
                     "bothConstrained", path);
    }

    /**
     * Non-null list with a blank element → only the element constraint fires.
     * @NotNull satisfied (list is non-null); @NotBlankElement catches the blank element.
     */
    @Test
    public void testFormC_NonNullListWithBlankElement_OnlyElementViolation() {
        TaggedItem item = new TaggedItem(
                new String[]{ "java" },
                List.of("ee"),
                List.of(""));   // non-null list, but element [0] is blank

        Set<ConstraintViolation<TaggedItem>> violations = validator.validate(item);

        assertEquals("One blank element with both constraints must produce exactly one violation (the element constraint)",
                     1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        assertTrue("Violation path must reference bothConstrained field",
                   path.startsWith("bothConstrained"));
        assertTrue("Violation path must include the list index [0]",
                   path.contains("[0]"));
    }

    /**
     * Valid non-null list with clean elements → no violations from either constraint.
     */
    @Test
    public void testFormC_ValidList_NoViolation() {
        TaggedItem item = new TaggedItem(
                new String[]{ "java" },
                List.of("ee"),
                List.of("open", "liberty"));

        assertTrue("A valid non-null list with valid elements must produce no violations under Form C",
                   validator.validate(item).isEmpty());
    }

    // =========================================================================
    // Section 4 — The ambiguity demonstration
    // =========================================================================

    /**
     * Demonstrates exactly WHY the placement is confusing.
     *
     * <p>Both fields receive a list/array containing only a blank string.
     * The outcome is completely different:
     * <ul>
     *   <li>{@code referenceConstrained} has {@code @NotNull} (FIELD) — does NOT fire
     *       because the array is non-null. The blank element is invisible.</li>
     *   <li>{@code elementConstrained} has {@code @NotBlankElement} (TYPE_USE) — DOES fire
     *       because the blank element inside the list is checked.</li>
     * </ul>
     *
     * <p>Same visual placement, completely different semantics — determined entirely
     * by which {@code ElementType} is in the annotation's {@code @Target}.
     */
    @Test
    public void testAmbiguity_SameLookDifferentMeaning() {
        TaggedItem item = new TaggedItem(
                new String[]{ "  " },  // referenceConstrained — non-null, blank element, @NotNull doesn't care
                List.of("  "),         // elementConstrained — blank element → @NotBlankElement fires
                List.of("ok"));        // bothConstrained — valid

        Set<ConstraintViolation<TaggedItem>> violations = validator.validate(item);

        // Only ONE violation: from elementConstrained (TYPE_USE constraint)
        // referenceConstrained's @NotNull does not fire because the array is non-null
        assertEquals("Only the element-level (TYPE_USE) constraint fires for a blank element " +
                     "— NOT the field-level (FIELD) reference constraint",
                     1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        assertTrue("The violation must come from elementConstrained (TYPE_USE), not referenceConstrained (@NotNull)",
                   path.startsWith("elementConstrained"));
    }
}
