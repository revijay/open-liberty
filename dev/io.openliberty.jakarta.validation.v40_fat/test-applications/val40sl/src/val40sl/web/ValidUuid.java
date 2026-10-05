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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Constraint that asserts a {@link String} value is a valid UUID.
 *
 * <h2>Issue #257 — service-loader validator declaration</h2>
 * <p>Jakarta Validation 4.0 issue #257 proposes that {@link jakarta.validation.ConstraintValidator}
 * implementations can be discovered via {@link java.util.ServiceLoader} rather than
 * being declared in {@code @Constraint(validatedBy=...)}. This allows third-party
 * libraries to contribute validators for constraints they do not own, without
 * modifying the constraint annotation source.
 *
 * <p>This annotation intentionally leaves {@code validatedBy} <b>empty</b>.
 * The validator ({@link ValidUuidValidator}) is registered exclusively through
 * the service-loader file:
 * <pre>
 *   META-INF/services/jakarta.validation.ConstraintValidator
 * </pre>
 * which contains a single line:
 * <pre>
 *   val40sl.web.ValidUuidValidator
 * </pre>
 *
 * <p>If the runtime honours issue #257, validation succeeds. If it does not,
 * applying {@code @ValidUuid} will throw {@link jakarta.validation.UnexpectedTypeException}
 * because no validator can be found for {@link String}.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUuid {

    String message() default "must be a valid UUID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
