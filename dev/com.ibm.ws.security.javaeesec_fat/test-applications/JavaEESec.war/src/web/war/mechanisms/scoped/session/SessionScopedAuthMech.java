/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package web.war.mechanisms.scoped.session;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import web.war.mechanisms.BaseAuthMech;

@SessionScoped
public class SessionScopedAuthMech extends BaseAuthMech implements Serializable {

    private static final long serialVersionUID = 1L;

    public SessionScopedAuthMech() {
        sourceClass = SessionScopedAuthMech.class.getName();
    }

}
