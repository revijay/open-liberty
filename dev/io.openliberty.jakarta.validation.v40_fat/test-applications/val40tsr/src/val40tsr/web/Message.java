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
package val40tsr.web;

/**
 * A simple message bean with a {@code @HasContent} constraint on its
 * {@code text} field.
 *
 * <p>{@code String} implements both {@code CharSequence} and
 * {@code Serializable}. The {@code @HasContent} constraint has validators
 * registered for both interfaces, reproducing the Table 5.1 scenario from
 * BVAL-698.
 */
public class Message {

    @HasContent
    private final String text;

    public Message(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
