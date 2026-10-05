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
 * A subscription bean whose {@code subscriptionId} field is constrained by
 * {@link ValidUuid2} — a constraint whose validator is registered exclusively
 * via {@code <constraint-definition>} in a constraint-mappings XML file.
 *
 * <p>This bean is used to test the XML registration path described by
 * Jakarta Validation 4.0 issue #257.
 */
public class Order2 {

    @ValidUuid2
    private final String subscriptionId;

    public Order2(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }
}
