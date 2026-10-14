/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package bval.v20.el.web;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CustomParamValidator implements ConstraintValidator<CustomParamConstraint, String> {

    @Override
    public void initialize(CustomParamConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            ClassLoader cl = context.getClass().getClassLoader();
            Class<?> hvContextClass = cl.loadClass("org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext");
            Object hvContext = context.unwrap(hvContextClass);
            hvContextClass.getMethod("addMessageParameter", String.class, Object.class)
                    .invoke(hvContext, "userInput", value);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return !value.startsWith("INVALID");
    }
}
