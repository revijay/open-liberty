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
 * {@code constraint-mappings XML} only (issue #257, XML registration path).
 *
 * <p>This validator is not listed in {@code @ValidUuid(validatedBy=...)} and is
 * not declared in {@code META-INF/services/jakarta.validation.ConstraintValidator}.
 * It is bound to {@link ValidUuid} exclusively through the {@code <constraint-definition>}
 * element in {@code WEB-INF/constraints-uuid-xml.xml}, which is referenced from
 * {@code WEB-INF/validation.xml}.
 *
 * <p>The XML {@code <validated-by>} element with {@code include-existing-validators="false"}
 * completely replaces the {@code validatedBy} list on the annotation. With
 * {@code include-existing-validators="true"} it would be additive.
 *
 * <p>Used by {@link Order2} in the XML-registration test scenarios.
 */
public class ValidUuidXmlValidator implements ConstraintValidator<ValidUuid, String> {

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
