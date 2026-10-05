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
import val40sl.web.ServiceLoaderValidatorTestServlet;

/**
 * FAT test class for Jakarta Validation 4.0 issue #257 —
 * {@code ConstraintValidator} declaration via {@link java.util.ServiceLoader}.
 *
 * <p>The {@code val40sl} web application contains:
 * <ul>
 *   <li>{@code @ValidUuid} — a constraint with {@code validatedBy = {}} (empty)</li>
 *   <li>{@code ValidUuidValidator} — registered only via
 *       {@code META-INF/services/jakarta.validation.ConstraintValidator}</li>
 *   <li>{@code Order} — a bean with a {@code @ValidUuid String orderId} field</li>
 * </ul>
 *
 * <p>A runtime that implements issue #257 discovers {@code ValidUuidValidator}
 * via {@code ServiceLoader} and validation succeeds. A runtime that does not
 * implement issue #257 throws {@link jakarta.validation.UnexpectedTypeException}
 * because no validator is bound to the {@code String} type.
 *
 * <p>Run in isolation:
 * <pre>
 *   ./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun \
 *       -Dfat.test.class=io.openliberty.jakarta.validation.v40.fat.ServiceLoaderValidatorTest
 * </pre>
 */
@RunWith(FATRunner.class)
public class ServiceLoaderValidatorTest extends FATServletClient {

    public static final String APP_NAME = "val40sl";

    @Server("validation.v40.fat")
    @TestServlet(servlet = ServiceLoaderValidatorTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "val40sl.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
