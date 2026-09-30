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
package io.openliberty.jakarta.validation.v40.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import val40tsr.web.TypeValidatorSelectionTestServlet;

/**
 * FAT test class for Jakarta Validation 4.0 spec correction BVAL-698.
 *
 * <p>Verifies that applying a constraint to a {@code String} field when
 * validators are registered for both {@code CharSequence} and
 * {@code Serializable} does <em>not</em> throw
 * {@code UnexpectedTypeException}. The spec's type-validator resolution
 * algorithm correctly selects the most specific applicable type
 * ({@code CharSequence}) and the {@code Serializable} validator is
 * never invoked.
 *
 * <p>Run in isolation:
 * <pre>
 *   ./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun \
 *       -Dfat.test.class=io.openliberty.jakarta.validation.v40.fat.TypeValidatorSelectionTest
 * </pre>
 */
@RunWith(FATRunner.class)
public class TypeValidatorSelectionTest extends FATServletClient {

    public static final String APP_NAME = "val40tsr";

    @Server("validation.v40.fat")
    @TestServlet(servlet = TypeValidatorSelectionTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "val40tsr.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
