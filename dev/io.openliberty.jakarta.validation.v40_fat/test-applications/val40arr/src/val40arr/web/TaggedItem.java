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

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * Bean demonstrating the three distinct ways a constraint annotation can sit
 * on or near a container of strings — the core of the array element annotation
 * discussion in Jakarta Validation 4.0.
 *
 * <h2>Why List instead of String[]</h2>
 *
 * <p>Raw Java arrays ({@code String[]}) do not have generic type parameters.
 * Hibernate Validator's container element metadata resolver binds
 * {@code TYPE_USE} constraints to generic type variables — it cannot do so
 * for raw array component types with custom constraints. This is precisely
 * the spec gap Jakarta Validation 4.0 is addressing: annotation placement
 * rules for array component types are not yet fully specified or implemented
 * for custom constraints. {@code List<@NotBlankElement String>} is used here
 * to demonstrate the concept correctly today.
 *
 * <h2>Form A — {@code @NotNull} on the reference (FIELD)</h2>
 * <p>{@code @NotNull} has {@code FIELD} in its {@code @Target} — it attaches
 * to the field declaration and constrains whether the reference is null.
 * Elements inside a non-null array/list are invisible to it.
 *
 * <h2>Form B — {@code @NotBlankElement} on each element (TYPE_USE)</h2>
 * <p>{@code @NotBlankElement} has only {@code TYPE_USE} — placed on the
 * {@code List} type argument, it constrains each {@code String} element
 * independently via HV's {@code ListValueExtractor}.
 *
 * <h2>Form C — both constraints together</h2>
 * <p>Both active and independent: the reference must not be null and each
 * element must not be blank.
 */
public class TaggedItem {

    /**
     * Form A — {@code @NotNull} on the field (array reference / FIELD annotation).
     * Guards whether the reference is null; has no visibility into the elements.
     */
    @NotNull(message = "tags array must not be null")
    private final String[] referenceConstrained;

    /**
     * Form B — {@code @NotBlankElement} on the List type argument (TYPE_USE).
     * HV's ListValueExtractor iterates each String element and applies
     * NotBlankElementValidator independently per element.
     */
    private final List<@NotBlankElement String> elementConstrained;

    /**
     * Form C — both reference and element constraints together.
     * @NotNull guards the List reference; @NotBlankElement guards each element.
     */
    @NotNull(message = "bothConstrained list must not be null")
    private final List<@NotBlankElement String> bothConstrained;

    public TaggedItem(String[] referenceConstrained,
                      List<String> elementConstrained,
                      List<String> bothConstrained) {
        this.referenceConstrained = referenceConstrained;
        this.elementConstrained   = elementConstrained;
        this.bothConstrained      = bothConstrained;
    }

    public String[]     getReferenceConstrained() { return referenceConstrained; }
    public List<String> getElementConstrained()   { return elementConstrained; }
    public List<String> getBothConstrained()      { return bothConstrained; }
}
