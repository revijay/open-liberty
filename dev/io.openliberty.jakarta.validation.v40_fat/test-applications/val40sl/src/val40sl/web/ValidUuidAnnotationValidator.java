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
package val40sl.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates {@code @ValidUuid} on a {@link String} — registered via
 * {@code @Constraint(validatedBy=...)} on the annotation itself.
 *
 * <p>This is the <em>traditional</em> registration path. It is used in the
 * <strong>precedence test</strong> ({@link Order3}) where all three registration
 * methods are present simultaneously:
 * <ol>
 *   <li>{@link ValidUuidAnnotationValidator} — listed in {@code validatedBy} on {@link ValidUuid3}</li>
 *   <li>{@link ValidUuidValidator} — declared in
 *       {@code META-INF/services/jakarta.validation.ConstraintValidator}</li>
 *   <li>{@link ValidUuidXmlValidator} — declared in {@code constraint-mappings XML}</li>
 * </ol>
 *
 * <h2>Precedence (Jakarta Validation spec)</h2>
 * <p>When all three are present for the same constraint on the same type:
 * <ul>
 *   <li><strong>XML wins</strong> when {@code include-existing-validators="false"} — all
 *       annotation-declared and service-loader validators are discarded and only the XML
 *       list is used.</li>
 *   <li><strong>XML is additive</strong> when {@code include-existing-validators="true"} —
 *       the XML list is appended to the combined annotation + service-loader list.</li>
 *   <li>When XML is absent, <strong>service-loader validators are merged</strong> with the
 *       {@code validatedBy} list.</li>
 * </ul>
 *
 * <p>Used by {@link Order3} in the precedence test scenarios.
 */
public class ValidUuidAnnotationValidator implements ConstraintValidator<ValidUuid3, String> {

    /** Set to {@code true} whenever this validator is invoked. */
    static volatile boolean wasInvoked = false;

    private static final java.util.regex.Pattern UUID_PATTERN =
        java.util.regex.Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        wasInvoked = true;
        if (value == null) {
            return true;
        }
        return UUID_PATTERN.matcher(value).matches();
    }
}
