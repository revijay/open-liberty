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
package val40opt.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * FAT servlet demonstrating the {@code Optional} value-extractor violation-path
 * change in Jakarta Validation 4.0, section 4.3.
 *
 * <h2>The change in one sentence</h2>
 * <p>Before 4.0, a constraint violation on a value <em>inside</em> an
 * {@code Optional} field was reported at the <em>field</em> path
 * (e.g. {@code "nickname"}).  After 4.0, it is reported at the
 * <em>container-element</em> path (e.g. {@code "nickname.<optional value>"}).
 *
 * <h2>Test structure</h2>
 * <ul>
 *   <li><b>Section 1</b> — Current behaviour (pre-4.0 / validation-3.1):
 *       violation path is just the field name.</li>
 *   <li><b>Section 2</b> — Post-4.0 expected behaviour: violation path
 *       includes the {@code <optional value>} container node.</li>
 *   <li><b>Section 3</b> — Edge cases: null Optional reference,
 *       Optional.empty(), valid value, and constraining the wrapper itself.</li>
 * </ul>
 *
 * <h2>Important</h2>
 * <p>Section 2 tests are written for <em>post-4.0</em> runtime behaviour.
 * They will FAIL against {@code validation-3.1} (current server.xml) because
 * the old runtime still suppresses the {@code <optional value>} node.
 * They will PASS once the runtime is updated to implement the 4.0 change.
 * This is intentional — it documents what needs to change.
 */
@SuppressWarnings("serial")
@WebServlet("/OptionalViolationPathTestServlet")
public class OptionalViolationPathTestServlet extends FATServlet {

    @Inject
    Validator validator;

    // =========================================================================
    // Section 1 — Current behaviour (pre-4.0 / validation-3.1)
    // Violation path = just the field name.  Optional wrapper is invisible.
    // =========================================================================

    /**
     * @NotBlank on Optional.empty() — field-level path (pre-4.0).
     *
     * <p>The Optional is empty, so the extracted value is null/blank.
     * Pre-4.0 reports the violation at "nickname", not "nickname.<optional value>".
     */
    @Test
    public void testPre40_EmptyOptional_PathIsFieldName() {
        // nickname = Optional.empty() → @NotBlank fails
        Person p = new Person(null, "user@example.com", "A long enough bio here!", Optional.of("x"));

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertEquals("Expected exactly one violation", 1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        System.out.println("Pre-4.0 Optional violation path: " + path);

        // PRE-4.0 behaviour: path is just the field name
        assertEquals("Pre-4.0: violation path must be the field name only",
                     "nickname", path);
    }

    /**
     * @Email on Optional containing an invalid email — field-level path (pre-4.0).
     */
    @Test
    public void testPre40_InvalidEmail_PathIsFieldName() {
        // email = Optional.of("not-an-email") → @Email fails
        Person p = new Person("nick", "not-an-email", "A long enough bio here!", Optional.of("x"));

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertEquals("Expected exactly one violation", 1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        System.out.println("Pre-4.0 @Email Optional violation path: " + path);

        assertEquals("Pre-4.0: violation path must be the field name only",
                     "email", path);
    }

    /**
     * @Size on Optional containing a too-short string — field-level path (pre-4.0).
     *
     * <p>The Optional IS present (not empty), but the value inside fails @Size.
     */
    @Test
    public void testPre40_SizeViolation_PathIsFieldName() {
        // bio = Optional.of("short") → @Size(min=10) fails
        Person p = new Person("nick", "user@example.com", "short", Optional.of("x"));

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertEquals("Expected exactly one violation", 1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        System.out.println("Pre-4.0 @Size Optional violation path: " + path);

        assertEquals("Pre-4.0: violation path must be the field name only",
                     "bio", path);
    }

    // =========================================================================
    // Section 2 — Post-4.0 expected behaviour
    // Violation path = "fieldName.<optional value>".  Optional wrapper visible.
    //
    // NOTE: These tests FAIL on validation-3.1.
    //       They will PASS once the runtime implements the 4.0 change.
    // =========================================================================

    /**
     * @NotBlank on Optional.empty() — container-element path (post-4.0).
     *
     * <p>After 4.0, the violation path includes the container node, matching
     * how {@code List} violations are reported (e.g. {@code "tags[0]"}).
     */
    @Test
    public void testPost40_EmptyOptional_PathIncludesOptionalNode() {
        Person p = new Person(null, "user@example.com", "A long enough bio here!", Optional.of("x"));

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertEquals("Expected exactly one violation", 1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        System.out.println("Post-4.0 Optional violation path: " + path);

        // POST-4.0 behaviour: path includes the <optional value> container node
        assertEquals("Post-4.0: violation path must include the container node",
                     "nickname.<optional value>", path);
    }

    /**
     * @Email violation inside Optional — container-element path (post-4.0).
     */
    @Test
    public void testPost40_InvalidEmail_PathIncludesOptionalNode() {
        Person p = new Person("nick", "not-an-email", "A long enough bio here!", Optional.of("x"));

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertEquals("Expected exactly one violation", 1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();

        assertEquals("Post-4.0: @Email violation path must include <optional value>",
                     "email.<optional value>", path);
    }

    /**
     * @Size violation inside Optional — container-element path (post-4.0).
     */
    @Test
    public void testPost40_SizeViolation_PathIncludesOptionalNode() {
        Person p = new Person("nick", "user@example.com", "short", Optional.of("x"));

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertEquals("Expected exactly one violation", 1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();

        assertEquals("Post-4.0: @Size violation path must include <optional value>",
                     "bio.<optional value>", path);
    }

    // =========================================================================
    // Section 3 — Edge cases (behaviour is same in pre-4.0 and post-4.0)
    // =========================================================================

    /**
     * Valid person — no violations regardless of spec version.
     */
    @Test
    public void testNoViolations_ValidPerson() {
        Person p = new Person("nick", "user@example.com",
                              "A long enough bio that satisfies the size constraint!", Optional.of("x"));

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertTrue("A fully valid Person must produce no violations",
                   violations.isEmpty());
    }

    /**
     * {@code @NotNull} on the Optional reference itself (not on the value inside).
     *
     * <p>This constrains the <em>wrapper object</em>, not the contained value.
     * No value-extraction happens — path is always just {@code "mandatoryField"}
     * in both pre-4.0 and post-4.0.
     */
    @Test
    public void testNotNullOnOptionalReference_PathIsAlwaysFieldName() {
        // mandatoryField = null (the Optional reference itself is null, not empty)
        Person p = new Person("nick", "user@example.com",
                              "A long enough bio here!", null);

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertEquals("Expected exactly one violation", 1, violations.size());

        String path = violations.iterator().next().getPropertyPath().toString();
        System.out.println("@NotNull on Optional reference path: " + path);

        // Same in pre-4.0 AND post-4.0 — we are constraining the wrapper, not the value
        assertEquals("@NotNull on the Optional reference must always report the field name",
                     "mandatoryField", path);
    }

    /**
     * Optional.empty() with @NotNull on the Optional reference — no violation.
     *
     * <p>{@code Optional.empty()} is a valid (non-null) {@code Optional} object.
     * {@code @NotNull} passes because the reference is not null.
     */
    @Test
    public void testOptionalEmpty_NotNullOnReference_NoViolation() {
        // mandatoryField = Optional.empty() — the reference is NOT null, so @NotNull passes
        Person p = new Person("nick", "user@example.com",
                              "A long enough bio here!", Optional.empty());

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertTrue("Optional.empty() must not violate @NotNull on the reference",
                   violations.isEmpty());
    }

    /**
     * Multiple violations — confirms each Optional field reports its own path.
     */
    @Test
    public void testMultipleViolations_EachFieldReportsOwnPath() {
        // nickname empty → @NotBlank fails
        // email invalid  → @Email fails
        Person p = new Person(null, "bad-email",
                              "A long enough bio here!", Optional.of("x"));

        Set<ConstraintViolation<Person>> violations = validator.validate(p);

        assertEquals("Expected exactly two violations", 2, violations.size());

        boolean hasNickname = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().startsWith("nickname"));
        boolean hasEmail = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().startsWith("email"));

        assertTrue("One violation must reference the nickname field", hasNickname);
        assertTrue("One violation must reference the email field", hasEmail);
    }
}
