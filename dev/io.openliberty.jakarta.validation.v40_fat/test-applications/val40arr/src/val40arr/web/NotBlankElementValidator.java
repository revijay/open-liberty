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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link NotBlankElement}.
 *
 * <p>Rejects {@code null} and blank (empty or whitespace-only) strings.
 * This is applied per array element when {@code @NotBlankElement} is placed
 * as a type-use annotation on a {@code String[]} component type.
 */
public class NotBlankElementValidator
        implements ConstraintValidator<NotBlankElement, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return !value.trim().isEmpty();
    }
}
