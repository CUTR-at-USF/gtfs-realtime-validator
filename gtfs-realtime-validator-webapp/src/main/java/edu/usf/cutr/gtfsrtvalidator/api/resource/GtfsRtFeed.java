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

import com.google.gson.JsonObject;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.background.BackgroundTask;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.QueryHelper;
import edu.usf.cutr.gtfsrtvalidator.helper.ServiceScheduler;
import edu.usf.cutr.gtfsrtvalidator.lib.model.*;
import edu.usf.cutr.gtfsrtvalidator.lib.model.combined.CombinedIterationMessageModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.combined.CombinedMessageOccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.IterationErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.MergeMonitorData;
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
import java.util.*;
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
        //Validate URL for GTFS feed and the GTFS ID.
        if (feedInfo.getGtfsRtUrl() == null) {
            return generateError("GTFS-RT URL is required");
        } else if (feedInfo.getGtfsFeedModel().getFeedId() == 0) {
            return generateError("GTFS Feed id is required");
        }

        //Check if URL is valid
        try {
            URL feedUrl = new URL(feedInfo.getGtfsRtUrl());
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
        if (checkFeedType(feedInfo.getGtfsRtUrl()) == INVALID_FEED) {
            return generateError("The GTFS-RT URL given is not a valid feed");
        }

        Session session = GTFSDB.initSessionBeginTrans();
        GtfsRtFeedModel storedFeedInfo = (GtfsRtFeedModel) session.createQuery(" FROM GtfsRtFeedModel WHERE "
                + "gtfsRtUrl= :gtfsRtUrl AND gtfsFeedModel.feedId = :feedId")
                .setParameter("gtfsRtUrl", feedInfo.getGtfsRtUrl())
                .setParameter("feedId", feedInfo.getGtfsFeedModel().getFeedId())
                .uniqueResult();
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

    private static HashMap<String, ServiceScheduler> runningTasks = new HashMap<>();

    @PUT
    @Path("/monitor/{id}")
    public Response startMonitor(
            @PathParam("id") int id,
            @QueryParam("clientId") String clientId,
            @DefaultValue("10") @QueryParam("updateInterval") int updateInterval,
            @DefaultValue("true") @QueryParam("enableShapes") String enableShapesStr) {
        // Store the timestamp when we start monitoring feeds that can be used to query database
        currentTimestamp = System.currentTimeMillis();
        //Get RtFeedModel from id
        Session session = GTFSDB.initSessionBeginTrans();
        GtfsRtFeedModel gtfsRtFeed = (GtfsRtFeedModel) session.createQuery(" FROM GtfsRtFeedModel "
                + "WHERE rtFeedID = :id")
                .setParameter("id", id)
                .uniqueResult();

        // Save the session data of a client monitoring feeds.
        SessionModel sessionModel = new SessionModel();
        sessionModel.setClientId(clientId);
        sessionModel.setSessionStartTime(currentTimestamp);
        sessionModel.setGtfsRtFeedModel(gtfsRtFeed);

        session.save(sessionModel);
        GTFSDB.commitAndCloseSession(session);
        boolean intervalUpdated = false;
        int leastInterval = updateInterval;
        if(runningTasks.containsKey(sessionModel.getGtfsRtFeedModel().getGtfsRtUrl()) &&
                leastInterval< runningTasks.get(sessionModel.getGtfsRtFeedModel().getGtfsRtUrl()).getUpdateInterval()){
            intervalUpdated=true;
        }
        boolean enableShapes = true;
        if ("false".equals(enableShapesStr)) {
            enableShapes = false;
        }
        //Extract the Url and gtfsId to start the background process
        startBackgroundTask(gtfsRtFeed, leastInterval, intervalUpdated, enableShapes);

        return Response.ok(sessionModel, MediaType.APPLICATION_JSON).build();
    }

    // Get Monitor data for requested gtfsRtId
    @GET
    @Path("/monitor-data/{id : \\d+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitorData(
            @PathParam("id") int gtfsRtId,
            @QueryParam("summaryCurPage") int summaryCurPage,
            @QueryParam("summaryRowsPerPage") int summaryRowsPerPage,
            @QueryParam("toggledData") String hideErrors,
            @QueryParam("logCurPage") int logCurPage,
            @QueryParam("logRowsPerPage") int logRowsPerPage,
            @DefaultValue("0") @QueryParam("startTime") long sessionStartTime,
            @DefaultValue("0") @QueryParam("endTime") long sessionEndTime) {

        MergeMonitorData mergeMonitorData = new MergeMonitorData();
        if(sessionStartTime <= 0) {
            sessionStartTime = currentTimestamp;
        }
        if(sessionEndTime <= 0) {
            sessionEndTime = System.currentTimeMillis();
        }
        Session session = GTFSDB.initSessionBeginTrans();

        ViewFeedIterationsCount iterationsCount;
        iterationsCount = (ViewFeedIterationsCount) session.createNamedQuery("feedIterationsCount", ViewFeedIterationsCount.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, sessionStartTime)
                .setParameter(2, sessionEndTime)
                .uniqueResult();
        mergeMonitorData.setIterationCount(iterationsCount.getIterationCount());

        ViewFeedUniqueResponseCount uniqueResponseCount;
        uniqueResponseCount = (ViewFeedUniqueResponseCount) session.createNamedQuery("feedUniqueResponseCount", ViewFeedUniqueResponseCount.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, sessionStartTime)
                .setParameter(2, sessionEndTime)
                .uniqueResult();
        mergeMonitorData.setUniqueFeedCount(uniqueResponseCount.getUniqueFeedCount());

        List<ViewGtfsRtFeedErrorCountModel> viewGtfsRtFeedErrorCountModel;
        viewGtfsRtFeedErrorCountModel = session.createNamedQuery("feedErrorCount", ViewGtfsRtFeedErrorCountModel.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, sessionStartTime)
                .setParameter(2, sessionEndTime)
                .list();
        mergeMonitorData.setViewGtfsRtFeedErrorCountModelList(viewGtfsRtFeedErrorCountModel);

        List<ViewErrorSummaryModel> feedSummary;
        int fromRow = (summaryCurPage - 1) * summaryRowsPerPage;
        feedSummary = session.createNamedQuery("ErrorSummaryByrtfeedID", ViewErrorSummaryModel.class)
                .setParameter(0, gtfsRtId)
                .setParameter(1, gtfsRtId)
                .setParameter(2, sessionStartTime)
                .setParameter(3, sessionEndTime)
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
                .setParameter(2, sessionStartTime)
                .setParameter(3, sessionEndTime)
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
     @Path("/feedMessage")
     @Produces(MediaType.APPLICATION_JSON)
     public String getFeedMessage(
             @QueryParam("iterationId") int iterationId,
             @QueryParam("gtfsRtId") int gtfsRtId) {

        ViewFeedMessageModel feedMessageModel;
         Session session = GTFSDB.initSessionBeginTrans();
         if(iterationId != -1) {
             feedMessageModel = session.createNamedQuery("feedMessageByIterationId", ViewFeedMessageModel.class)
                 .setParameter(0, iterationId)
                 .uniqueResult();
         } else {
             feedMessageModel = session.createNamedQuery("feedMessageByGtfsRtId", ViewFeedMessageModel.class)
                     .setParameter(0, gtfsRtId)
                     .setMaxResults(1).getSingleResult();
         }

        GTFSDB.commitAndCloseSession(session);
        feedMessageModel.setJsonFeedMessage(feedMessageModel.getByteFeedMessage());
        return feedMessageModel.getJsonFeedMessage();
     }

    // Returns feed errors/warnings for a requested iteration.
    @GET
    @Path("/iterationErrors")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIterationErrors(
            @QueryParam("iterationId") int iterationId)  {

        List<ViewIterationErrorsModel> viewIterationErrorsModelList;
        IterationErrorListHelperModel iterationErrorListHelperModel;
        List<IterationErrorListHelperModel> iterationErrorListHelperModelList = new ArrayList<>();

        List<Integer> messageIdList;
        Session session = GTFSDB.initSessionBeginTrans();

        /*
         * Get the list of messageIds for an iteration that helps in retrieving error/warning list from Occurrence table
         * Each messageId corresponds to an errorId whose list of error occurrences are retrieved from Occurrence table
         * ORDER BY errorId helps to have errors/warnings in ascending order i.e., first errors in ascending order then warnings in ascending order
         */
        messageIdList = session.createQuery(" SELECT messageId FROM MessageLogModel" +
                                                " WHERE iterationId = :iterationId" +
                                                " ORDER BY errorId")
                .setParameter("iterationId", iterationId)
                .list();

        GTFSDB.closeSession(session);

        /*
         * Each messageId corresponds to an error/warning that occurred in a particular iteration.
         * Iterates over those rule/warning and retrieves list of occurrences of that error/warning and
         *  adds it to the list of IterationErrorListHelperModel.
         * We separately retrieve list of ViewIterationErrorsModel for each error/warning so that we can have
         *  rowIds in increasing order starting from 1 and have separate list for each error/warning.
         */
        for (int messageId: messageIdList) {
            session = GTFSDB.initSessionBeginTrans();
            viewIterationErrorsModelList = session.createNamedQuery("IterationIdErrors", ViewIterationErrorsModel.class)
                    .setParameter(0, iterationId)
                    .setParameter(1, messageId)
                    .list();
            GTFSDB.closeSession(session);
            if (!viewIterationErrorsModelList.isEmpty()) {
                iterationErrorListHelperModel = new IterationErrorListHelperModel();

                // viewIterationErrorsModelList contains the table data to display in each error/warning card.
                iterationErrorListHelperModel.setViewIterationErrorsModelList(viewIterationErrorsModelList);

                // Add errorId and title to IterationErrorListHelperModel that is used to display "ErrorId - Title" for each error/warning card in iteration.html
                iterationErrorListHelperModel.setErrorId(viewIterationErrorsModelList.get(0).getErrorId());
                iterationErrorListHelperModel.setTitle(viewIterationErrorsModelList.get(0).getTitle());
                // Get the number of occurrences of each error/warning
                iterationErrorListHelperModel.setErrorOccurrences(viewIterationErrorsModelList.size());

                iterationErrorListHelperModelList.add(iterationErrorListHelperModel);
            }
        }

        GenericEntity<List<IterationErrorListHelperModel>> iterationErrorList = new GenericEntity<List<IterationErrorListHelperModel>>(iterationErrorListHelperModelList) {
        };
        return Response.ok(iterationErrorList).build();
    }

    // Returns iteration details.
    @GET
    @Path("/iterationSummary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIterationDetails(
            @QueryParam("iterationId") int iterationId,
            @QueryParam("gtfsRtId") int gtfsRtId) {

        GtfsRtFeedIterationModel gtfsRtFeedIterationModel;
        Session session = GTFSDB.initSessionBeginTrans();

        if(iterationId > -1) {
            gtfsRtFeedIterationModel = (GtfsRtFeedIterationModel) session.createQuery(" FROM GtfsRtFeedIterationModel" +
                    " WHERE IterationId = :iterationId")
                    .setParameter("iterationId", iterationId)
                    .uniqueResult();
        } else {
            gtfsRtFeedIterationModel = (GtfsRtFeedIterationModel) session.createQuery(" FROM GtfsRtFeedIterationModel" +
                    " WHERE rtFeedID = :gtfsRtId  ORDER BY IterationTimestamp DESC")
                    .setParameter("gtfsRtId", gtfsRtId)
                    .setMaxResults(1).getSingleResult();
        }


        GTFSDB.closeSession(session);
        gtfsRtFeedIterationModel.setDateFormat(getDateFormat(gtfsRtFeedIterationModel.getFeedTimestamp(), gtfsRtFeedIterationModel.getGtfsRtFeedModel().getGtfsRtId()));
        // Converting feedTimestamp from Milli seconds to seconds as we display timestamp in seconds at client side
        gtfsRtFeedIterationModel.setFeedTimestamp(TimeUnit.MILLISECONDS.toSeconds(gtfsRtFeedIterationModel.getFeedTimestamp()));
        return Response.ok(gtfsRtFeedIterationModel).build();
    }

    // Returns past session details for a particular clientId.
    @GET
    @Path("/pastSessions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSessionData(
            @QueryParam("clientId") String clientId) {

        if (clientId.isEmpty()) {
            String uuid = UUID.randomUUID().toString();
            JsonObject json = new JsonObject();
            json.addProperty("clientId", uuid);
            return Response.ok(json.toString()).build();
        }

        List<SessionModel> sessionModelList;
        Session session = GTFSDB.initSessionBeginTrans();
        long timeDiff;
        int rowId = 1;

        sessionModelList = session.createQuery(" FROM SessionModel" +
                " WHERE clientId = :clientId")
                .setParameter("clientId", clientId)
                .list();

        Iterator iterator = sessionModelList.listIterator();
        SessionModel eachSessionModel;
        DateFormat timeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        while(iterator.hasNext()) {
            eachSessionModel = (SessionModel) iterator.next();

            eachSessionModel.setRowId(rowId);
            rowId++;

            eachSessionModel.setStartTimeFormat(timeFormat.format(eachSessionModel.getSessionStartTime()));
            eachSessionModel.setEndTimeFormat(timeFormat.format(eachSessionModel.getSessionEndTime()));

            timeDiff = eachSessionModel.getSessionEndTime() - eachSessionModel.getSessionStartTime();
            timeFormat.format(timeDiff);
            eachSessionModel.setTotalTime(getTotalTimeFormat(timeDiff));
        }

        GenericEntity<List<SessionModel>> pastSessionsList = new GenericEntity<List<SessionModel>>(sessionModelList) {
        };
        return Response.ok(pastSessionsList).build();
    }

    // Update sessionEndTime, errorCount and warningCount of a session.
    @PUT
    @Path("/{sessionId}/closeSession")
    public void updateSessionData(
            @PathParam("sessionId") int sessionId) {

        long currentTime = System.currentTimeMillis();
        Session session = GTFSDB.initSessionBeginTrans();

        SessionModel sessionModel = (SessionModel) session.createQuery(" FROM SessionModel WHERE sessionId = :sessionId")
                .setParameter("sessionId", sessionId)
                .uniqueResult();
        sessionModel.setSessionEndTime(currentTime);

        List<String> errorAndWarningList = session.createQuery(QueryHelper.sessionErrorsAndWarnings)
                .setParameter("gtfsRtId", sessionModel.getGtfsRtFeedModel().getGtfsRtId())
                .setParameter("startTime", sessionModel.getSessionStartTime())
                .setParameter("endTime", currentTime)
                .list();

        int warningCount = 0;
        int errorCount = 0;
        for (String errorOrWarning: errorAndWarningList) {
            if (errorOrWarning.startsWith("W")) {
                warningCount++;
            } else {
                errorCount++;
            }
        }
        sessionModel.setErrorCount(errorCount);
        sessionModel.setWarningCount(warningCount);
        session.saveOrUpdate(sessionModel);
        GTFSDB.commitAndCloseSession(session);
        if (runningTasks.get(sessionModel.getGtfsRtFeedModel().getGtfsRtUrl()).getParallelClientCount() == 1) {
            runningTasks.get(sessionModel.getGtfsRtFeedModel().getGtfsRtUrl()).getScheduler().shutdown();
            runningTasks.remove(sessionModel.getGtfsRtFeedModel().getGtfsRtUrl());
        } else {
            runningTasks.get(sessionModel.getGtfsRtFeedModel().getGtfsRtUrl()).setParallelClientCount(
                    runningTasks.get(sessionModel.getGtfsRtFeedModel().getGtfsRtUrl()).getParallelClientCount()-1);
        }
    }

    @GET
    @Path("/{id}/{iteration}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessageDetails(@PathParam("id") int id, @PathParam("iteration") int iterationId) {
        CombinedIterationMessageModel messageList = new CombinedIterationMessageModel();
        Session session = GTFSDB.initSessionBeginTrans();
        GtfsRtFeedIterationModel iterationModel =
                (GtfsRtFeedIterationModel) session.createQuery("  FROM GtfsRtFeedIterationModel WHERE " +
                        "IterationId = :iterationId")
                        .setParameter("iterationId", iterationId)
                        .uniqueResult();

        GtfsRtFeedIterationString iterationString = new GtfsRtFeedIterationString(iterationModel);

        messageList.setGtfsFeedIterationModel(iterationString);

        List<CombinedMessageOccurrenceModel> combinedMessageOccurrenceModelList = new ArrayList<>();

        //Get a message list
        List<MessageLogModel> messageLogModels = session.createQuery(
                            " FROM MessageLogModel WHERE IterationId = :iterationId")
                .setParameter("iterationId", iterationId)
                .list();

        //For each message get the occurrences
        for (MessageLogModel messageLog : messageLogModels) {
            List<OccurrenceModel> occurrenceModels = session.createQuery(
                            "FROM OccurrenceModel WHERE messageId = :messageId")
                    .setParameter("messageId", messageLog.getMessageId())
                    .list();
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

    public synchronized static ServiceScheduler startBackgroundTask(GtfsRtFeedModel gtfsRtFeed, int updateInterval,
                                                                    boolean intervalUpdated, boolean enableShapes) {
        String rtFeedUrl = gtfsRtFeed.getGtfsRtUrl();
        gtfsRtFeed.setEnableShapes(enableShapes);
        if (!runningTasks.containsKey(rtFeedUrl)) {
            ServiceScheduler serviceScheduler = new ServiceScheduler();
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(new BackgroundTask(gtfsRtFeed), 0, updateInterval, TimeUnit.SECONDS);
            serviceScheduler.setScheduler(scheduler);
            serviceScheduler.setUpdateInterval(updateInterval);
            serviceScheduler.setParallelClientCount(1);
            runningTasks.put(rtFeedUrl, serviceScheduler);
            return serviceScheduler;
        } else {
            if (intervalUpdated) {
                ServiceScheduler serviceScheduler = runningTasks.get(rtFeedUrl);
                serviceScheduler.getScheduler().shutdown();
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.scheduleAtFixedRate(new BackgroundTask(gtfsRtFeed), 0, updateInterval, TimeUnit.SECONDS);
                serviceScheduler.setScheduler(scheduler);
                serviceScheduler.setUpdateInterval(updateInterval);
                serviceScheduler.setParallelClientCount(serviceScheduler.getParallelClientCount()+1);
                runningTasks.replace(rtFeedUrl, serviceScheduler);
                return serviceScheduler;
            } else {
                runningTasks.get(rtFeedUrl).setParallelClientCount(runningTasks.get(rtFeedUrl).getParallelClientCount()+1);
                runningTasks.get(rtFeedUrl).setUpdateInterval(updateInterval);
                return runningTasks.get(rtFeedUrl);
            }
        }
    }

    public String getDateFormat(long feedTimestamp, int gtfsRtId) {
        Session session = GTFSDB.initSessionBeginTrans();
        GtfsRtFeedModel gtfsRtFeed = (GtfsRtFeedModel) session.createQuery(" FROM GtfsRtFeedModel "
                + "WHERE rtFeedID = :gtfsRtId")
                .setParameter("gtfsRtId", gtfsRtId)
                .uniqueResult();

        GtfsFeedModel gtfsFeed = (GtfsFeedModel) session.createQuery("FROM GtfsFeedModel "
                + "WHERE feedID = :feedID")
                .setParameter("feedID", gtfsRtFeed.getGtfsFeedModel().getFeedId())
                .uniqueResult();
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

    // Returns elapsed time in Xh Xm Xs format.
    public String getTotalTimeFormat(long totalTime) {

        long hours = TimeUnit.MILLISECONDS.toHours(totalTime);
        totalTime -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime);
        totalTime -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(totalTime);

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}
