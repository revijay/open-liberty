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

/**
 * A payment bean whose {@code paymentRef} field is constrained by
 * {@link ValidUuid3} — a constraint that has validators registered via all
 * three mechanisms simultaneously:
 * <ol>
 *   <li>{@code validatedBy} on the annotation ({@link ValidUuidAnnotationValidator})</li>
 *   <li>Service loader ({@code META-INF/services/jakarta.validation.ConstraintValidator})</li>
 *   <li>Constraint-mappings XML ({@code WEB-INF/constraints-uuid-precedence.xml})</li>
 * </ol>
 *
 * <p>This bean is used to test the precedence rules described by
 * Jakarta Validation 4.0 issue #257.
 */
public class Order3 {

    @ValidUuid3
    private final String paymentRef;

    public Order3(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public String getPaymentRef() {
        return paymentRef;
    }
}
