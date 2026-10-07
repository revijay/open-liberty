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
import val40arr.web.ArrayAnnotationTestServlet;

/**
 * FAT test class demonstrating array element annotation placement — a topic
 * being clarified in Jakarta Validation 4.0.
 *
 * <p>The {@code val40arr} application contains {@link val40arr.web.TaggedItem},
 * a bean with three {@code String[]} fields that show the three distinct ways
 * a constraint annotation can be placed on or near an array:
 *
 * <ol>
 *   <li><b>Form A</b> — {@code @NotNull} on the field (array reference):
 *       constrains whether the array object itself is null.</li>
 *   <li><b>Form B</b> — {@code @NotBlankElement} as a TYPE_USE annotation on
 *       the component type: constrains each individual array element.</li>
 *   <li><b>Form C</b> — both constraints together: reference AND elements.</li>
 * </ol>
 *
 * <p>Run in isolation:
 * <pre>
 *   ./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun \
 *       -Dfat.test.class=io.openliberty.jakarta.validation.v40.fat.ArrayAnnotationTest
 * </pre>
 */
@RunWith(FATRunner.class)
public class ArrayAnnotationTest extends FATServletClient {

    public static final String APP_NAME = "val40arr";

    @Server("validation.v40.fat")
    @TestServlet(servlet = ArrayAnnotationTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "val40arr.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
