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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

public class UniqueElementsELBean {

    @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = UniqueElementsSimulatorValidator.class)
    @Documented
    public @interface UniqueElementsSimulator {
        String message() default "Duplicate entries found: {duplicates}";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class UniqueElementsSimulatorValidator implements ConstraintValidator<UniqueElementsSimulator, Collection<?>> {
        @Override
        public void initialize(UniqueElementsSimulator constraintAnnotation) {
        }

        @Override
        public boolean isValid(Collection<?> value, ConstraintValidatorContext context) {
            if (value == null || value.size() < 2) {
                return true;
            }
            Set<Object> set = new HashSet<>();
            Set<Object> duplicates = new HashSet<>();
            for (Object obj : value) {
                if (!set.add(obj)) {
                    duplicates.add(obj);
                }
            }
            if (duplicates.isEmpty()) {
                return true;
            }

            try {
                ClassLoader cl = context.getClass().getClassLoader();
                Class<?> hvContextClass = cl.loadClass("org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext");
                Object hvContext = context.unwrap(hvContextClass);
                hvContextClass.getMethod("addMessageParameter", String.class, Object.class)
                        .invoke(hvContext, "duplicates", duplicates.toString());
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return false;
        }
    }

    @UniqueElementsSimulator
    private Collection<String> items;

    public UniqueElementsELBean(Collection<String> items) {
        this.items = items;
    }

    public Collection<String> getItems() {
        return items;
    }

    public void setItems(Collection<String> items) {
        this.items = items;
    }
}
