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
 * <h2>All-three-present precedence test (issue #257)</h2>
 * <p>This annotation lists {@link ValidUuidAnnotationValidator} in
 * {@code validatedBy} — the traditional annotation-based path.
 *
 * <p>At the same time:
 * <ul>
 *   <li>A service-loader validator for this constraint is listed in
 *       {@code META-INF/services/jakarta.validation.ConstraintValidator}</li>
 *   <li>An XML validator is bound via {@code WEB-INF/constraints-uuid-precedence.xml}</li>
 * </ul>
 *
 * <h2>Spec-defined precedence</h2>
 * <pre>
 *   XML (include-existing-validators="false")  →  XML list only (annotation + SL discarded)
 *   XML (include-existing-validators="true")   →  annotation list + SL list + XML list
 *   No XML                                     →  annotation list merged with SL list
 * </pre>
 *
 * <p>Used by {@link Order3}.
 */
@Documented
@Constraint(validatedBy = { ValidUuidAnnotationValidator.class })
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUuid3 {

    String message() default "must be a valid UUID (precedence test)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
