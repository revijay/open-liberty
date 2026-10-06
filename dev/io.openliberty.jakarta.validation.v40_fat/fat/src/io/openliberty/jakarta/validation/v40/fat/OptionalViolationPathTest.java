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
import val40opt.web.OptionalViolationPathTestServlet;

/**
 * FAT test class for the Jakarta Validation 4.0 section 4.3 change:
 * removal of the {@code null}-node requirement from the built-in
 * {@code Optional} value extractor.
 *
 * <h2>What this tests</h2>
 * <p>Before 4.0, a constraint violation on a value <em>inside</em> an
 * {@code Optional} field was reported at the field path (e.g. {@code "nickname"})
 * because the spec required the extractor to pass {@code null} as the node name,
 * suppressing the container node.
 *
 * <p>After 4.0, that requirement is removed. {@code Optional} is treated like
 * any other container ({@code List}, {@code Map}…), so the violation path
 * includes the container node: {@code "nickname.<optional value>"}.
 *
 * <p>The {@code val40opt} application contains two sets of tests:
 * <ul>
 *   <li><b>Section 1 ({@code testPre40_*})</b> — assert the pre-4.0 path
 *       (field name only). These pass against {@code validation-3.1}.</li>
 *   <li><b>Section 2 ({@code testPost40_*})</b> — assert the post-4.0 path
 *       ({@code field.<optional value>}). These will FAIL against
 *       {@code validation-3.1} and will PASS once the 4.0 runtime is active.</li>
 *   <li><b>Section 3 ({@code test*})</b> — edge cases that behave identically
 *       in both spec versions.</li>
 * </ul>
 *
 * <p>Run in isolation:
 * <pre>
 *   ./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun \
 *       -Dfat.test.class=io.openliberty.jakarta.validation.v40.fat.OptionalViolationPathTest
 * </pre>
 */
@RunWith(FATRunner.class)
public class OptionalViolationPathTest extends FATServletClient {

    public static final String APP_NAME = "val40opt";

    @Server("validation.v40.fat")
    @TestServlet(servlet = OptionalViolationPathTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "val40opt.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
