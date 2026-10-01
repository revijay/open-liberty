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
package val40cascade.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * A customer bean demonstrating the three forms of {@code @Valid} cascading
 * on container fields — the subject of Jakarta Validation 4.0 issue #260.
 *
 * <h2>Three cascading styles on {@code List<Address>}</h2>
 *
 * <dl>
 *   <dt>{@link #legacyAddresses} — legacy placement</dt>
 *   <dd>{@code @Valid} on the field itself (Bean Validation 1.0 style).
 *       The runtime detects that {@code List} is a container and cascades
 *       into its elements automatically.</dd>
 *
 *   <dt>{@link #modernAddresses} — type-argument placement</dt>
 *   <dd>{@code @Valid} on the type argument (since Bean Validation 1.1 /
 *       Java 8). Explicitly states "validate elements of this list."
 *       This is the preferred modern form.</dd>
 *
 *   <dt>{@link #bothAddresses} — BOTH placements (undefined behaviour)</dt>
 *   <dd>Both {@code @Valid} on the field AND {@code @Valid} on the type
 *       argument. Jakarta Validation 4.0 (issue #260) declares this
 *       combination as having <em>undefined behaviour</em> and requires
 *       implementations to warn the user. In Hibernate Validator today,
 *       the two annotations are merged and each element is validated exactly
 *       once — but this is HV's private choice, not a spec guarantee.</dd>
 * </dl>
 */
public class Customer {

    @NotBlank(message = "name must not be blank")
    private final String name;

    /**
     * Legacy cascading — {@code @Valid} on the field.
     * HV auto-detects List is a container and cascades into Address elements.
     * This is Bean Validation 1.0 style — still works, but the modern
     * type-argument form is preferred.
     */
    @Valid
    private final List<Address> legacyAddresses;

    /**
     * Modern cascading — {@code @Valid} on the type argument.
     * Explicitly targets Address elements. Preferred since Java 8 / BV 1.1.
     */
    private final List<@Valid Address> modernAddresses;

    /**
     * Both placements on the same field — undefined behaviour per
     * Jakarta Validation 4.0 spec (issue #260).
     *
     * <p>In Hibernate Validator, the two are merged and each Address
     * is validated once. In a future spec version this will be a hard error.
     * Implementations are required to log a warning when this is detected.
     */
    @Valid
    private final List<@Valid Address> bothAddresses;

    public Customer(String name,
                    List<Address> legacyAddresses,
                    List<Address> modernAddresses,
                    List<Address> bothAddresses) {
        this.name            = name;
        this.legacyAddresses = legacyAddresses;
        this.modernAddresses = modernAddresses;
        this.bothAddresses   = bothAddresses;
    }

    public String getName()                  { return name; }
    public List<Address> getLegacyAddresses(){ return legacyAddresses; }
    public List<Address> getModernAddresses(){ return modernAddresses; }
    public List<Address> getBothAddresses()  { return bothAddresses; }
}
