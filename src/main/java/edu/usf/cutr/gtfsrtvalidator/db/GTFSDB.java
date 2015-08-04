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

import edu.usf.cutr.gtfsrtvalidator.api.model.*;
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GTFSDB {

    public static void InitializeDB() {
        Statement stmt;
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
                stmt.executeUpdate(createStatement);
                stmt.close();
                con.close();
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        System.out.println("Table created successfully");
    }

    //region CURD: Gtfs-Feed
    //Create
    public static synchronized int createGtfsFeed(GtfsFeedModel gtfsFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        int createdId = 0;

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsFeed (feedUrl, fileLocation, downloadTimestamp)VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, gtfsFeed.getGtfsUrl());
            stmt.setString(2, gtfsFeed.getFeedLocation());
            stmt.setLong(3, TimeStampHelper.getCurrentTimestamp());

            stmt.executeUpdate();

            //Get the id of the created feed
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                createdId = rs.getInt(1);
            }

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

        return createdId;
    }

    //Update
    public static synchronized void updateGtfsFeed(GtfsFeedModel gtfsFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("UPDATE GtfsFeed SET feedUrl = ?, fileLocation = ?, downloadTimestamp = ? " +
                    "WHERE feedID = ?");
            stmt.setString(1, gtfsFeed.getGtfsUrl());
            stmt.setString(2, gtfsFeed.getFeedLocation());
            stmt.setLong(3, gtfsFeed.getStartTime());
            stmt.setInt(4, gtfsFeed.getFeedId());

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

    //Read
    public static synchronized GtfsFeedModel readGtfsFeed(GtfsFeedModel searchFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            GtfsFeedModel gtfsFeed = new GtfsFeedModel();

            con.setAutoCommit(false);

            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM GtfsFeed");

            Map<String, String> params = new HashMap<>();
            if (searchFeed.getGtfsUrl() != null && searchFeed.getGtfsUrl().length() != 0) {
                params.put(GtfsFeedModel.FEEDURL, searchFeed.getGtfsUrl());
            }
            if (searchFeed.getFeedLocation() != null && searchFeed.getFeedLocation().length() != 0) {
                params.put(GtfsFeedModel.FILELOCATION, searchFeed.getFeedLocation());
            }

            Set<String> colSet = params.keySet();
            if (!colSet.isEmpty()) {
                StringBuilder whereClause = new StringBuilder(" WHERE");
                String andOp = "";
                for (String colName : colSet) {
                    whereClause.append(andOp);
                    whereClause.append(" ");
                    whereClause.append(colName);
                    whereClause.append("=? ");
                    andOp = " AND ";
                }
                sqlBuilder.append(whereClause);
            }

            stmt = con.prepareStatement(sqlBuilder.toString());

            int paramPos = 1;
            for (String colName : colSet) {
                if (colName.equals(GtfsFeedModel.FEEDURL)) {
                    stmt.setString(paramPos, params.get(colName));
                }

                if (colName.equals(GtfsFeedModel.FILELOCATION)) {
                    stmt.setString(paramPos, params.get(colName));
                }

                paramPos++;
            }

            ResultSet rs = stmt.executeQuery();

            //If record exists, return that item
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
    public static synchronized GtfsFeedModel readGtfsFeed(int feedId) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            GtfsFeedModel gtfsFeed = new GtfsFeedModel();

            con.setAutoCommit(false);

            String sql = "SELECT * FROM GtfsFeed WHERE feedID = ?;";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, feedId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                gtfsFeed.setGtfsUrl(rs.getString("feedURL"));
                gtfsFeed.setFeedId(rs.getInt("feedID"));
                gtfsFeed.setStartTime(rs.getLong("downloadTimestamp"));
                gtfsFeed.setFeedLocation(rs.getString("fileLocation"));
            } else {
                return null;
            }

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

    //Delete
    public static synchronized void deleteGtfsFeed(int feedId) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM GtfsFeed WHERE feedID = ?");
            stmt.setInt(1, feedId);

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

    //region CURD: Gtfs-Realtime-Feed
    //Create
    public static synchronized int createGtfsRtFeed(GtfsRtFeedModel rtFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        int createdId = 0;
        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsRtFeed (feedURL, gtfsFeedID, startTime) VALUES (?,?,?)");
            stmt.setString(1, rtFeed.getGtfsUrl());
            stmt.setInt(2, rtFeed.getGtfsId());
            stmt.setLong(3, TimeStampHelper.getCurrentTimestamp());

            stmt.executeUpdate();

            //Get the id of the created feed
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                createdId = rs.getInt(1);
            }

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
        return createdId;
    }

    //Update
    public static synchronized void updateGtfsFeed(GtfsRtFeedModel gtfsFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("UPDATE GtfsRtFeed SET feedUrl = ?, gtfsFeedID = ?, startTime = ?" +
                    "WHERE rtFeedID = ?");
            stmt.setString(1, gtfsFeed.getGtfsUrl());
            stmt.setInt(2, gtfsFeed.getGtfsId());
            stmt.setLong(3, gtfsFeed.getStartTime());
            stmt.setInt(4, gtfsFeed.getGtfsRtId());

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

    //Read
    public static synchronized GtfsRtFeedModel getGtfsRtFeed(int gtfsFeedID) {

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
    public static GtfsRtFeedModel getGtfsRtFeed(GtfsRtFeedModel gtfsRtFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        PreparedStatement stmt;
        try {
            con.setAutoCommit(false);

            String sql = "SELECT * FROM GtfsRtFeed WHERE feedURL=?;";
            stmt = con.prepareStatement(sql);

            //stmt.setInt(1, gtfsRtFeed.getGtfsId());
            stmt.setString(1, gtfsRtFeed.getGtfsUrl());

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

    //Delete
    public static synchronized void deleteGtfsRtFeed(int RtfeedId) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM GtfsRtFeed WHERE gtfsFeedID = ?");
            stmt.setInt(1, RtfeedId);

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

    //region CURD: Rt-feed-Info
    public static synchronized int createRtFeedInfo(GtfsFeedIterationModel feedIteration) {
        int createdId = 0;

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsRtFeedIteration (feedProtobuf, rtFeedID, IterationTimestamp)VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setBytes(1, feedIteration.getFeedprotobuf());
            stmt.setInt(2, feedIteration.getRtFeedId());
            stmt.setLong(3, feedIteration.getTimeStamp());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                createdId = rs.getInt(1);
            }

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
        return createdId;
    }
    //endregion

    //region CURD: Message Log
    //Create
    public static synchronized void createMessageLog(MessageLogModel messageLog) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO MessageLog (itterationID, errorID)VALUES (?, ?)");
            stmt.setInt(1, messageLog.getIterationId());
            stmt.setString(2, messageLog.getErrorId());

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

    //Update
    public static synchronized void updateMessageLog(MessageLogModel messageLog) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("UPDATE MessageLog SET itterationID = ?, errorID = ?" +
                    "WHERE messageID = ?");
            stmt.setInt(1, messageLog.getIterationId());
            stmt.setString(2, messageLog.getErrorId());
            stmt.setInt(3, messageLog.getMessageId());

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

    //Read
    public static synchronized MessageLogModel getMessageLog(int messageId) {

        MessageLogModel messageLogModel = new MessageLogModel();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM MessageLog WHERE messageID = ?");
            stmt.setInt(1, messageId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messageLogModel.setErrorId(rs.getString(MessageLogModel.ERROR_ID));
                messageLogModel.setIterationId(rs.getInt(MessageLogModel.ITERATION_ID));
                messageLogModel.setMessageId(rs.getInt(MessageLogModel.MESSAGE_ID));
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

        return messageLogModel;
    }

    //Delete
    public static synchronized void deleteMessageLog(int messageId) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM MessageLog WHERE messageID = ?");
            stmt.setInt(1, messageId);

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

    //region CURD: Occurrence
    public static synchronized void createOccurrence(OccurrenceModel occurrence) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO Occurrence (messageID, elementPath, elementValue)VALUES (?,?,?)");
            stmt.setInt(1, occurrence.getMessageId());
            stmt.setString(2, occurrence.getElementPath());
            stmt.setString(3, occurrence.getElementValue());

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

    //Update
    public static synchronized void updateOccurrence(OccurrenceModel occurrence) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("UPDATE Occurrence SET messageID = ?, elementPath = ?, elementValue = ?" +
                    "WHERE messageID = ?");
            stmt.setInt(1, occurrence.getMessageId());
            stmt.setString(2, occurrence.getElementPath());
            stmt.setString(3, occurrence.getElementValue());
            stmt.setInt(4, occurrence.getOccurrenceId());

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

    //Read
    public static synchronized OccurrenceModel getOccurrence(int occurrenceId) {

        OccurrenceModel occurrenceModel = new OccurrenceModel();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM Occurrence WHERE occurrenceID = ?");
            stmt.setInt(1, occurrenceId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                occurrenceModel.setMessageId(rs.getInt(OccurrenceModel.MESSAGE_ID));
                occurrenceModel.setElementPath(rs.getString(OccurrenceModel.ELEMENT_PATH));
                occurrenceModel.setElementValue(rs.getString(OccurrenceModel.ELEMENT_VALUE));
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

        return occurrenceModel;
    }

    //Delete
    public static synchronized void deleteOccurrence(int occurenceId) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM Occurrence WHERE occurrenceID = ?");
            stmt.setInt(1, occurenceId);

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
