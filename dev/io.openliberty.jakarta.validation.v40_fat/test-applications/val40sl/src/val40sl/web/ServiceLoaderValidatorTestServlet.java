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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * FAT servlet covering all three {@link jakarta.validation.ConstraintValidator}
 * registration mechanisms proposed by Jakarta Validation 4.0 issue #257, and
 * the spec-defined precedence rules when multiple mechanisms are combined.
 *
 * <h2>Three registration mechanisms</h2>
 * <table border="1">
 *   <tr><th>Mechanism</th><th>How</th><th>Constraint used</th><th>Validator</th></tr>
 *   <tr>
 *     <td>1. Service Loader</td>
 *     <td>{@code META-INF/services/jakarta.validation.ConstraintValidator}</td>
 *     <td>{@link ValidUuid}</td>
 *     <td>{@link ValidUuidValidator}</td>
 *   </tr>
 *   <tr>
 *     <td>2. XML ({@code <constraint-definition>})</td>
 *     <td>{@code WEB-INF/constraints-uuid-xml.xml} via {@code WEB-INF/validation.xml}</td>
 *     <td>{@link ValidUuid2}</td>
 *     <td>{@link ValidUuidXmlValidator}</td>
 *   </tr>
 *   <tr>
 *     <td>3. Annotation ({@code validatedBy})</td>
 *     <td>{@code @Constraint(validatedBy = { ValidUuidAnnotationValidator.class })}</td>
 *     <td>{@link ValidUuid3}</td>
 *     <td>{@link ValidUuidAnnotationValidator}</td>
 *   </tr>
 * </table>
 *
 * <h2>Precedence rules (spec-defined)</h2>
 * <p>{@link ValidUuid3} has all three mechanisms active at once. The behaviour is
 * controlled by {@code include-existing-validators} in the XML mapping:
 * <ul>
 *   <li>{@code "false"} (used in {@code constraints-uuid-precedence.xml}):
 *       XML replaces everything — only {@link ValidUuid3XmlValidator} is used.</li>
 *   <li>{@code "true"}: XML is additive — annotation + service-loader + XML validators
 *       are all in the resolution set (runtime picks by type specificity).</li>
 *   <li>No XML: annotation list and service-loader list are merged.</li>
 * </ul>
 */
@SuppressWarnings("serial")
@WebServlet("/ServiceLoaderValidatorTestServlet")
public class ServiceLoaderValidatorTestServlet extends FATServlet {

    @Inject
    Validator validator;

    // =======================================================================
    // Section 1 — Service-loader registration (@ValidUuid / Order)
    // Validator declared only in META-INF/services/jakarta.validation.ConstraintValidator
    // =======================================================================

    /**
     * A well-formed UUID must produce no violations when the validator is
     * discovered exclusively via ServiceLoader.
     *
     * <p>If service-loader discovery is not implemented, the runtime will throw
     * {@link jakarta.validation.UnexpectedTypeException} because no validator is
     * bound to {@code String} in {@code @ValidUuid(validatedBy={})}.
     */
    @Test
    public void testServiceLoader_ValidUuidProducesNoViolations() {
        Set<ConstraintViolation<Order>> violations =
            validator.validate(new Order("550e8400-e29b-41d4-a716-446655440000"));

        assertTrue("A well-formed UUID must produce no violations (service-loader path)",
                   violations.isEmpty());
    }

    /**
     * Confirms the service-loader-discovered validator was actually invoked.
     */
    @Test
    public void testServiceLoader_ValidatorWasInvoked() {
        ValidUuidValidator.wasInvoked = false;

        validator.validate(new Order("550e8400-e29b-41d4-a716-446655440000"));

        assertTrue("ValidUuidValidator must be invoked — confirming service-loader discovery",
                   ValidUuidValidator.wasInvoked);
    }

    /**
     * A malformed UUID must produce exactly one violation.
     */
    @Test
    public void testServiceLoader_MalformedUuidProducesViolation() {
        Set<ConstraintViolation<Order>> violations =
            validator.validate(new Order("not-a-uuid"));

        assertEquals("A non-UUID string must produce exactly one violation (service-loader path)",
                     1, violations.size());
    }

    /**
     * Null is skipped — null handling belongs to {@code @NotNull}.
     */
    @Test
    public void testServiceLoader_NullProducesNoViolations() {
        assertTrue("Null must produce no violations (service-loader path)",
                   validator.validate(new Order(null)).isEmpty());
    }

    // =======================================================================
    // Section 2 — XML registration (@ValidUuid2 / Order2)
    // Validator bound via <constraint-definition> in constraints-uuid-xml.xml
    // =======================================================================

    /**
     * A well-formed UUID must produce no violations when the validator is
     * bound via XML {@code <constraint-definition>}.
     *
     * <p>{@link ValidUuid2} has {@code validatedBy={}} (empty). The XML file
     * {@code WEB-INF/constraints-uuid-xml.xml} uses:
     * <pre>
     *   &lt;constraint-definition annotation="val40sl.web.ValidUuid2"&gt;
     *     &lt;validated-by include-existing-validators="false"&gt;
     *       &lt;value&gt;val40sl.web.ValidUuidXmlValidator&lt;/value&gt;
     *     &lt;/validated-by&gt;
     *   &lt;/constraint-definition&gt;
     * </pre>
     */
    @Test
    public void testXml_ValidUuidProducesNoViolations() {
        Set<ConstraintViolation<Order2>> violations =
            validator.validate(new Order2("550e8400-e29b-41d4-a716-446655440000"));

        assertTrue("A well-formed UUID must produce no violations (XML registration path)",
                   violations.isEmpty());
    }

    /**
     * Confirms the XML-registered validator was actually invoked.
     */
    @Test
    public void testXml_ValidatorWasInvoked() {
        ValidUuidXmlValidator.wasInvoked = false;

        validator.validate(new Order2("550e8400-e29b-41d4-a716-446655440000"));

        assertTrue("ValidUuidXmlValidator must be invoked — confirming XML constraint-definition",
                   ValidUuidXmlValidator.wasInvoked);
    }

    /**
     * A malformed UUID must produce exactly one violation (XML path).
     */
    @Test
    public void testXml_MalformedUuidProducesViolation() {
        Set<ConstraintViolation<Order2>> violations =
            validator.validate(new Order2("not-a-uuid"));

        assertEquals("A non-UUID string must produce exactly one violation (XML registration path)",
                     1, violations.size());
    }

    /**
     * Violation message must match the default on {@link ValidUuid2}.
     */
    @Test
    public void testXml_ViolationMessageMatchesConstraintDefault() {
        Set<ConstraintViolation<Order2>> violations =
            validator.validate(new Order2("bad"));

        assertFalse("Expected at least one violation", violations.isEmpty());
        assertEquals("must be a valid UUID (xml-registered validator)",
                     violations.iterator().next().getMessage());
    }

    // =======================================================================
    // Section 3 — All three present + precedence (@ValidUuid3 / Order3)
    //
    // @ValidUuid3 has:
    //   - validatedBy = { ValidUuidAnnotationValidator }   (annotation path)
    //   - ValidUuid3ServiceLoaderValidator                 (service-loader path)
    //   - ValidUuid3XmlValidator                           (XML path, constraints-uuid-precedence.xml)
    //
    // The XML uses include-existing-validators="false"  →  XML WINS.
    // Only ValidUuid3XmlValidator is invoked; the other two are discarded.
    // =======================================================================

    /**
     * A well-formed UUID produces no violations even with all three validators
     * registered — the runtime resolves a single winner (the XML one).
     */
    @Test
    public void testPrecedence_ValidUuidProducesNoViolations() {
        Set<ConstraintViolation<Order3>> violations =
            validator.validate(new Order3("550e8400-e29b-41d4-a716-446655440000"));

        assertTrue("A well-formed UUID must produce no violations (all-three-present)",
                   violations.isEmpty());
    }

    /**
     * XML wins — with {@code include-existing-validators="false"}, only the
     * XML-declared validator ({@link ValidUuid3XmlValidator}) is used.
     *
     * <p>The annotation-declared validator ({@link ValidUuidAnnotationValidator})
     * and the service-loader validator ({@link ValidUuid3ServiceLoaderValidator})
     * must NOT be invoked.
     */
    @Test
    public void testPrecedence_XmlValidatorWinsOverAnnotationAndServiceLoader() {
        ValidUuidAnnotationValidator.wasInvoked      = false;
        ValidUuid3ServiceLoaderValidator.wasInvoked  = false;
        ValidUuid3XmlValidator.wasInvoked            = false;

        validator.validate(new Order3("550e8400-e29b-41d4-a716-446655440000"));

        assertTrue("XML-registered validator must be invoked (XML wins)",
                   ValidUuid3XmlValidator.wasInvoked);
        assertFalse("Annotation-declared validator must NOT be invoked when XML overrides with include-existing-validators=false",
                    ValidUuidAnnotationValidator.wasInvoked);
        assertFalse("Service-loader validator must NOT be invoked when XML overrides with include-existing-validators=false",
                    ValidUuid3ServiceLoaderValidator.wasInvoked);
    }

    /**
     * A malformed UUID produces exactly one violation (all-three-present, XML wins path).
     */
    @Test
    public void testPrecedence_MalformedUuidProducesViolation() {
        Set<ConstraintViolation<Order3>> violations =
            validator.validate(new Order3("not-a-uuid"));

        assertEquals("A non-UUID string must produce exactly one violation (XML-wins precedence)",
                     1, violations.size());
    }

    /**
     * Violation message matches the default on {@link ValidUuid3}.
     */
    @Test
    public void testPrecedence_ViolationMessageMatchesConstraintDefault() {
        Set<ConstraintViolation<Order3>> violations =
            validator.validate(new Order3("bad"));

        assertFalse("Expected at least one violation", violations.isEmpty());
        assertEquals("must be a valid UUID (precedence test)",
                     violations.iterator().next().getMessage());
    }

    /**
     * Violation property path points to the {@code paymentRef} field of {@link Order3}.
     */
    @Test
    public void testPrecedence_ViolationPropertyPath() {
        Set<ConstraintViolation<Order3>> violations =
            validator.validate(new Order3("bad"));

        assertFalse("Expected at least one violation", violations.isEmpty());
        assertEquals("paymentRef",
                     violations.iterator().next().getPropertyPath().toString());
    }
}
