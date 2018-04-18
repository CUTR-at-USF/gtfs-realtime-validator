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
package edu.usf.cutr.gtfsrtvalidator.test.queries;

import edu.usf.cutr.gtfsrtvalidator.api.resource.GtfsFeedTest;
import edu.usf.cutr.gtfsrtvalidator.api.resource.GtfsRtFeed;
import edu.usf.cutr.gtfsrtvalidator.lib.model.ViewErrorLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.ViewErrorSummaryModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.MergeMonitorData;
import junit.framework.TestCase;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/*
 * Tests the queries written for Summary and Log tables
 */
public class QueryTest extends TestCase {

    private final String projectDir = System.getProperty("user.dir");
    private final String SQL_SCRIPT = projectDir + "/src/test/resources/testSQLScript.sql";

    // GtfsRtId value should be same as what we insert into database in 'testSQLScript.sql' file
    private int gtfsRtId = -1;
    // This will set 'setFirstResult' parameter value to 0
    private int currentPage = 1;
    // Specifies maximum number of records to fetch from database
    private int rowsPerPage;

    private Statement stmt;
    private GtfsRtFeed gtfsRtFeed;
    private GtfsFeedTest gtfsFeedTest;
    MergeMonitorData mergeMonitorData;

    @Override
    protected void setUp() {

        gtfsFeedTest = new GtfsFeedTest();
        gtfsRtFeed = new GtfsRtFeed();
        /*
         * Timestamp value is given 0 because the Timestamp values inserted into database using 'testSQLScript.sql'
         * file are 1 and 2. So, value 0 ensures to retrieve records from database whose Timestamp values are > 0
         */
        gtfsRtFeed.currentTimestamp = 0;
        gtfsFeedTest.setUp();

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(SQL_SCRIPT));
            String createQuery = new String(encoded, "UTF-8");

            String[] createStatements = createQuery.split(";");

            for (String createStatement : createStatements) {
                Class.forName("org.hsqldb.jdbcDriver");
                Connection con = DriverManager.getConnection("jdbc:hsqldb:file:gtfsrthsql", "sa", "");
                stmt = con.createStatement();
                stmt.execute(createStatement);
                stmt.close();
                con.close();
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    @Test
    public void testViewErrorSummaryModel() {

        List<ViewErrorSummaryModel> staticResult = new ArrayList<>();
        ViewErrorSummaryModel viewErrorSummaryModel = new ViewErrorSummaryModel();

        /*
         * Form the 'staticResult' that needs to be compared with the query results
         * We get query results in descending order of iterationIds and ascending order of error Ids
         * ViewErrorSummaryModel values are inserted into 'staticResult' in the same order as we get the query results
         * See 'testSQLScript.sql' file for database records
         * Order by errorId ASC will produce results in the order of ids 'E002', 'W001', 'W002', and are added in 'staticResult' in that order
         */
        viewErrorSummaryModel.setGtfsRtId(gtfsRtId);
        viewErrorSummaryModel.setLastTime(2);
        viewErrorSummaryModel.setLastFeedTime(2);
        viewErrorSummaryModel.setCount(2);
        viewErrorSummaryModel.setId("E002");
        viewErrorSummaryModel.setSeverity("ERROR");
        viewErrorSummaryModel.setTitle("Unsorted stop_sequence");
        viewErrorSummaryModel.setLastIterationId(-1);
        viewErrorSummaryModel.setLastRowId(2);
        viewErrorSummaryModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorSummaryModel.getLastFeedTime(), gtfsRtId));
        viewErrorSummaryModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorSummaryModel);

        viewErrorSummaryModel = new ViewErrorSummaryModel();
        viewErrorSummaryModel.setGtfsRtId(gtfsRtId);
        viewErrorSummaryModel.setLastTime(2);
        viewErrorSummaryModel.setLastFeedTime(2);
        viewErrorSummaryModel.setCount(2);
        viewErrorSummaryModel.setId("W001");
        viewErrorSummaryModel.setSeverity("WARNING");
        viewErrorSummaryModel.setTitle("Timestamp not populated");
        viewErrorSummaryModel.setLastIterationId(-1);
        viewErrorSummaryModel.setLastRowId(2);
        viewErrorSummaryModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorSummaryModel.getLastFeedTime(), gtfsRtId));
        viewErrorSummaryModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorSummaryModel);

        viewErrorSummaryModel = new ViewErrorSummaryModel();
        viewErrorSummaryModel.setGtfsRtId(gtfsRtId);
        viewErrorSummaryModel.setLastTime(2);
        viewErrorSummaryModel.setLastFeedTime(2);
        viewErrorSummaryModel.setCount(2);
        viewErrorSummaryModel.setId("W002");
        viewErrorSummaryModel.setSeverity("WARNING");
        viewErrorSummaryModel.setTitle("Vehicle_id not populated");
        viewErrorSummaryModel.setLastIterationId(-1);
        viewErrorSummaryModel.setLastRowId(2);
        viewErrorSummaryModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorSummaryModel.getLastFeedTime(), gtfsRtId));
        viewErrorSummaryModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorSummaryModel);

        // We need to retrieve maximum of 3 records from database
        rowsPerPage = 3;
        mergeMonitorData = (MergeMonitorData) gtfsRtFeed.getMonitorData(gtfsRtId, currentPage, rowsPerPage, "", currentPage, rowsPerPage, 0, 0).getEntity();

        assertEquals(staticResult, mergeMonitorData.getViewErrorSummaryModelList());
    }

    @Test
    public void testViewErrorLogModel() {

        List<ViewErrorLogModel> staticResult = new ArrayList<>();
        ViewErrorLogModel viewErrorLogModel = new ViewErrorLogModel();

        /*
         * Form the 'staticResult' that needs to be compared with the query results/response.
         * We get query results in descending order of iterationIds and ascending order of error Ids.
         * ViewErrorSummaryModel values are inserted into 'staticResult' in the same order as we get the query results.
         * See 'testSQLScript.sql' file for database records.
         * Order by iterationIds DESC, errorId ASC will produce results in the order of ids 'E002', 'W001', 'W002'
         *  with decreasing order of iterationIds, and are added in 'staticResult' in that same order.
         */
        viewErrorLogModel.setRowId(2);
        viewErrorLogModel.setGtfsRtId(gtfsRtId);
        viewErrorLogModel.setIterationId(-1);
        viewErrorLogModel.setOccurrence(2);
        viewErrorLogModel.setLoggingTime(2);
        viewErrorLogModel.setId("E002");
        viewErrorLogModel.setSeverity("ERROR");
        viewErrorLogModel.setTitle("Unsorted stop_sequence");
        viewErrorLogModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorLogModel.getOccurrence(), gtfsRtId));
        viewErrorLogModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorLogModel);

        viewErrorLogModel = new ViewErrorLogModel();
        viewErrorLogModel.setRowId(2);
        viewErrorLogModel.setGtfsRtId(gtfsRtId);
        viewErrorLogModel.setIterationId(-1);
        viewErrorLogModel.setOccurrence(2);
        viewErrorLogModel.setLoggingTime(2);
        viewErrorLogModel.setId("W001");
        viewErrorLogModel.setSeverity("WARNING");
        viewErrorLogModel.setTitle("Timestamp not populated");
        viewErrorLogModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorLogModel.getOccurrence(), gtfsRtId));
        viewErrorLogModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorLogModel);

        viewErrorLogModel = new ViewErrorLogModel();
        viewErrorLogModel.setRowId(2);
        viewErrorLogModel.setGtfsRtId(gtfsRtId);
        viewErrorLogModel.setIterationId(-1);
        viewErrorLogModel.setOccurrence(2);
        viewErrorLogModel.setLoggingTime(2);
        viewErrorLogModel.setId("W002");
        viewErrorLogModel.setSeverity("WARNING");
        viewErrorLogModel.setTitle("Vehicle_id not populated");
        viewErrorLogModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorLogModel.getOccurrence(), gtfsRtId));
        viewErrorLogModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorLogModel);

        viewErrorLogModel = new ViewErrorLogModel();
        viewErrorLogModel.setRowId(1);
        viewErrorLogModel.setGtfsRtId(gtfsRtId);
        viewErrorLogModel.setIterationId(-2);
        viewErrorLogModel.setOccurrence(1);
        viewErrorLogModel.setLoggingTime(1);
        viewErrorLogModel.setId("E002");
        viewErrorLogModel.setSeverity("ERROR");
        viewErrorLogModel.setTitle("Unsorted stop_sequence");
        viewErrorLogModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorLogModel.getOccurrence(), gtfsRtId));
        viewErrorLogModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorLogModel);

        viewErrorLogModel = new ViewErrorLogModel();
        viewErrorLogModel.setRowId(1);
        viewErrorLogModel.setGtfsRtId(gtfsRtId);
        viewErrorLogModel.setIterationId(-2);
        viewErrorLogModel.setOccurrence(1);
        viewErrorLogModel.setLoggingTime(1);
        viewErrorLogModel.setId("W001");
        viewErrorLogModel.setSeverity("WARNING");
        viewErrorLogModel.setTitle("Timestamp not populated");
        viewErrorLogModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorLogModel.getOccurrence(), gtfsRtId));
        viewErrorLogModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorLogModel);

        viewErrorLogModel = new ViewErrorLogModel();
        viewErrorLogModel.setRowId(1);
        viewErrorLogModel.setGtfsRtId(gtfsRtId);
        viewErrorLogModel.setIterationId(-2);
        viewErrorLogModel.setOccurrence(1);
        viewErrorLogModel.setLoggingTime(1);
        viewErrorLogModel.setId("W002");
        viewErrorLogModel.setSeverity("WARNING");
        viewErrorLogModel.setTitle("Vehicle_id not populated");
        viewErrorLogModel.setFormattedTimestamp(gtfsRtFeed.getDateFormat(viewErrorLogModel.getOccurrence(), gtfsRtId));
        viewErrorLogModel.setTimeZone(GtfsRtFeed.agencyTimezone);

        staticResult.add(viewErrorLogModel);

        // We need to retrieve maximum of 3 records from database
        rowsPerPage = 6;
        mergeMonitorData = (MergeMonitorData) gtfsRtFeed.getMonitorData(gtfsRtId, currentPage, rowsPerPage, "", currentPage, rowsPerPage, 0, 0)
                .getEntity();

        assertEquals(staticResult, mergeMonitorData.getViewErrorLogModelList());
    }
}
