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

/**
 * Validation group for basic / structural checks.
 *
 * <p>Constraints in this group run first in the {@link Order} group sequence.
 * If any of them fail, later groups ({@link BusinessChecks}) are skipped.
 *
 * <p>Typical use: null checks, size limits, format checks — things that must
 * pass before any business-rule validation makes sense.
 */
public interface BasicChecks {}
