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
 * <h2>XML registration path (issue #257)</h2>
 * <p>This annotation deliberately leaves {@code validatedBy} <b>empty</b>, just
 * like {@link ValidUuid}. The validator ({@link ValidUuidXmlValidator}) is bound
 * to this constraint exclusively via the {@code <constraint-definition>} element
 * in {@code WEB-INF/constraints-uuid-xml.xml}, which is listed as a
 * {@code <constraint-mapping>} inside {@code WEB-INF/validation.xml}.
 *
 * <p>The XML {@code <validated-by include-existing-validators="false">} element
 * with {@code include-existing-validators="false"} replaces any prior
 * {@code validatedBy} list entirely.  With {@code "true"} the XML list is additive.
 *
 * <p>Used by {@link Order2}.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUuid2 {

    String message() default "must be a valid UUID (xml-registered validator)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
