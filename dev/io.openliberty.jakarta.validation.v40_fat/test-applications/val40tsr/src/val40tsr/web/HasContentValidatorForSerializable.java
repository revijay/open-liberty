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

import java.io.Serializable;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link HasContent} on {@link Serializable} values.
 *
 * <p>This is the <em>less specific</em> of the two registered validators for
 * the {@code @HasContent} constraint. When a {@code String} field is annotated
 * with {@code @HasContent}, the spec's type-validator resolution algorithm
 * selects {@link HasContentValidatorForCharSequence} instead of this class,
 * because {@code CharSequence} is more specific than {@code Serializable} for
 * {@code String}.
 *
 * <p>This validator should <b>never</b> be invoked for a {@code String} field —
 * the tests assert that only the {@code CharSequence} validator runs.
 *
 * <p>Valid if the {@code Serializable} is non-null (length not checkable at
 * this level — only used as a fallback for non-CharSequence Serializable types).
 */
public class HasContentValidatorForSerializable
        implements ConstraintValidator<HasContent, Serializable> {

    /**
     * Flag set during validation — tests assert this is <em>never</em> set
     * for a {@code String} field (the CharSequence validator should be used).
     */
    static volatile boolean wasInvoked = false;

    @Override
    public boolean isValid(Serializable value, ConstraintValidatorContext context) {
        wasInvoked = true;
        return value != null;
    }
}
