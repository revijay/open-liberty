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
 * Validation group for business-rule checks.
 *
 * <p>Constraints in this group run second in the {@link Order} group sequence,
 * only after all {@link BasicChecks} constraints pass.
 *
 * <p>Typical use: range checks, cross-field rules, domain-specific constraints
 * that are only meaningful once the basic data is already sane.
 */
public interface BusinessChecks {}
