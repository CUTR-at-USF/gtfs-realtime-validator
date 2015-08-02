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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Date;

public class GTFSDB {

    public static void InitializeDB() {
        Statement stmt = null;
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

    //region Gtfs-Feed related database CURD functions
    //TODO: Add get gtfs feed from id method
    public static synchronized void createGtfsFeed(GtfsFeedModel gtfsFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsFeed (feedUrl, fileLocation, downloadTimestamp)VALUES (?,?,?)");
            stmt.setString(1, gtfsFeed.getGtfsUrl());
            stmt.setString(2, gtfsFeed.getFeedLocation());
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
    //endregion

    //region Gtfs-Realtime-Feed related database CURD functions
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
    //TODO: Combine get-rt-feed methods
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
    //TODO: Make removal use an ID
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
    //TODO: Make insert accept a GtfsFeedModel
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
    //endregion

}
