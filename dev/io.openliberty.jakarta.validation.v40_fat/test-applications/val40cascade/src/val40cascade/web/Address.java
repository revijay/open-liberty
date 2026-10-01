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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * A simple address bean used as the cascaded validation target.
 *
 * <p>When {@code @Valid} is placed on a field that holds an {@code Address}
 * (directly or inside a container), the runtime recursively validates these
 * constraints on the {@code Address} object.
 */
public class Address {

    @NotBlank(message = "street must not be blank")
    private final String street;

    @NotBlank(message = "city must not be blank")
    private final String city;

    @Email(message = "contact must be a valid email")
    private final String contact;

    public Address(String street, String city, String contact) {
        this.street  = street;
        this.city    = city;
        this.contact = contact;
    }

    public String getStreet()  { return street; }
    public String getCity()    { return city; }
    public String getContact() { return contact; }
}
