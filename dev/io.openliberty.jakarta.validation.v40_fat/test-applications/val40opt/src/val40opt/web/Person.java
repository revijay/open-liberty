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

import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A simple person bean with several {@link Optional} fields, each demonstrating
 * a different aspect of the Optional value-extractor path change in
 * Jakarta Validation 4.0 (section 4.3).
 *
 * <h2>The change</h2>
 * <p><b>Before 4.0</b>: the built-in {@code Optional} extractor was required to call
 * its extraction callback with {@code null} as the node name.  That {@code null}
 * suppressed the container node in the violation path, so a violation on
 * {@code Optional<String> nickname} was reported as {@code "nickname"} — the
 * Optional wrapper was invisible.
 *
 * <p><b>After 4.0</b>: the {@code null}-node requirement is removed.  Optional is
 * treated like any other container (List, Map…), so the same violation is
 * reported as {@code "nickname.<optional value>"} — the Optional wrapper is
 * visible in the path.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code nickname} — {@code @NotBlank} on {@code Optional<String>};
 *       core scenario for the path change</li>
 *   <li>{@code email} — {@code @Email} on {@code Optional<String>};
 *       shows the same behaviour with a different constraint</li>
 *   <li>{@code bio} — {@code @Size} on {@code Optional<String>};
 *       shows the behaviour when Optional IS present but the inner value fails</li>
 *   <li>{@code mandatoryField} — {@code @NotNull} directly on the
 *       {@code Optional} reference itself (not on the value inside);
 *       the path is always just {@code "mandatoryField"} regardless of spec version,
 *       because we are constraining the wrapper, not the contained value</li>
 * </ul>
 */
public class Person {

    /**
     * Core scenario: @NotBlank inside Optional.
     *
     * Pre-4.0 violation path:  "nickname"
     * Post-4.0 violation path: "nickname.<optional value>"
     */
    @NotBlank
    private final Optional<String> nickname;

    /**
     * @Email inside Optional — same path-change behaviour.
     *
     * Pre-4.0 violation path:  "email"
     * Post-4.0 violation path: "email.<optional value>"
     */
    @Email
    private final Optional<String> email;

    /**
     * @Size inside Optional — Optional IS present, but inner value violates.
     *
     * Pre-4.0 violation path:  "bio"
     * Post-4.0 violation path: "bio.<optional value>"
     */
    @Size(min = 10, max = 200)
    private final Optional<String> bio;

    /**
     * @NotNull on the Optional reference itself — constrains the wrapper,
     * not the value inside it.  Path is always "mandatoryField" in both
     * pre-4.0 and post-4.0 because no value-extraction happens here.
     */
    @NotNull
    private final Optional<String> mandatoryField;

    public Person(String nickname,
                  String email,
                  String bio,
                  Optional<String> mandatoryField) {
        this.nickname       = nickname == null ? Optional.empty() : Optional.of(nickname);
        this.email          = email    == null ? Optional.empty() : Optional.of(email);
        this.bio            = bio      == null ? Optional.empty() : Optional.of(bio);
        this.mandatoryField = mandatoryField;
    }

    public Optional<String> getNickname()       { return nickname; }
    public Optional<String> getEmail()          { return email; }
    public Optional<String> getBio()            { return bio; }
    public Optional<String> getMandatoryField() { return mandatoryField; }
}
