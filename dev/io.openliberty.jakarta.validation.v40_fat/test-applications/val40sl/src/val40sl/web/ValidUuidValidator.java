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
 * Validates that a {@link String} conforms to UUID format.
 *
 * <h2>Service-loader registration (issue #257)</h2>
 * <p>This validator is <b>not</b> listed in {@code @ValidUuid(validatedBy=...)}.
 * Instead it is declared in:
 * <pre>
 *   META-INF/services/jakarta.validation.ConstraintValidator
 * </pre>
 * A runtime that implements Jakarta Validation 4.0 issue #257 will discover
 * this class via {@link java.util.ServiceLoader} and bind it to
 * {@link ValidUuid} automatically, based on the generic type parameters
 * {@code <ValidUuid, String>}.
 *
 * <p>A runtime that does <em>not</em> yet implement issue #257 will not find
 * this validator and will throw {@link jakarta.validation.UnexpectedTypeException}
 * when {@code @ValidUuid} is applied to a {@code String} field.
 */
public class ValidUuidValidator implements ConstraintValidator<ValidUuid, String> {

    /**
     * Set to {@code true} whenever this validator is invoked.
     * Allows tests to assert that service-loader discovery worked and this
     * class — rather than some fallback — was actually called.
     */
    static volatile boolean wasInvoked = false;

    // Simple UUID pattern: 8-4-4-4-12 hex digits
    private static final java.util.regex.Pattern UUID_PATTERN =
        java.util.regex.Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        wasInvoked = true;
        if (value == null) {
            return true; // null handling is @NotNull's responsibility
        }
        return UUID_PATTERN.matcher(value).matches();
    }
}
