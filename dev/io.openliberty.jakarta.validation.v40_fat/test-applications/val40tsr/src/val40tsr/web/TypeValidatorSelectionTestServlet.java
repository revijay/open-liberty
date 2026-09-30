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
package val40tsr.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * FAT servlet verifying the type-validator selection rule corrected by
 * Jakarta Validation 4.0 (BVAL-698).
 *
 * <h2>Background — Table 5.1 / BVAL-698</h2>
 * <p>The Jakarta Validation spec (section on validation routine / type validator
 * resolution, formerly Table 5.1) contained an incorrect example: it showed
 * that applying a constraint to a {@code String} field when validators were
 * registered for both {@code CharSequence} and {@code Serializable} would
 * result in an {@code UnexpectedTypeException}.
 *
 * <p>That was wrong. {@code String} implements both {@code CharSequence} and
 * {@code Serializable}. The resolution algorithm finds {@code CharSequence}
 * to be the more specific applicable type (it is a direct supertype of
 * {@code String} in the type hierarchy, while {@code Serializable} is a
 * marker interface), and therefore selects
 * {@link HasContentValidatorForCharSequence} without any exception.
 *
 * <p>Implementations (including Hibernate Validator) have always behaved
 * correctly. Jakarta Validation 4.0 (BVAL-698) corrects the spec text to
 * match what implementations have always done.
 *
 * <h2>Test setup</h2>
 * <p>{@link HasContent} has two validators:
 * <ul>
 *   <li>{@link HasContentValidatorForCharSequence} — handles {@code CharSequence}</li>
 *   <li>{@link HasContentValidatorForSerializable} — handles {@code Serializable}</li>
 * </ul>
 * {@link Message} has a {@code @HasContent String text} field. {@code String}
 * satisfies both validators; the runtime must pick the most specific one
 * ({@code CharSequence}) and succeed.
 */
@SuppressWarnings("serial")
@WebServlet("/TypeValidatorSelectionTestServlet")
public class TypeValidatorSelectionTestServlet extends FATServlet {

    @Inject
    Validator validator;

    // -----------------------------------------------------------------------
    // Core test — no UnexpectedTypeException
    // -----------------------------------------------------------------------

    /**
     * Validates that applying {@code @HasContent} to a {@code String} field
     * when both a {@code CharSequence} and a {@code Serializable} validator
     * are registered does <em>not</em> throw {@code UnexpectedTypeException}.
     *
     * <p>This is the direct counter-evidence to the incorrect Table 5.1 entry
     * that was corrected by BVAL-698 in Jakarta Validation 4.0.
     */
    @Test
    public void testNoUnexpectedTypeExceptionForStringField() {
        // Must not throw — if the old (wrong) spec behaviour were implemented
        // this would blow up with UnexpectedTypeException
        Set<ConstraintViolation<Message>> violations =
            validator.validate(new Message("hello"));

        assertTrue("Validation must succeed without UnexpectedTypeException",
                   violations.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Correct validator is selected
    // -----------------------------------------------------------------------

    /**
     * Confirms that the {@code CharSequence} validator — not the
     * {@code Serializable} validator — is invoked for a {@code String} field.
     *
     * <p>The resolution algorithm must select the most specific applicable
     * type. {@code CharSequence} is more specific than {@code Serializable}
     * for {@code String}, so {@link HasContentValidatorForCharSequence} wins.
     */
    @Test
    public void testCharSequenceValidatorSelectedForStringField() {
        // Reset flags immediately before this test so prior validations don't
        // interfere — avoids needing a @Before method (which FATServlet would
        // treat as a runnable test).
        HasContentValidatorForCharSequence.wasInvoked  = false;
        HasContentValidatorForSerializable.wasInvoked = false;

        validator.validate(new Message("hello"));

        assertTrue("CharSequence validator must be selected for a String field",
                   HasContentValidatorForCharSequence.wasInvoked);
        assertFalse("Serializable validator must NOT be selected when CharSequence is more specific",
                    HasContentValidatorForSerializable.wasInvoked);
    }

    // -----------------------------------------------------------------------
    // Validation correctness — the selected validator enforces the constraint
    // -----------------------------------------------------------------------

    /**
     * A non-empty string should produce no violations.
     */
    @Test
    public void testNonEmptyStringProducesNoViolations() {
        Set<ConstraintViolation<Message>> violations =
            validator.validate(new Message("OpenLiberty"));

        assertTrue("A non-empty string should produce no violations",
                   violations.isEmpty());
    }

    /**
     * An empty string should produce exactly one violation.
     */
    @Test
    public void testEmptyStringProducesViolation() {
        Set<ConstraintViolation<Message>> violations =
            validator.validate(new Message(""));

        assertEquals("An empty string should produce exactly one violation",
                     1, violations.size());
    }

    /**
     * A null value should produce no violations — null handling is
     * {@code @NotNull}'s responsibility, not {@code @HasContent}'s.
     */
    @Test
    public void testNullStringProducesNoViolations() {
        Set<ConstraintViolation<Message>> violations =
            validator.validate(new Message(null));

        assertTrue("A null value should produce no violations (@NotNull handles nulls)",
                   violations.isEmpty());
    }

    /**
     * The violation message should match the default template on
     * {@link HasContent}.
     */
    @Test
    public void testViolationMessageMatchesDefaultTemplate() {
        Set<ConstraintViolation<Message>> violations =
            validator.validate(new Message(""));

        assertEquals("Violation message must match the @HasContent default",
                     "value must not be empty",
                     violations.iterator().next().getMessage());
    }
}
