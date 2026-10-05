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
 * A simple order bean with a {@code @ValidUuid} constraint on its
 * {@code orderId} field.
 *
 * <p>The constraint annotation declares {@code validatedBy = {}} (empty).
 * The validator ({@link ValidUuidValidator}) is discovered solely via the
 * service-loader file {@code META-INF/services/jakarta.validation.ConstraintValidator}.
 * This is the core scenario for Jakarta Validation 4.0 issue #257.
 */
public class Order {

    @ValidUuid
    private final String orderId;

    public Order(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
