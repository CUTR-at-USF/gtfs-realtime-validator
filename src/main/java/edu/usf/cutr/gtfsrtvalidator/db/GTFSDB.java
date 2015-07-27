/*
 * Copyright (C) 2015 Nipuna Gunathilake.
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

package edu.usf.cutr.gtfsrtvalidator.db;

import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsRtFeedModel;
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;
import edu.usf.cutr.gtfsrtvalidator.json.MonitorDetails;
import edu.usf.cutr.gtfsrtvalidator.json.MonitorLog;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GTFSDB {

    static Statement stmt = null;

    public static void InitializeDB() {

        String workingDir = System.getProperty("user.dir");
        String createTablePath = workingDir + "/target/classes/createTables.sql";

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(createTablePath));
            String createTableQuerry = new String(encoded, "UTF-8");

            String[] createStatements = createTableQuerry.split(";");

            for (String createStatement : createStatements) {
                Class.forName("org.sqlite.JDBC");
                Connection con = DriverManager.getConnection("jdbc:sqlite:gtfsrt.db");

                stmt = con.createStatement();
                String sql = createStatement;
                stmt.executeUpdate(sql);
                stmt.close();
                con.close();
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        System.out.println("Table created successfully");
    }

    public static synchronized void setFeedDetails(String url, int vehicle, int trip, int alert) {
        try {
            PreparedStatement stmt;

            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            Date currentDate = new Date();
            long unixTIme = currentDate.getTime() / 1000;

            stmt = con.prepareStatement("INSERT INTO FEED_DETAILS (Time_Stamp, Trip_Count, Alert_Count, Vehicle_Count, Feed_Url) VALUES ( ? , ? , ? , ?, ?);");
            stmt.setLong(1, unixTIme);
            stmt.setInt(2, vehicle);
            stmt.setInt(3, alert);
            stmt.setInt(4, trip);
            stmt.setString(5, url);

            stmt.executeUpdate();

            stmt.close();
            con.commit();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public static GtfsRtFeedModel getFeedFromID(int id) {
        try {
            PreparedStatement stmt;
            GtfsRtFeedModel gtfsFeed = new GtfsRtFeedModel();

            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            String sql = "SELECT * FROM GtfsRtFeed WHERE rtFeedID=?;";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            //If record alerady exists, return that item

            if (rs.next()) {
                System.out.println("the record exists");
                gtfsFeed.setGtfsUrl(rs.getString("feedURL"));
                gtfsFeed.setGtfsId(rs.getInt("gtfsFeedID"));
                gtfsFeed.setStartTime(rs.getLong("startTime"));
            } else {
                return null;
            }

            //rtFeedInDB = rs.isBeforeFirst();
            stmt.close();
            con.commit();
            con.close();
            return  gtfsFeed;

        } catch (Exception ex) {
            return null;
        }
    }

    public static synchronized MonitorLog getFeedDetails(String feedUrl) {
        return getFeedDetails(feedUrl, 11);
    }

    public static synchronized MonitorLog getFeedDetails(String feedUrl, int limit) {

        MonitorLog monitorLog = new MonitorLog();
        List<MonitorDetails> monitorDetails = new ArrayList<>();

        try {
            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            PreparedStatement stmt;

            String sql = "SELECT * FROM FEED_DETAILS WHERE Feed_Url=? ORDER BY Time_Stamp DESC LIMIT ?;";
            stmt = con.prepareStatement(sql);

            stmt.setString(1, feedUrl);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                MonitorDetails monitor = new MonitorDetails();

                monitor.setVehicleCount(rs.getInt("Vehicle_Count"));
                monitor.setUpdateCount(rs.getInt("Trip_Count"));
                monitor.setAlertCount(rs.getInt("Alert_Count"));

                monitor.setTimestamp(rs.getLong("Time_Stamp"));

                monitorDetails.add(monitor);
            }

            monitorLog.setMonitorDetails(monitorDetails);

            stmt.close();
            con.commit();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        return monitorLog;
    }

    public static void setGtfsFeed(String url, String fileLocation) {
        try {
            PreparedStatement stmt;

            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsFeed (feedUrl, fileLocation, downloadTimestamp)VALUES (?,?,?)");
            stmt.setString(1, url);
            stmt.setString(2, fileLocation);
            stmt.setLong(3, TimeStampHelper.getCurrentTimestamp());

            stmt.executeUpdate();

            stmt.close();
            con.commit();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public static void setGtfsRtFeed(String url, int gtfsFeedID) {
        try {
            PreparedStatement stmt;

            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            Date currentDate = new Date();
            long unixTIme = currentDate.getTime() / 1000;

            stmt = con.prepareStatement("INSERT INTO GtfsRtFeed (feedURL, gtfsFeedID, startTime) VALUES (?,?,?)");
            stmt.setString(1, url);
            stmt.setInt(2, gtfsFeedID);
            stmt.setLong(3, TimeStampHelper.getCurrentTimestamp());

            stmt.executeUpdate();

            stmt.close();
            con.commit();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
}
