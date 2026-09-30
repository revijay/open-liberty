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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link HasContent} on {@link CharSequence} values.
 *
 * <p>This is the <em>more specific</em> of the two registered validators for
 * the {@code @HasContent} constraint. When a {@code String} field is annotated
 * with {@code @HasContent}, the spec's type-validator resolution algorithm
 * selects this validator — not {@link HasContentValidatorForSerializable} —
 * because {@code CharSequence} is a more specific type for {@code String} than
 * {@code Serializable}.
 *
 * <p>Valid if the {@code CharSequence} is non-null and has length > 0.
 */
public class HasContentValidatorForCharSequence
        implements ConstraintValidator<HasContent, CharSequence> {

    /**
     * Flag set during validation — allows tests to confirm this validator
     * (and not the Serializable one) was invoked by the runtime.
     */
    static volatile boolean wasInvoked = false;

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        wasInvoked = true;
        if (value == null) {
            return true; // null handling is @NotNull's responsibility
        }
        return value.length() > 0;
    }
}
