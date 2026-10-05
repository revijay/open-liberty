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
 * Validates {@code @ValidUuid3} on a {@link String} — registered via
 * {@code META-INF/services/jakarta.validation.ConstraintValidator} (service-loader path).
 *
 * <p>Used alongside {@link ValidUuidAnnotationValidator} (annotation path) and
 * an XML-registered validator in the precedence scenario ({@link Order3}) to
 * demonstrate all three registration mechanisms co-existing.
 */
public class ValidUuid3ServiceLoaderValidator implements ConstraintValidator<ValidUuid3, String> {

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
