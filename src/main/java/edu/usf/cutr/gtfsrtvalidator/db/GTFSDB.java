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
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

        //Use reflection to get the list of rules from the ValidataionRules class
        Field[] fields = ValidationRules.class.getDeclaredFields();

        List<ValidationRule> rulesInClass = new ArrayList<>();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                Class classType = field.getType();
                if (classType == ValidationRule.class) {
                    ValidationRule rule = new ValidationRule();
                    try {
                        Object value = field.get(rule);
                        rule = (ValidationRule)value;
                        System.out.println(rule.getErrorDescription());
                        rulesInClass.add(rule);
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        GTFSDB.deleteAllRules();

        try {
            for (ValidationRule rule : rulesInClass) {
                GTFSDB.createError(rule);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("Table initialized successfully");
    }

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
    public static synchronized GtfsRtFeedModel readGtfsRtFeed(int gtfsFeedID) {

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

    public static GtfsRtFeedModel readGtfsRtFeed(GtfsRtFeedModel gtfsRtFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        PreparedStatement stmt;
        try {
            con.setAutoCommit(false);

            String sql = "SELECT * FROM GtfsRtFeed WHERE feedURL=? AND gtfsFeedID = ?;";
            stmt = con.prepareStatement(sql);

            //stmt.setInt(1, gtfsRtFeed.getGtfsId());
            stmt.setString(1, gtfsRtFeed.getGtfsUrl());
            stmt.setInt(2, gtfsRtFeed.getGtfsId());

            ResultSet rs = stmt.executeQuery();

            //If record already exists, return that item
            if (rs.isBeforeFirst()) {
                GtfsRtFeedModel gtfsFeed = new GtfsRtFeedModel();
                if (rs.next()) {
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

    //region CURD: GtfsRt Iteration
    //Read
    public static synchronized GtfsRtFeedIterationModel getIteration(int iterationId) {

        GtfsRtFeedIterationModel gtfsIteration = new GtfsRtFeedIterationModel();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM GtfsRtFeedIteration WHERE IterationID = ?");
            stmt.setInt(1, iterationId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gtfsIteration.setRtFeedId(rs.getInt(GtfsRtFeedIterationModel.RTFEEDID));
                gtfsIteration.setTimeStamp(rs.getLong(GtfsRtFeedIterationModel.ITERATIONTIMESTAMP));
                gtfsIteration.setFeedprotobuf(rs.getBytes(GtfsRtFeedIterationModel.FEEDPROTOBUF));
                gtfsIteration.setIterationId(rs.getInt(GtfsRtFeedIterationModel.ITERATIONID));
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
        return gtfsIteration;
    }
    //endregion

    //region CURD: Rt-feed-Info
    public static synchronized int createRtFeedInfo(GtfsRtFeedIterationModel feedIteration) {
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
    public static synchronized int createMessageLog(MessageLogModel messageLog) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        int createdId = 0;

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO MessageLog (iterationID, errorID)VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, messageLog.getIterationId());
            stmt.setString(2, messageLog.getErrorId());

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
    public static synchronized void updateMessageLog(MessageLogModel messageLog) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("UPDATE MessageLog SET iterationID = ?, errorID = ?" +
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

    public static synchronized List<MessageLogModel> getMessageListForIteration(int iterationId) {
        List<MessageLogModel> messageLogModelList = new ArrayList<>();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM MessageLog WHERE iterationID = ?");
            stmt.setInt(1, iterationId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                MessageLogModel messageLogModel = new MessageLogModel();

                messageLogModel.setErrorId(rs.getString(MessageLogModel.ERROR_ID));
                messageLogModel.setIterationId(rs.getInt(MessageLogModel.ITERATION_ID));
                messageLogModel.setMessageId(rs.getInt(MessageLogModel.MESSAGE_ID));

                messageLogModelList.add(messageLogModel);
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

        return messageLogModelList;
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

    //region CURD: GtfsRt Occurrence
    //Create
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

    //Read list from messageId
    public static synchronized List<OccurrenceModel> getOccurrenceListForMessage(int messageId) {

        List<OccurrenceModel> occurrenceModelList = new ArrayList<>();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM Occurrence WHERE messageID = ?");
            stmt.setInt(1, messageId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                OccurrenceModel occurrence = new OccurrenceModel();

                occurrence.setMessageId(rs.getInt(OccurrenceModel.MESSAGE_ID));
                occurrence.setElementPath(rs.getString(OccurrenceModel.ELEMENT_PATH));
                occurrence.setElementValue(rs.getString(OccurrenceModel.ELEMENT_VALUE));

                occurrenceModelList.add(occurrence);
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

        return occurrenceModelList;
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

            stmt = con.prepareStatement("SELECT * FROM GtfsFeed WHERE feedUrl = ?");
            stmt.setString(1, searchFeed.getGtfsUrl());

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

            rs.close();
            stmt.close();
            con.commit();
            con.close();
            return gtfsFeed;

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
            return gtfsFeed;

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

    public static synchronized List<GtfsFeedModel> readAllGtfsFeeds() {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            List<GtfsFeedModel> gtfsFeedModelList = new ArrayList<>();
            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM GtfsFeed");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GtfsFeedModel gtfsFeed = new GtfsFeedModel();

                gtfsFeed.setGtfsUrl(rs.getString("feedURL"));
                gtfsFeed.setFeedId(rs.getInt("feedID"));
                gtfsFeed.setStartTime(rs.getLong("downloadTimestamp"));
                gtfsFeed.setFeedLocation(rs.getString("fileLocation"));

                gtfsFeedModelList.add(gtfsFeed);
            }

            rs.close();
            stmt.close();
            con.commit();
            con.close();
            return gtfsFeedModelList;

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

    //region CURD: GtfsRt Iteration
    //Create
    public static synchronized GtfsFeedIterationModel createGtfsFeedIteration(GtfsFeedModel gtfsFeed) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        int createdId = 0;
        int gtfsFeedId = gtfsFeed.getFeedId();
        long currentTimestamp = TimeStampHelper.getCurrentTimestamp();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsFeedIteration (feedID, IterationTimestamp) VALUES (?,?)");
            stmt.setInt(1, gtfsFeedId);
            stmt.setLong(2, currentTimestamp);

            stmt.executeUpdate();

            //Get the id of the created iteration
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

        return new GtfsFeedIterationModel(createdId, currentTimestamp, gtfsFeedId);
    }

    //Read
    public static synchronized GtfsFeedIterationModel readGtfsIteration(int iterationId) {

        GtfsFeedIterationModel gtfsIteration = new GtfsFeedIterationModel();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM GtfsFeedIteration WHERE IterationID = ?");
            stmt.setInt(1, iterationId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gtfsIteration.setFeedId(rs.getInt(GtfsFeedIterationModel.FEEDID));
                gtfsIteration.setTimeStamp(rs.getLong(GtfsFeedIterationModel.ITERATIONTIMESTAMP));
                gtfsIteration.setIterationId(rs.getInt(GtfsFeedIterationModel.ITERATIONID));
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
        return gtfsIteration;
    }
    //endregion

    //region CURD: Gtfs Message Log
    //Create
    public static synchronized int createGtfsMessageLog(MessageLogModel messageLog) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        int createdId = 0;

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsMessageLog (errorID, iterationID)VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, messageLog.getErrorId());
            stmt.setInt(2, messageLog.getIterationId());

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
    public static synchronized void updateGtfsMessageLog(MessageLogModel messageLog) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("UPDATE GtfsMessageLog SET errorID = ?" +
                    "WHERE messageID = ?");
            stmt.setString(1, messageLog.getErrorId());
            stmt.setInt(2, messageLog.getMessageId());

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
    public static synchronized MessageLogModel getGtfsMessageLog(int messageId) {
        MessageLogModel messageLogModel = new MessageLogModel();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM GtfsMessageLog WHERE messageID = ?");
            stmt.setInt(1, messageId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messageLogModel.setErrorId(rs.getString(MessageLogModel.ERROR_ID));
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

    public static synchronized List<MessageLogModel> getGtfsMessageList() {
        List<MessageLogModel> messageLogModelList = new ArrayList<>();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);
            stmt = con.prepareStatement("SELECT * FROM GtfsMessageLog");

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                MessageLogModel messageLogModel = new MessageLogModel();

                messageLogModel.setErrorId(rs.getString(MessageLogModel.ERROR_ID));
                messageLogModel.setMessageId(rs.getInt(MessageLogModel.MESSAGE_ID));

                messageLogModelList.add(messageLogModel);
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

        return messageLogModelList;
    }

    //Delete
    public static synchronized void deleteGtfsMessageLog(int messageId) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM GtfsMessageLog WHERE messageID = ?");
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

    public static synchronized void deleteGtfsMessageLogByFeed(int feedId) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM GtfsMessageLog WHERE iterationID = ?");
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

    //region CURD: Gtfs Occurrence
    //Create
    public static synchronized void createGtfsOccurrence(OccurrenceModel occurrence) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsOccurrence (messageID, elementPath, elementValue)VALUES (?,?,?)");
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
    public static synchronized void updateGtfsOccurrence(OccurrenceModel occurrence) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("UPDATE GtfsOccurrence SET messageID = ?, elementPath = ?, elementValue = ?" +
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
    public static synchronized OccurrenceModel getGtfsOccurrence(int occurrenceId) {

        OccurrenceModel occurrenceModel = new OccurrenceModel();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM GtfsOccurrence WHERE occurrenceID = ?");
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

    //Read list from messageId
    public static synchronized List<OccurrenceModel> getGtfsOccurrenceListForMessage(int messageId) {

        List<OccurrenceModel> occurrenceModelList = new ArrayList<>();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;

            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM GtfsOccurrence WHERE messageID = ?");
            stmt.setInt(1, messageId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                OccurrenceModel occurrence = new OccurrenceModel();

                occurrence.setMessageId(rs.getInt(OccurrenceModel.MESSAGE_ID));
                occurrence.setElementPath(rs.getString(OccurrenceModel.ELEMENT_PATH));
                occurrence.setElementValue(rs.getString(OccurrenceModel.ELEMENT_VALUE));

                occurrenceModelList.add(occurrence);
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

        return occurrenceModelList;
    }

    //Delete
    public static synchronized void deleteGtfsOccurrence(int occurenceId) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM GtfsOccurrence WHERE occurrenceID = ?");
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

    //region CURD: Error
    //create
    private static void createError(ValidationRule error) {
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO Error (errorID, errorDescription) VALUES (?,?)");
            stmt.setString(1, error.getErrorId());
            stmt.setString(2, error.getErrorDescription());

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
    public static synchronized List<ValidationRule> getAllErrors() {
        List<ValidationRule> errorList = new ArrayList<>();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM Error");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ValidationRule validationRule = new ValidationRule();
                validationRule.setErrorId(rs.getString(ValidationRule.ERROR_ID));
                validationRule.setErrorDescription(rs.getString(ValidationRule.ERROR_DESCRIPTION));

                errorList.add(validationRule);
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

        return errorList;
    }

    //Delete All
    public static void deleteAllRules(){
        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();

        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM Error");

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

    //region VIEW errorCount
    //Read
    public static synchronized List<ViewErrorCountModel> getErrors(int feedId, int limit) {
        List<ViewErrorCountModel> errorList = new ArrayList<>();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM errorCount WHERE rtFeedID = ? ORDER BY IterationID DESC LIMIT ?");
            stmt.setInt(1, feedId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ViewErrorCountModel errorModel = new ViewErrorCountModel();

                errorModel.setGtfsId(rs.getInt(ViewErrorCountModel.GTFS_ID));
                errorModel.setGtfsRtId(rs.getInt(ViewErrorCountModel.RT_FEED_ID));
                errorModel.setIterationId(rs.getInt(ViewErrorCountModel.ITERATION_ID));
                errorModel.setErrorCount(rs.getInt(ViewErrorCountModel.ERROR_COUNT));
                errorModel.setFeedUrl(rs.getString(ViewErrorCountModel.FEED_URL));
                errorModel.setIterationTime(rs.getLong(ViewErrorCountModel.ITERATION_TIME));

                errorList.add(errorModel);
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

        return errorList;
    }
    //endregion

    //region VIEW gtfsErrorCount
    //---------------------------------------------------------------------------------------
    public static synchronized List<ViewGtfsErrorCountModel> getGtfsErrorList(int feedId){
        List<ViewGtfsErrorCountModel> errorList = new ArrayList<>();



        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM gtfsErrorCount WHERE iterationID = ?");
            stmt.setInt(1, feedId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ViewGtfsErrorCountModel errorModel = new ViewGtfsErrorCountModel();

                errorModel.setMessageId(rs.getInt(ViewGtfsErrorCountModel.MESSAGE_ID));
                errorModel.setErrorId(rs.getString(ViewGtfsErrorCountModel.ERROR_ID));
                errorModel.setErrorDesc(rs.getString(ViewGtfsErrorCountModel.ERROR_DESC));
                errorModel.setIterationId(rs.getInt(ViewGtfsErrorCountModel.ITERATION_ID));
                errorModel.setErrorCount(rs.getInt(ViewGtfsErrorCountModel.ERROR_COUNT));
                errorModel.setFeedUrl(rs.getString(ViewGtfsErrorCountModel.FEED_URL));
                errorModel.setFileLocation(rs.getString(ViewGtfsErrorCountModel.FILE_LOCATION));
                errorModel.setDownloadTime(rs.getLong(ViewGtfsErrorCountModel.DOWNLOAD_TIME));

                errorList.add(errorModel);
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
        System.out.println(errorList.size() + " Number of errors");
        return errorList;
    }
    //---------------------------------------------------------------------------------------
    //endregion


    //region VIEW messageDetails
    //Read
    public static synchronized List<ViewMessageDetailsModel> getViewMessageDetails(int iterationId) {
        List<ViewMessageDetailsModel> messageDetailList = new ArrayList<>();

        Datasource ds = Datasource.getInstance();
        Connection con = ds.getConnection();
        try {
            PreparedStatement stmt;
            con.setAutoCommit(false);

            stmt = con.prepareStatement("SELECT * FROM messageDetails WHERE IterationID = ?");
            stmt.setInt(1, iterationId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ViewMessageDetailsModel messageDetail = new ViewMessageDetailsModel();

                //messageDetail.setFeedProtobuf(rs.getBytes(ViewMessageDetailsModel.FEED_PROTOCOL_BUFFER));
                messageDetail.setMessageId(rs.getInt(ViewMessageDetailsModel.MESSAGE_ID));
                messageDetail.setIterationId(rs.getInt(ViewMessageDetailsModel.ITERATION_ID));
                messageDetail.setErrorDescription(rs.getString(ViewMessageDetailsModel.ERROR_DESC));
                messageDetail.setErrorId(rs.getString(ViewMessageDetailsModel.ERROR_ID));
                messageDetail.setOccurrenceId(rs.getInt(ViewMessageDetailsModel.OCCURRENCE_ID));
                messageDetail.setElementPath(rs.getString(ViewMessageDetailsModel.ELEMENT_PATH));
                messageDetail.setElementValue(rs.getString(ViewMessageDetailsModel.ELEMENT_VALUE));

                messageDetailList.add(messageDetail);
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

        return messageDetailList;
    }
    //endregion

}
