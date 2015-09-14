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

import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsFeedIterationModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsFeedModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.ViewGtfsErrorCountModel;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.DBHelper;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.helper.GetFile;
import edu.usf.cutr.gtfsrtvalidator.validation.gtfs.StopLocationTypeValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.GtfsFeedValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;

import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.helper.HttpMessageHelper.generateError;

@Path("/gtfs-feed")
public class GtfsFeed {
    private static final int BUFFER_SIZE = 4096;
    public static Map<Integer, GtfsDaoImpl> GtfsDaoMap = new HashMap<>();

    //DELETE {id} remove feed with the given id
    @DELETE
    @Path("/{id}")
    public Response deleteGtfsFeed(@PathParam("id") String id) {
        GTFSDB.deleteGtfsFeed(Integer.parseInt(id));
        return Response.accepted().build();
    }

    //GET return list of available gtfs-feeds
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGtfsFeeds() {
        List<GtfsFeedModel> gtfsFeeds = new ArrayList<>();
        try {
            List<GtfsFeedModel> tempGtfsFeeds = GTFSDB.readAllGtfsFeeds();
            if (tempGtfsFeeds != null) {
                gtfsFeeds = tempGtfsFeeds;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        GenericEntity<List<GtfsFeedModel>> feedList = new GenericEntity<List<GtfsFeedModel>>(gtfsFeeds) {
        };
        return Response.ok(feedList).build();
    }

    //GET return list of errors for a given feed
    @GET
    @Path("/{id}/errors")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGtfsFeedErrors(@PathParam("id") String id) {
        List<ViewGtfsErrorCountModel> gtfsFeeds = GTFSDB.getGtfsErrorList(Integer.parseInt(id));
        GenericEntity<List<ViewGtfsErrorCountModel>> feedList = new GenericEntity<List<ViewGtfsErrorCountModel>>(gtfsFeeds) {
        };
        return Response.ok(feedList).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postGtfsFeed(@FormParam("gtfsurl") String gtfsFeedUrl) {
        //Extract the URL from the provided gtfsFeedUrl
        URL url = getUrlFromString(gtfsFeedUrl);
        if (url == null)
            return generateError("Malformed URL", "Malformed URL for the GTFS feed", Response.Status.BAD_REQUEST);

        //Open a connection for the given URL
        HttpURLConnection connection = getHttpURLConnection(url);
        if (connection == null)
            return generateError("Can't read from URL", "Can't read content from the GTFS URL", Response.Status.BAD_REQUEST);

        String saveFilePath = getSaveFilePath(gtfsFeedUrl, connection);

        //Read gtfsFeedModel with the same URL in the database
        GtfsFeedModel searchFeed = new GtfsFeedModel();
        searchFeed.setGtfsUrl(gtfsFeedUrl);
        GtfsFeedModel gtfsFeed = GTFSDB.readGtfsFeed(searchFeed);

        //TODO: Move to one method
        if (gtfsFeed != null) {
            System.out.println("URL exists in database");
            File f = new File(gtfsFeed.getFeedLocation());

            //Download the GTFS feed again if it doesn't exist in the filesystem
            if (!f.exists() || f.isDirectory()) {
                downloadGtfsFeed(gtfsFeed.getFeedLocation(), connection);
            }
        }else{
            //If the GTFS file associated with the
            System.out.println("File Doesn't Exist");
            downloadGtfsFeed(saveFilePath, connection);
            gtfsFeed = createGtfsFeedModel(gtfsFeedUrl, saveFilePath);
        }

        //Saves GTFS data to store and validates GTFS feed
        GtfsDaoImpl store = saveGtfsFeed(gtfsFeed);
        if (store == null)
            return generateError("Can't read content", "Can't read content from the GTFS URL", Response.Status.NOT_FOUND);

        //Create a new iteration for the GTFS feed
        GtfsFeedIterationModel gtfsFeedIteration = GTFSDB.createGtfsFeedIteration(gtfsFeed);

        GtfsDaoMap.put(gtfsFeed.getFeedId(), store);

        //Check GTFS feed for errors
        StopLocationTypeValidator StopLocationTypeValidator = new StopLocationTypeValidator();
        validateGtfsError(gtfsFeedIteration.getIterationId(), store, StopLocationTypeValidator);

        //Return the Response from the downloadFeed method
        return Response.ok(gtfsFeed).build();
    }

    //Gets URL from string returns null if failed to parse URL
    private URL getUrlFromString(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            System.out.println("Invalid URL");
            url = null;
        }

        return url;
    }

    private HttpURLConnection getHttpURLConnection(URL url) {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            connection.connect();

            //Check if the request is handled successfully
            if (connection.getResponseCode() / 100 == 2) {
                connection = null;
            }

        } catch (IOException ex) {
            System.out.println("Can't read from GTFS URL");
            connection = null;
        }
        return connection;
    }

    private String getSaveFilePath(String gtfsFeedUrl, HttpURLConnection connection) {
        String saveFilePath;
        String fileName = "";
        String disposition = connection.getHeaderField("Content-Disposition");

        if (disposition == null) {
            //Extracts file name from URL
            fileName = gtfsFeedUrl.substring(gtfsFeedUrl.lastIndexOf("/") + 1,
                    gtfsFeedUrl.length());
        } else {
            //Extracts file name from header field
            int index = disposition.indexOf("filename=");
            if (index > 0) {
                fileName = disposition.substring(index + 10, disposition.length() - 1);
            }
        }

        //get the location of the executed jar file
        GetFile jarInfo = new GetFile();

        //remove file.jar from the path to get the folder where the jar is
        File jarLocation = jarInfo.getJarLocation().getParentFile();
        String saveDir = jarLocation.toString();

        saveFilePath = saveDir + File.separator + fileName;
        return saveFilePath;
    }

    private GtfsFeedModel createGtfsFeedModel(String gtfsFeedUrl, String saveFilePath) {
        GtfsFeedModel gtfsFeed;
        gtfsFeed = new GtfsFeedModel();
        gtfsFeed.setFeedLocation(saveFilePath);
        gtfsFeed.setGtfsUrl(gtfsFeedUrl);

        //Create GTFS feed row in database
        int feedId = GTFSDB.createGtfsFeed(gtfsFeed);

        //Get the newly created GTFSfeed model from id
        gtfsFeed = GTFSDB.readGtfsFeed(feedId);
        return gtfsFeed;
    }

    private GtfsDaoImpl saveGtfsFeed(GtfsFeedModel gtfsFeed) {
        GtfsDaoImpl store = new GtfsDaoImpl();

        try {
            //Read GTFS data into a GtfsDaoImpl
            GtfsReader reader = new GtfsReader();
            reader.setInputLocation(new File(gtfsFeed.getFeedLocation()));

            reader.setEntityStore(store);
            reader.run();
        } catch (Exception ex) {
            return null;
        }
        return store;
    }

    //helper method to validate GTFS feed according to a given rule
    private void validateGtfsError(int iterationID, GtfsDaoImpl gtfsData, GtfsFeedValidator feedEntityValidator) {
        ErrorListHelperModel errorList = feedEntityValidator.validate(gtfsData);

        if (errorList != null && !errorList.getOccurrenceList().isEmpty()) {
            //Set Iteration ID to save the data under
            errorList.getErrorMessage().setIterationId(iterationID);
            //Save the captured errors to the database
            DBHelper.saveGtfsError(errorList);
        }
    }

    private void downloadGtfsFeed(String saveFilePath, HttpURLConnection connection) {
        try {
            // opens input stream from the HTTP connection
            InputStream inputStream = connection.getInputStream();

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
        } catch (IOException ex) {
            System.out.println("Downloading GTFS Feed Failed");
        }
    }
}
