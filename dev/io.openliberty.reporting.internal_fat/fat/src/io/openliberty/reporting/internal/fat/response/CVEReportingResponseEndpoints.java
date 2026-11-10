/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal.fat.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationPath("")
@Path("/endpoints")
public class CVEReportingResponseEndpoints extends Application {

    private static JsonData data;
    private static int lastResponseCode = -1;

    /**
     * Accepts the CVE data POST from the feature, records the received data and
     * returns HTTP 200 with a sample CVE response body.
     *
     * @param dataReceived
     * @return HTTP 200 response with CVE response JSON
     * @throws IOException
     */
    @POST
    @Path("/checkResponse")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postResponse(JsonData dataReceived) throws IOException {

        data = dataReceived;
        lastResponseCode = Response.Status.OK.getStatusCode();

        System.out.println("POST COMPLETED");

        CveProduct cveProduct = new CveProduct();
        CveProduct cveProductJava = new CveProduct();
        CveResponseModel cveModel = new CveResponseModel();
        List<Cves> cvesList = new ArrayList<>();
        List<CveProduct> cveProducts = new ArrayList<>();

        Cves cves = new Cves();
        Cves cvesNew = new Cves();
        cves.setId("CVE-2023-50312");
        cves.setUrl("https://www.ibm.com/support/pages/node/7125527");
        cvesNew.setId("CVE-2023-50313");
        cvesNew.setUrl("https://www.ibm.com/support/pages/node/7125528");

        cvesList.add(cves);
        cvesList.add(cvesNew);

        cveProduct.setCves(cvesList);

        cveProduct.setProductName("Open Liberty");
        cveProduct.setUrl("https://openliberty.io/docs/latest/security-vulnerabilities.html");

        cveProducts.add(cveProduct);

        Cves cvesJava = new Cves();
        Cves cvesNewJava = new Cves();

        cvesJava.setId("CVE-2023-50314");
        cvesJava.setUrl("https://www.ibm.com/support/pages/node/7125529");

        cvesNewJava.setId("CVE-2023-50315");
        cvesNewJava.setUrl("https://www.ibm.com/support/pages/node/7125529");

        cvesList.clear();
        cvesList.add(cvesJava);
        cvesList.add(cvesNewJava);

        cveProductJava.setCves(cvesList);

        cveProductJava.setProductName("IBM Semeru Java");
        cveProductJava.setUrl("https://www.ibm.com/support/pages/semeru-runtimes-security-vulnerabilites");

        cveProducts.add(cveProductJava);

        cveModel.setCveProducts(cveProducts);

        Jsonb JSONB = JsonbBuilder.create();

        return Response.ok(JSONB.toJson(cveModel), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns the HTTP status code that was sent back to the feature on the last
     * POST request. Returns -1 if no POST has been received yet.
     *
     * @return the last HTTP response code as plain text
     */
    @GET
    @Path("/getResponseCode")
    @Produces(MediaType.TEXT_PLAIN)
    public int getResponseCode() {
        return lastResponseCode;
    }

    @GET
    @Path("/getResponse")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonData get() {

        return data;
    }

}
