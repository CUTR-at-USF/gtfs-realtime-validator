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
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;
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
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Path("/gtfs-rt-feed")
public class GtfsRtFeed {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(GtfsRtFeed.class);

    private static final int INVALID_FEED = 0;
    private static final int VALID_FEED = 1;
    PreparedStatement stmt;

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
        feedInfo.setStartTime(TimeStampHelper.getCurrentTimestamp());

        //Validate URL for GTFS feed and the GTFS ID.
        if (feedInfo.getGtfsUrl() == null) {
            return generateError("GTFS-RT URL is required");
        } else if (feedInfo.getGtfsId() == 0) {
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

        Session session = GTFSDB.InitSessionBeginTrans();
        GtfsRtFeedModel storedFeedInfo = (GtfsRtFeedModel) session.createQuery(" FROM GtfsRtFeedModel WHERE "
                + "gtfsUrl= '"+feedInfo.getGtfsUrl()+"' AND gtfsId = "+feedInfo.getGtfsId()).uniqueResult();
        GTFSDB.commitAndCloseSession(session);
        if(storedFeedInfo == null) {    //If null, create the gtfs-rt feed in the DB and return the feed
            session = GTFSDB.InitSessionBeginTrans();
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
            Session session = GTFSDB.InitSessionBeginTrans();
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
    @Path("/{id}/monitor")
    public Response getID(@PathParam("id") int id, @DefaultValue("10") @QueryParam("updateInterval") int updateInterval) {
        //Get RtFeedModel from id
        Session session = GTFSDB.InitSessionBeginTrans();
        GtfsRtFeedModel gtfsRtFeed = (GtfsRtFeedModel) session.createQuery(" FROM GtfsRtFeedModel "
                + "WHERE rtFeedID = "+id).uniqueResult();
        GTFSDB.commitAndCloseSession(session);

        //Extract the Url and gtfsId to start the background process
        startBackgroundTask(gtfsRtFeed, updateInterval);

        return Response.ok(gtfsRtFeed, MediaType.APPLICATION_JSON).build();
    }

    //GET {id} return information about the feed with {id}
    @GET
    @Path("{id : \\d+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRtFeedDetails(@PathParam("id") int id) {
        List<ViewErrorCountModel> gtfsFeeds;
        Session session = GTFSDB.InitSessionBeginTrans();
        gtfsFeeds = session.createNamedQuery("ErrorCountByrtfeedID", ViewErrorCountModel.class)
                .setParameter(0, id).setParameter(1, 10).list();
        GTFSDB.commitAndCloseSession(session);
        GenericEntity<List<ViewErrorCountModel>> feedList = new GenericEntity<List<ViewErrorCountModel>>(gtfsFeeds) {
        };
        return Response.ok(feedList).build();
    }

    @GET
    @Path("/{id}/{iteration}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessageDetails(@PathParam("id") int id, @PathParam("iteration") int iterationId) {
        CombinedIterationMessageModel messageList = new CombinedIterationMessageModel();
        Session session = GTFSDB.InitSessionBeginTrans();
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
}
