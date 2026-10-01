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

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * An order bean that demonstrates Jakarta Validation group sequencing.
 *
 * <h2>Group sequence</h2>
 * <p>The {@code @GroupSequence} on this class redefines what "Default" means:
 * <ol>
 *   <li>{@link BasicChecks} — null and blank checks run first</li>
 *   <li>{@link BusinessChecks} — business-rule checks run second</li>
 *   <li>{@code Order.class} — any un-grouped constraints on Order run last</li>
 * </ol>
 *
 * <p>If any phase produces violations the remaining phases are skipped entirely.
 *
 * <h2>Field breakdown</h2>
 * <ul>
 *   <li>{@code productId} — {@link BasicChecks}: must be non-null and non-blank</li>
 *   <li>{@code quantity}  — {@link BasicChecks}: must be non-null;
 *                           {@link BusinessChecks}: must be >= 1</li>
 *   <li>{@code notes}     — no group = Default = runs at the Order.class slot (phase 3)</li>
 * </ul>
 */
@GroupSequence({ BasicChecks.class, BusinessChecks.class, Order.class })
public class Order {

    /** Must be present and non-blank — BasicChecks (phase 1). */
    @NotNull(groups = BasicChecks.class)
    @NotBlank(groups = BasicChecks.class)
    private final String productId;

    /** Must be present — BasicChecks (phase 1).
     *  Must be at least 1 — BusinessChecks (phase 2). */
    @NotNull(groups = BasicChecks.class)
    @Min(value = 1, groups = BusinessChecks.class)
    private final Integer quantity;

    /**
     * Optional free-text notes — no group specified, so belongs to Default.
     * Runs at the Order.class position in the sequence (phase 3).
     * If present, must not exceed 200 characters.
     */
    @jakarta.validation.constraints.Size(max = 200)
    private final String notes;

    public Order(String productId, Integer quantity, String notes) {
        this.productId = productId;
        this.quantity  = quantity;
        this.notes     = notes;
    }

    public String getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public String getNotes()     { return notes; }
}
