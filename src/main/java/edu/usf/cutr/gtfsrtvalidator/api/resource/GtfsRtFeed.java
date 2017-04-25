/*
 * Copyright (C) 2011 Nipuna Gunathilake.
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

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.*;
import edu.usf.cutr.gtfsrtvalidator.api.model.combined.CombinedIterationMessageModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.combined.CombinedMessageOccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.BackgroundTask;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.MergeMonitorData;
import org.hibernate.Session;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Path("/gtfs-rt-feed")
public class GtfsRtFeed {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(GtfsRtFeed.class);

    private static final int INVALID_FEED = 0;
    private static final int VALID_FEED = 1;
    public static String agencyTimezone;
    public static long currentTimestamp;

    public Response generateError(String errorMessage) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessageModel(errorMessage)).build();
    }

    //Add new gtfs-rt feed to monitored list
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response postGtfsRtFeed(GtfsRtFeedModel feedInfo) {
        //feedInfo.setGtfsId(1);
        feedInfo.setStartTime(System.currentTimeMillis());

        //Validate URL for GTFS feed and the GTFS ID.
        if (feedInfo.getGtfsUrl() == null) {
            return generateError("GTFS-RT URL is required");
        } else if (feedInfo.getGtfsFeedModel().getFeedId() == 0) {
            return generateError("GTFS Feed id is required");
        }

        //Check if URL is valid
        try {
            URL feedUrl = new URL(feedInfo.getGtfsUrl());
            HttpURLConnection connection = (HttpURLConnection) feedUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            boolean connectionSuccessful = connection.getResponseCode() / 100 == 2;
            if (!connectionSuccessful) {
                return generateError("URL returns code: " + connection.getResponseCode());
            }
        } catch (Exception ex) {
            return generateError("Invalid URL");
        }


        //Checks if the GTFS-RT feed returns valid protobuf
        if (checkFeedType(feedInfo.getGtfsUrl()) == INVALID_FEED) {
            return generateError("The GTFS-RT URL given is not a valid feed");
        }

        Session session = GTFSDB.initSessionBeginTrans();
        GtfsRtFeedModel storedFeedInfo = (GtfsRtFeedModel) session.createQuery(" FROM GtfsRtFeedModel WHERE "
                + "gtfsUrl= '"+feedInfo.getGtfsUrl()+"' AND gtfsFeedModel.feedId = "+feedInfo.getGtfsFeedModel().getFeedId()).uniqueResult();
        GTFSDB.commitAndCloseSession(session);
        if(storedFeedInfo == null) {    //If null, create the gtfs-rt feed in the DB and return the feed
            session = GTFSDB.initSessionBeginTrans();
            session.save(feedInfo);
            GTFSDB.commitAndCloseSession(session);
        }
        else return Response.ok(storedFeedInfo).build();
        return Response.ok(feedInfo).build();
    }

    //GET feed to retrive all RT-feeds
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRtFeeds() {
        List<GtfsRtFeedModel> gtfsFeeds = new ArrayList<>();
        try {
            Session session = GTFSDB.initSessionBeginTrans();
            gtfsFeeds = session.createQuery(" FROM GtfsRtFeedModel").list();
            GTFSDB.commitAndCloseSession(session);
            } catch (Exception e) {
            e.printStackTrace();
        }
        GenericEntity<List<GtfsRtFeedModel>> feedList = new GenericEntity<List<GtfsRtFeedModel>>(gtfsFeeds) {
        };
        return Response.ok(feedList).build();
    }

    private static HashMap<String, ScheduledExecutorService> runningTasks = new HashMap<>();

    @PUT
    @Path("/{id}/{updateInterval}/monitor")
    public Response getID(@PathParam("id") int id, @PathParam("updateInterval") int updateInterval) {
        // Store the timestamp when we start monitoring feeds that can be used to query database
        currentTimestamp = System.currentTimeMillis();
        //Get RtFeedModel from id
        Session session = GTFSDB.initSessionBeginTrans();
        GtfsRtFeedModel gtfsRtFeed = (GtfsRtFeedModel) session.createQuery(" FROM GtfsRtFeedModel "
                + "WHERE rtFeedID = "+id).uniqueResult();
        GTFSDB.commitAndCloseSession(session);

        //Extract the Url and gtfsId to start the background process
        startBackgroundTask(gtfsRtFeed, updateInterval);

        return Response.ok(gtfsRtFeed, MediaType.APPLICATION_JSON).build();
    }

    // Get Monitor data for requested gtfsRtId
    @GET
    @Path("/{id : \\d+}/summary/pagination/{summaryCurPage: \\d+}/{summaryRowsPerPage: \\d+}" +
            "/log/{toggledData: .*}/pagination/{logCurPage: \\d+}/{logRowsPerPage: \\d+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitorData(
            @PathParam("id") int gtfsRtId,
            @PathParam("summaryCurPage") int summaryCurPage,
            @PathParam("summaryRowsPerPage") int summaryRowsPerPage,
            @PathParam("toggledData") String hideErrors,
            @PathParam("logCurPage") int logCurPage,
            @PathParam("logRowsPerPage") int logRowsPerPage) {

        MergeMonitorData mergeMonitorData = new MergeMonitorData();
        Session session = GTFSDB.initSessionBeginTrans();

        ViewFeedIterationsCount iterationsCount;
        iterationsCount = (ViewFeedIterationsCount) session.createNamedQuery("feedIterationsCount", ViewFeedIterationsCount.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, currentTimestamp)
                .uniqueResult();
        mergeMonitorData.setIterationCount(iterationsCount.getIterationCount());

        ViewFeedUniqueResponseCount uniqueResponseCount;
        uniqueResponseCount = (ViewFeedUniqueResponseCount) session.createNamedQuery("feedUniqueResponseCount", ViewFeedUniqueResponseCount.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, currentTimestamp)
                .uniqueResult();
        mergeMonitorData.setUniqueFeedCount(uniqueResponseCount.getUniqueFeedCount());

        List<ViewGtfsRtFeedErrorCountModel> viewGtfsRtFeedErrorCountModel;
        viewGtfsRtFeedErrorCountModel = session.createNamedQuery("feedErrorCount", ViewGtfsRtFeedErrorCountModel.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, currentTimestamp)
                .list();
        mergeMonitorData.setViewGtfsRtFeedErrorCountModelList(viewGtfsRtFeedErrorCountModel);

        List<ViewErrorSummaryModel> feedSummary;
        int fromRow = (summaryCurPage - 1) * summaryRowsPerPage;
        feedSummary = session.createNamedQuery("ErrorSummaryByrtfeedID", ViewErrorSummaryModel.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, gtfsRtId)
                .setParameter(2, currentTimestamp)
                .setFirstResult(fromRow)
                .setMaxResults(summaryRowsPerPage)
                .list();

        for (ViewErrorSummaryModel viewErrorSummaryModel : feedSummary) {
            int index = feedSummary.indexOf(viewErrorSummaryModel);
            String formattedTimestamp = getDateFormat(viewErrorSummaryModel.getLastFeedTime(), gtfsRtId);
            viewErrorSummaryModel.setFormattedTimestamp(formattedTimestamp);
            viewErrorSummaryModel.setLastFeedTime(TimeUnit.MILLISECONDS.toSeconds(viewErrorSummaryModel.getLastFeedTime()));
            viewErrorSummaryModel.setTimeZone(agencyTimezone);
        }
        mergeMonitorData.setViewErrorSummaryModelList(feedSummary);

        List<ViewErrorLogModel> feedLog;
        String [] removeIds = hideErrors.split(",");

        // Getting the value of fromRow from the rowsPerPage and currentPage values.
        fromRow = (logCurPage - 1) * logRowsPerPage;
        feedLog = session.createNamedQuery("ErrorLogByrtfeedID", ViewErrorLogModel.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, gtfsRtId)
                .setParameter(2, currentTimestamp)
                .setParameterList("errorIds", removeIds)
                .setFirstResult(fromRow)
                .setMaxResults(logRowsPerPage)
                .list();

        for (ViewErrorLogModel viewErrorLogModel: feedLog) {
            String formattedTimestamp = getDateFormat(viewErrorLogModel.getOccurrence(), gtfsRtId);
            viewErrorLogModel.setFormattedTimestamp(formattedTimestamp);
            viewErrorLogModel.setOccurrence(TimeUnit.MILLISECONDS.toSeconds(viewErrorLogModel.getOccurrence()));
            viewErrorLogModel.setTimeZone(agencyTimezone);
        }
        mergeMonitorData.setViewErrorLogModelList(feedLog);

        GTFSDB.closeSession(session);

        return Response.ok(mergeMonitorData).build();
    }

    // Returns feed message for a requested iteration
     @GET
     @Path("/{iterationId : \\d+}/feedMessage")
     @Produces(MediaType.APPLICATION_JSON)
     public String getFeedMessage(
             @PathParam("iterationId") int iterationId) {

        ViewFeedMessageModel feedMessageModel;
         Session session = GTFSDB.initSessionBeginTrans();
        feedMessageModel = session.createNamedQuery("feedMessageByIterationId", ViewFeedMessageModel.class)
                .setParameter(0, iterationId)
                .uniqueResult();
        GTFSDB.commitAndCloseSession(session);
        feedMessageModel.setJsonFeedMessage(feedMessageModel.getByteFeedMessage());
        return feedMessageModel.getJsonFeedMessage();
     }

    @GET
    @Path("/{id}/{iteration}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessageDetails(@PathParam("id") int id, @PathParam("iteration") int iterationId) {
        CombinedIterationMessageModel messageList = new CombinedIterationMessageModel();
        Session session = GTFSDB.initSessionBeginTrans();
        GtfsRtFeedIterationModel iterationModel = (GtfsRtFeedIterationModel) session.
                            createQuery("  FROM GtfsRtFeedIterationModel WHERE IterationId = "+iterationId).uniqueResult();

        GtfsRtFeedIterationString iterationString = new GtfsRtFeedIterationString(iterationModel);

        messageList.setGtfsFeedIterationModel(iterationString);

        List<CombinedMessageOccurrenceModel> combinedMessageOccurrenceModelList = new ArrayList<>();

        //Get a message list
        List<MessageLogModel> messageLogModels = session.createQuery(
                            " FROM MessageLogModel WHERE IterationId = "+iterationId).list();

        //For each message get the occurrences
        for (MessageLogModel messageLog : messageLogModels) {
            List<OccurrenceModel> occurrenceModels = session.createQuery(
                            "FROM OccurrenceModel WHERE messageId = "+messageLog.getMessageId()).list();
            //Add both to the returned list
            CombinedMessageOccurrenceModel messageOccurrence = new CombinedMessageOccurrenceModel();
            messageOccurrence.setMessageLogModel(messageLog);
            messageOccurrence.setOccurrenceModels(occurrenceModels);

            combinedMessageOccurrenceModelList.add(messageOccurrence);
        }

        messageList.setMessageOccurrenceList(combinedMessageOccurrenceModelList);
        GTFSDB.commitAndCloseSession(session);
        return Response.ok(messageList).build();
    }

    //TODO: DELETE {id} remove feed with {id}
    private int checkFeedType(String FeedURL) {
        GtfsRealtime.FeedMessage feed;
        try {
            URI FeedURI = new URI(FeedURL);
            URL url = FeedURI.toURL();
            feed = GtfsRealtime.FeedMessage.parseFrom(url.openStream());
        } catch (URISyntaxException | IllegalArgumentException | IOException e) {
            return INVALID_FEED;
        }
        if (feed.hasHeader()) {
            _log.info(String.format("%s is a valid GTFS-realtime feed", FeedURL));
            return VALID_FEED;
        }
        return INVALID_FEED;
    }

    public synchronized static ScheduledExecutorService startBackgroundTask(GtfsRtFeedModel gtfsRtFeed, int updateInterval) {
        String rtFeedUrl = gtfsRtFeed.getGtfsUrl();

        if (!runningTasks.containsKey(rtFeedUrl)) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(new BackgroundTask(gtfsRtFeed), 0, updateInterval, TimeUnit.SECONDS);
            runningTasks.put(rtFeedUrl, scheduler);
            return scheduler;
        } else {
            return runningTasks.get(rtFeedUrl);
        }
    }

    public String getDateFormat(long feedTimestamp, int gtfsRtId) {
        Session session = GTFSDB.initSessionBeginTrans();
        GtfsRtFeedModel gtfsRtFeed = (GtfsRtFeedModel) session.createQuery(" FROM GtfsRtFeedModel "
                + "WHERE rtFeedID = " + gtfsRtId).uniqueResult();

        GtfsFeedModel gtfsFeed = (GtfsFeedModel) session.createQuery("FROM GtfsFeedModel "
                + "WHERE feedID = " + gtfsRtFeed.getGtfsFeedModel().getFeedId()).uniqueResult();
        GTFSDB.commitAndCloseSession(session);
        agencyTimezone = gtfsFeed.getAgency();

        DateFormat todaytimeFormat = new SimpleDateFormat("hh:mm:ss a");
        DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
        DateFormat todayDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        TimeZone timeZone = TimeZone.getTimeZone(agencyTimezone);
        todaytimeFormat.setTimeZone(timeZone);
        dateTimeFormat.setTimeZone(timeZone);
        todayDateFormat.setTimeZone(timeZone);

        String formattedTime;
        String currentDate = todayDateFormat.format(System.currentTimeMillis());
        long fromStartOfDay = 0;
        try {
            fromStartOfDay = todayDateFormat.parse(currentDate).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(feedTimestamp < fromStartOfDay) {
            formattedTime = dateTimeFormat.format(feedTimestamp);
        } else {
            formattedTime = todaytimeFormat.format(feedTimestamp);
        }
        return formattedTime;
    }
}
