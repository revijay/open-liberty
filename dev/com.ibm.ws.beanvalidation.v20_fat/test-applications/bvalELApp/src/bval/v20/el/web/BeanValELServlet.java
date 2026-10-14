/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package bval.v20.el.web;

import static componenttest.annotation.SkipForRepeat.EE9_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

/**
 * Bean Validation App to test Expression Language. There is behavior change with EL evaluation
 * between EE8 and EE9+, so this Servlet confirms the difference in behavior.
 */
@SuppressWarnings("serial")
@WebServlet("/BeanValELServlet")
public class BeanValELServlet extends FATServlet {

    @Inject
    Validator validator;

    @Inject
    ELBean bean;

    /**
     * Test that Expression Language can be evaluated in a validation method. This was changed
     * in Hibernate Validator 7.0, so it should only occur on EE8 (NO_MODIFICATION).
     */
    @SkipForRepeat(EE9_OR_LATER_FEATURES)
    @Test
    public void testELEvaluationEE8() {
        Set<ConstraintViolation<ELBean>> violations = validator.validate(bean);
        violations.iterator().forEachRemaining(v -> {
            assertEquals("Expression Language String should have evaluated to 3", "3", v.getMessage());
        });
    }

    /**
     * Test that Expression Language can not be evaluated in a validation method. This was changed
     * in Hibernate Validator 7.0, so it should not work on EE8 (NO_MODIFICATION).
     */
    @SkipForRepeat(NO_MODIFICATION)
    @Test
    public void testELEvaluationEE9plus() {
        Set<ConstraintViolation<ELBean>> violations = validator.validate(bean);
        violations.iterator().forEachRemaining(v -> {
            assertEquals("Expression Language String should not have been evaluated", "${1+2}", v.getMessage());
        });
    }

    /**
     * Demonstrates CVE-2026-93879 reproduction for @UniqueElements with {duplicates}.
     * On vulnerable versions: ${1+1} inside {duplicates} is evaluated to 2.
     * On fixed versions (HV-2256): ${1+1} is escaped and preserved as literal string "${1+1}".
     */
    @SkipForRepeat(EE9_OR_LATER_FEATURES)
    @Test
    public void testUniqueElementsELParameterEvaluation() {
        String payload = "${1+1}";
        UniqueElementsELBean testBean = new UniqueElementsELBean(java.util.Arrays.asList(payload, payload));

        Set<ConstraintViolation<UniqueElementsELBean>> violations = validator.validate(testBean);
        org.junit.Assert.assertFalse("There should be a constraint violation for duplicate elements", violations.isEmpty());

        String message = violations.iterator().next().getMessage();
        System.out.println("==========================================================");
        System.out.println("[VERIFICATION] UniqueElements violation message: " + message);
        System.out.println("==========================================================");
        org.junit.Assert.assertTrue("Parameter ${1+1} should NOT be evaluated as EL (should contain '${1+1}')", message.contains("${1+1}"));
        org.junit.Assert.assertFalse("Parameter ${1+1} must not evaluate to '2'", message.contains("2"));
    }

    /**
     * Demonstrates CVE-2026-93879 reproduction for custom validators using addMessageParameter.
     */
    @SkipForRepeat(EE9_OR_LATER_FEATURES)
    @Test
    public void testCustomParamValidatorEvaluation() {
        CustomParamBean testBean = new CustomParamBean("INVALID:${3*3}");

        Set<ConstraintViolation<CustomParamBean>> violations = validator.validate(testBean);
        org.junit.Assert.assertFalse("There should be a constraint violation", violations.isEmpty());

        String message = violations.iterator().next().getMessage();
        System.out.println("==========================================================");
        System.out.println("[VERIFICATION] CustomParam violation message: " + message);
        System.out.println("==========================================================");
        org.junit.Assert.assertTrue("Parameter ${3*3} should NOT be evaluated as EL (should contain '${3*3}')", message.contains("${3*3}"));
        org.junit.Assert.assertFalse("Parameter ${3*3} must not evaluate to '9'", message.contains("9"));
    }

}
