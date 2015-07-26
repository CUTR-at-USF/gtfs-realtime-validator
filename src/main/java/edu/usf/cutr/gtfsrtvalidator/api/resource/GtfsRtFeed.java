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

import com.google.gson.Gson;
import edu.usf.cutr.gtfsrtvalidator.db.Datasource;
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Path("/gtfs-rt-feed")
public class GtfsRtFeed {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGtfsRtFeed(){

        GtfsRtFeedModel feedInfo = new GtfsRtFeedModel();
        feedInfo.setStartTime(121334);
        feedInfo.setGtfsId(1);
        feedInfo.setGtfsUrl("http://www.google.com");
        Gson gson = new Gson();

        return Response.ok(feedInfo).build();
    }

    //Add new gtfs-rt feed to monitored list
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response postGtfsRtFeed(GtfsRtFeedModel feedInfo) {

        System.out.println(feedInfo.toString());
        //TODO: Check if url exists in DB
        //TODO: If yes, return that item

        feedInfo.setGtfsId(1);
        feedInfo.setStartTime(TimeStampHelper.getCurrentTimestamp());
        //If not, create the gtfs-rt feed in the DB and return the feed
        try {
            PreparedStatement stmt;

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

//
//        try {
//
//        } catch (ServletException | IOException e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid GTFS-Feed URL").build();
//        }
//
        Gson gson = new Gson();
//        SuccessMessage success = new SuccessMessage();
//        success.feedStatus = 1;
        String s = gson.toJson("test");
        return Response.ok(s).build();
    }


    //TODO: GET feed to retrive all rt-feeds

    //INFO: @Path("{id : \\d+}") //support digit only
    //TODO: GET {id} return information about the feed with {id}
    //TODO: DELETE {id} remove feed with {id}
}
