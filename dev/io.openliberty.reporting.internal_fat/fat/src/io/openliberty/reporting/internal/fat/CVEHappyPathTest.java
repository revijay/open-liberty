/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Verifies that the CVE reporting feature can successfully send the collected
 * server data to the external UMS endpoint.
 */
@RunWith(FATRunner.class)
public class CVEHappyPathTest extends FATServletClient {

    public static final String SERVER_NAME = "io.openliberty.reporting.server";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            // Allow CWWKF1705W on teardown in case the external cluster is unreachable
            server.stopServer("CWWKF1705W");
        }
        server.restoreServerConfiguration();
    }

    /**
     * Verifies that the CVE reporting feature:
     * <ol>
     *   <li>Is enabled and fires the reporting task (CWWKF1700I logged)</li>
     *   <li>Successfully sends the data to the UMS endpoint with no
     *       communication error (CWWKF1705W must NOT be logged)</li>
     * </ol>
     */
    @Test
    public void testDataSentToUMSEndpoint() throws Exception {
        server.startServer();

        // 1. Feature is enabled and the reporting task ran
        assertNotNull("CVE Reporting feature did not run — CWWKF1700I not found in log",
                      server.waitForStringInLog("CWWKF1700I"));

        // 2. Data was sent successfully — no communication failure warning
        // CWWKF1705W is only emitted when the POST fails with an IOException
        // (e.g. connection refused, SSL error, unreachable host)
        assertNull("Data was NOT sent to the UMS endpoint — CWWKF1705W (communication failure) was logged",
                   server.waitForStringInLog("CWWKF1705W", 30000));
    }

    /**
     * Verifies that the Liberty runtime exposes its edition today. The edition is
     * read from the product properties file in lib/versions of the installation,
     * which is the same source ProductInfo.getEdition() uses.
     */
    @Test
    public void testLibertyEditionIsExposed() throws Exception {
        File versionsDir = new File(server.getInstallRoot(), "lib/versions");
        File[] propertyFiles = versionsDir.listFiles((dir, name) -> name.endsWith(".properties"));
        assertNotNull("No lib/versions directory found at " + versionsDir, propertyFiles);

        String edition = null;
        for (File file : propertyFiles) {
            Properties props = new Properties();
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            }
            String productId = props.getProperty("com.ibm.websphere.productId");
            if ("io.openliberty".equals(productId) || "com.ibm.websphere.appserver".equals(productId)) {
                edition = props.getProperty("com.ibm.websphere.productEdition");
                break;
            }
        }

        Log.info(CVEHappyPathTest.class, "testLibertyEditionIsExposed", "Liberty edition read from lib/versions: " + edition);

        assertNotNull("com.ibm.websphere.productEdition not found for the Liberty product in " + versionsDir, edition);
        assertTrue("Liberty edition is empty", !edition.trim().isEmpty());

        List<String> knownEditions = Arrays.asList("Core", "CORE", "BASE", "DEVELOPERS", "EXPRESS", "BLUEMIX",
                                                   "EARLY_ACCESS", "zOS", "ND", "BASE_ILAN", "Open");
        assertTrue("Unexpected Liberty edition: " + edition, knownEditions.contains(edition));
    }

    /**
     * Verifies that the productName sent to UMS is read from the installed
     * product (lib/versions) and is no longer hardcoded as "Liberty".
     */
    @Test
    public void testProductNameIsDynamic() throws Exception {
        File versionsDir = new File(server.getInstallRoot(), "lib/versions");
        File[] propertyFiles = versionsDir.listFiles((dir, name) -> name.endsWith(".properties"));
        assertNotNull("No lib/versions directory found at " + versionsDir, propertyFiles);

        String expectedName = null;
        for (File file : propertyFiles) {
            Properties props = new Properties();
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            }
            String productId = props.getProperty("com.ibm.websphere.productId");
            if ("io.openliberty".equals(productId) || "com.ibm.websphere.appserver".equals(productId)) {
                expectedName = props.getProperty("com.ibm.websphere.productName");
                break;
            }
        }
        assertNotNull("com.ibm.websphere.productName not found for the Liberty product in " + versionsDir, expectedName);

        server.startServer();
        assertNotNull("CVE Reporting feature did not run — CWWKF1700I not found in log",
                      server.waitForStringInLog("CWWKF1700I"));

        // The data map is traced by DataCollector.getData() on exit
        String line = server.waitForStringInTrace("productName=" + expectedName, 30000);
        Log.info(CVEHappyPathTest.class, "testProductNameIsDynamic", "Expected productName: " + expectedName + ", trace line: " + line);
        assertNotNull("productName in the reported data was not '" + expectedName + "'", line);
    }
}
