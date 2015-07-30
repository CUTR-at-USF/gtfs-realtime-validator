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

import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsFeedModel;
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
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

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
        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized GtfsRtFeedModel getFeedFromID(int id) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;
            GtfsRtFeedModel gtfsFeed = new GtfsRtFeedModel();

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

            rs.close();
            stmt.close();
            con.commit();
            con.close();
            return  gtfsFeed;

        } catch (Exception ex) {
            return null;
        } finally {
            try {
                con.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized MonitorLog getFeedDetails(String feedUrl) {
        return getFeedDetails(feedUrl, 11);
    }
    public static synchronized MonitorLog getFeedDetails(String feedUrl, int limit) {

        MonitorLog monitorLog = new MonitorLog();
        List<MonitorDetails> monitorDetails = new ArrayList<>();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
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

            rs.close();
            stmt.close();
            con.commit();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        } finally {
            try {
                con.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return monitorLog;
    }

    public static synchronized void setGtfsFeed(String url, String fileLocation) {

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;

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
        } finally {
            try {
                con.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized void setGtfsRtFeed(String url, int gtfsFeedID) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

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
        } finally {
            try {
                con.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized GtfsRtFeedModel getGtfsRtFeedById(int gtfsFeedID) {

        GtfsRtFeedModel feedModel = new GtfsRtFeedModel();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM GtfsRtFeed WHERE rtFeedID = ?");
            stmt.setInt(1, gtfsFeedID);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                feedModel.setGtfsRtId(rs.getInt("rtFeedID"));
                feedModel.setGtfsUrl(rs.getString("feedURL"));
                feedModel.setGtfsId(rs.getInt("gtfsFeedID"));
                feedModel.setStartTime(rs.getLong("startTime"));
            }

            rs.close();
            stmt.close();
            con.commit();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return feedModel;
    }

    public static synchronized GtfsFeedModel getGtfsFeedFromUrl(String fileURL) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            GtfsFeedModel gtfsFeed = new GtfsFeedModel();

            con.setAutoCommit(false);

            String sql = "SELECT * FROM GtfsFeed WHERE feedUrl=?;";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, fileURL);

            ResultSet rs = stmt.executeQuery();

            //If record alerady exists, return that item

            if (rs.next()) {
                gtfsFeed.setGtfsUrl(rs.getString("feedURL"));
                gtfsFeed.setFeedId(rs.getInt("feedID"));
                gtfsFeed.setStartTime(rs.getLong("downloadTimestamp"));
                gtfsFeed.setFeedLocation(rs.getString("fileLocation"));
            } else {
                return null;
            }

            //rtFeedInDB = rs.isBeforeFirst();

            rs.close();
            stmt.close();
            con.commit();
            con.close();
            return  gtfsFeed;

        } catch (Exception ex) {
            return null;
        } finally {
            try {
                con.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized void removeGtfsFeedFromUrl(String fileURL) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;
            GtfsFeedModel gtfsFeed = new GtfsFeedModel();

            con.setAutoCommit(false);

            String sql = "DELETE FROM GtfsFeed WHERE feedUrl=?;";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, fileURL);

            stmt.executeUpdate();

            //rtFeedInDB = rs.isBeforeFirst();
            stmt.close();
            con.commit();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                con.close();
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static synchronized void setRtFeedInfo() {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsRtFeedIteration (feedProtobuf)VALUES (?)");
            stmt.setString(1, "teststring");

            stmt.executeUpdate();

            stmt.close();
            con.commit();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        } finally {
            try {
                con.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static GtfsRtFeedModel getGtfsRtFeed(int gtfsId, String gtfsUrl) {

        boolean entryExists = false;

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        PreparedStatement stmt;
        try {
            con.setAutoCommit(false);

            String sql = "SELECT * FROM GtfsRtFeed WHERE gtfsFeedID=? AND feedURL=?;";
            stmt = con.prepareStatement(sql);

            stmt.setInt(1, gtfsId);
            stmt.setString(2, gtfsUrl);

            ResultSet rs = stmt.executeQuery();

            //If record alerady exists, return that item
            if (rs.isBeforeFirst()) {
                GtfsRtFeedModel gtfsFeed = new GtfsRtFeedModel();
                if (rs.next()) {
                    System.out.println("the record exists");
                    gtfsFeed.setGtfsUrl(rs.getString("feedURL"));
                    gtfsFeed.setGtfsId(rs.getInt("gtfsFeedID"));
                    gtfsFeed.setStartTime(rs.getLong("startTime"));
                    gtfsFeed.setGtfsRtId(rs.getInt("rtFeedID"));
                    return gtfsFeed;
                }
            }

            stmt.close();
            con.commit();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
