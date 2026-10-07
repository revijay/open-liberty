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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Custom constraint that mirrors {@code @NotBlank} semantics, used to make
 * the three array annotation placement scenarios concrete and unambiguous.
 *
 * <p>Declared with <b>only {@code TYPE_USE}</b> in {@code @Target} — this is
 * the correct target for constraining an array element type argument.
 * Declaring {@code FIELD} here would make the placement ambiguous between
 * "constrain the array reference" and "constrain each element".
 *
 * <h2>Three placement forms on {@code String[]}</h2>
 * <pre>
 * // Form A — field-level @NotNull: constrains the array REFERENCE
 * {@literal @}NotNull
 * private String[] tags;
 *
 * // Form B — TYPE_USE on the component type: constrains each ELEMENT
 * private {@literal @}NotBlankElement String[] tags;
 *
 * // Form C — BOTH (undefined, similar to double-@Valid issue)
 * {@literal @}NotNull
 * private {@literal @}NotBlankElement String[] tags;
 * </pre>
 */
@Documented
@Constraint(validatedBy = { NotBlankElementValidator.class })
@Target({ ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBlankElement {

    String message() default "array element must not be blank";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
