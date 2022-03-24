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
import edu.usf.cutr.gtfsrtvalidator.lib.model.GtfsFeedModel;
import edu.usf.cutr.gtfsrtvalidator.util.FileUtil;
import junit.framework.TestCase;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;

/*
 * Tests loading GTFS data.
 */
public class GtfsFeedTest extends TestCase {

    private final String validGtfsFeedURL = "https://github.com/MobilityData/gtfs-realtime-validator/raw/master/gtfs-realtime-validator-webapp/src/test/resources/bullrunner-gtfs.zip";
    private final String invalidGtfsFeedURL = "DUMMY";
    private final String downloadFailURL = "http://gohart.org/google/file_not_exist.zip";
    private final String badGTFS = "https://github.com/MobilityData/gtfs-realtime-validator/raw/master/gtfs-realtime-validator-webapp/src/test/resources/badgtfs.zip";

    private GtfsFeed mGtfsFeed;

    public void setUp() {
        mGtfsFeed = new GtfsFeed();
        HibernateUtil.configureSessionFactory();
        GTFSDB.initializeDB();
    }

    public void testGtfsFeed() {
        String gtfsFileName = FileUtil.getGtfsFileName(validGtfsFeedURL);
        File validationFile = FileUtil.getGtfsValidationOutputFile(this, gtfsFileName);

        // Delete any existing validation file for the valid feed
        validationFile.delete();
        assertFalse(validationFile.exists());

        // Valid feed URL with real GTFS data
        Response response = mGtfsFeed.postGtfsFeed(validGtfsFeedURL, "checked");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        GtfsFeedModel model = (GtfsFeedModel) response.getEntity();
        assertEquals(model.getGtfsUrl(), validGtfsFeedURL);

        // We asked for the feed to be validated ("checked" parameter), so make sure the validation file exists
        assertTrue(validationFile.exists());
        long validationFileLastModified = validationFile.lastModified();

        // Make sure we can get the GTFS feed that we just added
        boolean foundFeed = false;
        int feedId = model.getFeedId();
        response = mGtfsFeed.getGtfsFeeds();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<GtfsFeedModel> feedList = (List<GtfsFeedModel>) response.getEntity();
        for (GtfsFeedModel m : feedList) {
            if (m.getGtfsUrl().equals(validGtfsFeedURL)) {
                foundFeed = true;
            }
        }
        assertTrue(foundFeed);

        // Submit the same URL again to make sure the validation file doesn't change (if the GTFS didn't change, it shouldn't validate again)
        response = mGtfsFeed.postGtfsFeed(validGtfsFeedURL, "checked");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(validationFileLastModified, validationFile.lastModified());

        // Delete the feed we just added
        response = mGtfsFeed.deleteGtfsFeed(String.valueOf(feedId));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());

        // Verify that it doesn't exist anymore
        foundFeed = false;
        response = mGtfsFeed.getGtfsFeeds();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        feedList = (List<GtfsFeedModel>) response.getEntity();
        for (GtfsFeedModel m : feedList) {
            if (m.getGtfsUrl().equals(validGtfsFeedURL)) {
                foundFeed = true;
            }
        }
        assertFalse(foundFeed);

        // Delete the validation file again
        validationFile.delete();
        assertFalse(validationFile.exists());

        // Add the feed again, but this time don't request validation - the validation file then shouldn't exist
        response = mGtfsFeed.postGtfsFeed(validGtfsFeedURL, "unchecked");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertFalse(validationFile.exists());

        // Invalid feed URL
        response = mGtfsFeed.postGtfsFeed(invalidGtfsFeedURL, "checked");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        // Valid URL, but missing file
        response = mGtfsFeed.postGtfsFeed(downloadFailURL, "checked");
        assertTrue(Response.Status.BAD_REQUEST.getStatusCode() == response.getStatus() || Response.Status.FORBIDDEN.getStatusCode() == response.getStatus());

        // Valid URL, but bad GTFS data
        response = mGtfsFeed.postGtfsFeed(badGTFS, "checked");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}