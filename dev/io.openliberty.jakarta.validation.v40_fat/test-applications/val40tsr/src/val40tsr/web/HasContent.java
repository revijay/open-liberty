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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Field-level constraint that asserts a value is non-empty / non-blank.
 *
 * <p>Two validators are registered — one for {@link CharSequence} and one for
 * {@link java.io.Serializable} — deliberately reproducing the scenario from
 * <b>Table 5.1</b> of the Jakarta Validation specification (BVAL-698):
 *
 * <pre>
 *   {@literal @}Constraint(validatedBy = {
 *       HasContentValidatorForCharSequence.class,   // handles CharSequence
 *       HasContentValidatorForSerializable.class    // handles Serializable
 *   })
 * </pre>
 *
 * <p>{@link String} implements <em>both</em> {@code CharSequence} and
 * {@code Serializable}. The spec's validator-resolution algorithm finds
 * {@code HasContentValidatorForCharSequence} as the most specific applicable
 * validator (because {@code CharSequence} is more specific than
 * {@code Serializable} for {@code String}) and selects it without throwing
 * an {@code UnexpectedTypeException}.
 *
 * <p>Table 5.1 in Bean Validation 2.0 / Jakarta Validation 3.x
 * incorrectly documented this as throwing an exception. Jakarta Validation 4.0
 * (BVAL-698) corrects the table to match what implementations have always done.
 */
@Documented
@Constraint(validatedBy = {
    HasContentValidatorForCharSequence.class,
    HasContentValidatorForSerializable.class
})
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface HasContent {

    String message() default "value must not be empty";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
