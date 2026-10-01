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
package val40grp.web;

import jakarta.validation.constraints.NotNull;

/**
 * A priority order that extends {@link Order} — demonstrates Jakarta Validation
 * 4.0 spec clarification BVAL-711 (issue #261): inheritance + default group
 * redefinition.
 *
 * <h2>What this class demonstrates</h2>
 * <p>{@code Order} defines a {@code @GroupSequence({ BasicChecks, BusinessChecks, Order.class })}.
 * {@code PriorityOrder} adds its own field {@code priority} with no explicit group,
 * meaning it belongs to the {@code Default} group.
 *
 * <p>The spec clarification (BVAL-711) formalises that the child's own Default
 * constraints slot in at the position where the parent class ({@code Order.class})
 * appears in the inherited sequence — i.e. phase 3. So the effective execution
 * order when validating a {@code PriorityOrder} is:
 * <ol>
 *   <li>BasicChecks (from Order fields)</li>
 *   <li>BusinessChecks (from Order fields)</li>
 *   <li>Order.class position — Order's own Default constraints AND
 *       PriorityOrder's own Default constraints (including {@code @NotNull priority})</li>
 * </ol>
 *
 * <p>If phase 1 or 2 produces violations, phase 3 is never reached — so
 * {@code @NotNull priority} is never checked if {@code productId} is null.
 */
public class PriorityOrder extends Order {

    /**
     * Priority level for this order.
     *
     * <p>No group specified — belongs to Default — runs at the Order.class
     * slot (phase 3) in the inherited group sequence. If BasicChecks or
     * BusinessChecks fail, this constraint is never evaluated.
     */
    @NotNull
    private final String priority;

    public PriorityOrder(String productId, Integer quantity, String notes, String priority) {
        super(productId, quantity, notes);
        this.priority = priority;
    }

    public String getPriority() { return priority; }
}
