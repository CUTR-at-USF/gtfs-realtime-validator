/*
 * Copyright (C) 2017 University of South Florida.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.usf.cutr.gtfsrtvalidator.api.resource;

import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.hibernate.HibernateUtil;
import junit.framework.TestCase;

import javax.ws.rs.core.Response;

/*
 * Tests loading GTFS data.
 */
public class GtfsFeedTest extends TestCase {

    private final String validGtfsFeedURL = "https://github.com/CUTR-at-USF/gtfs-realtime-validator/raw/master/src/test/resources/bullrunner-gtfs.zip";
    private final String invalidGtfsFeedURL = "DUMMY";
    private final String downloadFailURL = "http://gohart.org/google/file_not_exist.zip";
    private final String badGTFS = "https://github.com/CUTR-at-USF/gtfs-realtime-validator/raw/master/src/test/resources/badgtfs.zip";

    GtfsFeed gtfsFeed;

    public void setUp() {
        gtfsFeed  = new GtfsFeed();
        HibernateUtil.configureSessionFactory();
        GTFSDB.initializeDB();
    }

    public void testGtfsFeed() {
        Response response;

        response = gtfsFeed.postGtfsFeed(validGtfsFeedURL);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        response = gtfsFeed.postGtfsFeed(invalidGtfsFeedURL);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        response = gtfsFeed.postGtfsFeed(downloadFailURL);
        assertTrue(Response.Status.BAD_REQUEST.getStatusCode() == response.getStatus() || Response.Status.FORBIDDEN.getStatusCode() == response.getStatus());

        response = gtfsFeed.postGtfsFeed(badGTFS);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}