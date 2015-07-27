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
import edu.usf.cutr.gtfsrtvalidator.api.model.ErrorMessageModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsRtFeedModel;
import edu.usf.cutr.gtfsrtvalidator.db.Datasource;
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/gtfs-rt-feed")
public class GtfsRtFeed {

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
        feedInfo.setGtfsId(1);
        feedInfo.setStartTime(TimeStampHelper.getCurrentTimestamp());

        //Validate URL for GTFS feed and the GTFS ID.
        if (feedInfo.getGtfsUrl() == null) {
            generateError("GTFS-RT URL is required");
        }else if (feedInfo.getGtfsId() == 0) {
            generateError("GTFS Feed id is required");
        }

        //Checks if the GTFS-RT feed returns valid protobuf
        if (checkFeedType(feedInfo.getGtfsUrl()) == INVALID_FEED) {
            generateError("The GTFS-RT URL given is not a valid feed");
        }

        //Check if url exists in DB
        try {
            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            String sql = "SELECT * FROM GtfsRtFeed WHERE gtfsFeedID=? AND feedURL=?;";
            stmt = con.prepareStatement(sql);

            stmt.setInt(1, feedInfo.getGtfsId());
            stmt.setString(2, feedInfo.getGtfsUrl());

            ResultSet rs = stmt.executeQuery();

            //If record alerady exists, return that item
            if (rs.isBeforeFirst()) {
                GtfsRtFeedModel gtfsFeed = new GtfsRtFeedModel();
                if (rs.next()) {
                    System.out.println("the record exists");
                    gtfsFeed.setGtfsUrl(rs.getString("feedURL"));
                    gtfsFeed.setGtfsId(rs.getInt("gtfsFeedID"));
                    gtfsFeed.setStartTime(rs.getLong("startTime"));

                    return Response.ok(gtfsFeed).build();
                }

            }
            //rtFeedInDB = rs.isBeforeFirst();

            stmt.close();
            con.commit();
            con.close();

        } catch (SQLException | PropertyVetoException | IOException e) {
            e.printStackTrace();
        }

        //If not, create the gtfs-rt feed in the DB and return the feed
        try {
            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            stmt = con.prepareStatement("INSERT INTO GtfsRtFeed (feedUrl, gtfsFeedID, startTime)VALUES (?,?,?)");
            stmt.setString(1, feedInfo.getGtfsUrl());
            stmt.setInt(2, feedInfo.getGtfsId());
            stmt.setLong(3, feedInfo.getStartTime());

            stmt.executeUpdate();

            stmt.close();
            con.commit();
            con.close();

        } catch (SQLException | PropertyVetoException | IOException e) {
            e.printStackTrace();
        }

        return Response.ok(feedInfo).build();
    }


    //GET feed to retrive all RT-feeds
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRtFeeds(){
        List<GtfsRtFeedModel> gtfsFeeds = new ArrayList<>();
        try {
            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            String sql = "SELECT * FROM GtfsRtFeed";
            stmt = con.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            GtfsRtFeedModel gtfsFeed = new GtfsRtFeedModel();

            while (rs.next()) {
                gtfsFeed.setGtfsUrl(rs.getString("feedURL"));
                gtfsFeed.setGtfsId(rs.getInt("gtfsFeedID"));
                gtfsFeed.setStartTime(rs.getLong("startTime"));

                gtfsFeeds.add(gtfsFeed);
            }

            stmt.close();
            con.commit();
            con.close();

        } catch (SQLException | PropertyVetoException | IOException e) {
            e.printStackTrace();
        }
        GenericEntity<List<GtfsRtFeedModel>> feedList = new GenericEntity<List<GtfsRtFeedModel>>(gtfsFeeds){};
        return Response.ok(feedList).build();
    }



    //INFO: @Path("{id : \\d+}") //support digit only
    //TODO: GET {id} return information about the feed with {id}
    //TODO: DELETE {id} remove feed with {id}

    private int checkFeedType(String FeedURL) {
        GtfsRealtime.FeedMessage feed;
        try {
            System.out.println(FeedURL);
            URI FeedURI = new URI(FeedURL);
            URL url = FeedURI.toURL();
            feed = GtfsRealtime.FeedMessage.parseFrom(url.openStream());
        } catch (URISyntaxException | IllegalArgumentException | IOException e ) {
            return INVALID_FEED;
        }
        if (feed.hasHeader()) {
            return VALID_FEED;
        }
        return INVALID_FEED;
    }
}
