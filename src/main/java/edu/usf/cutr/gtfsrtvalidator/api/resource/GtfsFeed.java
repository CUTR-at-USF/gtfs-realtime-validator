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

import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsFeedModel;
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

@Path("/gtfs-feed")
public class GtfsFeed {
    private static final int BUFFER_SIZE = 4096;
    public static Map<Integer, GtfsDaoImpl> GtfsDaoMap = new HashMap<>();

    //DELETE {id} remove feed with the given id
    @DELETE
    @Path("/{id}")
    public Response deleteGtfsFeed(@PathParam("id") String id){
        GTFSDB.deleteGtfsFeed(Integer.parseInt(id));
        return Response.accepted().build();
    }

    //GET return list of available gtfs-feeds
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGtfsFeeds(){
        List<GtfsFeedModel> gtfsFeeds = new ArrayList<>();
        try {
            gtfsFeeds = GTFSDB.readAllGtfsFeeds();
        } catch (Exception e) {
            e.printStackTrace();
        }
        GenericEntity<List<GtfsFeedModel>> feedList = new GenericEntity<List<GtfsFeedModel>>(gtfsFeeds){};
        return Response.ok(feedList).build();
    }

    //Add gtfs feed to local storage and database
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postGtfsFeed(@FormParam("gtfsurl") String gtfsFeedUrl) {

        GtfsFeedModel downloadedFeed;
        downloadedFeed = downloadFeed(gtfsFeedUrl);

        //Return gtfs item on success
        return Response.ok(downloadedFeed).build();
    }

    private GtfsFeedModel downloadFeed(String gtfsFeedUrl) {
        GtfsFeedModel gtfsModel = null;

        String saveFilePath;
        URL url;

        try {
            url = new URL(gtfsFeedUrl);
        } catch (MalformedURLException ex) {
            System.out.println("Invalid URL");
            return null;
        }

        boolean connectionSuccessful;
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            System.out.println(connection.getResponseCode());

            //Check if the request is handled successfully
            connectionSuccessful = connection.getResponseCode() / 100 == 2;
        } catch (IOException ex) {
            System.out.println("Can't read from URL");
            return null;
        }

        if (connectionSuccessful) {
            //This gets you the size of the file to download (in bytes)
            System.out.println(connection.getContentLength());
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

            GtfsFeedModel searchFeed = new GtfsFeedModel();
            searchFeed.setGtfsUrl(gtfsFeedUrl);

            GtfsFeedModel gtfsFeed = GTFSDB.readGtfsFeed(searchFeed);

            if (gtfsFeed != null) {
                System.out.println("URL exists in database");
                File f = new File(gtfsFeed.getFeedLocation());

                if (f.exists() && !f.isDirectory()) {
                    System.out.println("File in db exists in filesystem");
                    gtfsModel = gtfsFeed;
                } else {
                    //Remove db records and download data
                    System.out.println("File Doesn't Exist in FileSystem");
                    GTFSDB.deleteGtfsFeed(gtfsFeed.getFeedId());
                    System.out.println("DB entry deleted");

                    downloadGtfsFeed(saveFilePath, connection);

                    gtfsFeed = new GtfsFeedModel();
                    gtfsFeed.setFeedLocation(saveFilePath);
                    gtfsFeed.setGtfsUrl(gtfsFeedUrl);

                    //Create GTFS feed row in database
                    int feedId = GTFSDB.createGtfsFeed(gtfsFeed);

                    //Get the newly created GTFSfeed model from id
                    gtfsFeed = GTFSDB.readGtfsFeed(feedId);

                    //Return GTFS Model object
                    gtfsModel = gtfsFeed;
                }
            } else {
                System.out.println("File Doesn't Exist");
                downloadGtfsFeed(saveFilePath, connection);

                gtfsFeed = new GtfsFeedModel();
                gtfsFeed.setFeedLocation(saveFilePath);
                gtfsFeed.setGtfsUrl(gtfsFeedUrl);

                //Create GTFS feed row in database
                GTFSDB.createGtfsFeed(gtfsFeed);

                //Get the newly created GTFSfeed model from url
                gtfsFeed = GTFSDB.readGtfsFeed(gtfsFeed);
                //Return GTFS Model object
                gtfsModel = gtfsFeed;
            }

            if (gtfsFeed != null) {
                try {
                    //Read GTFS data into a GtfsDaoImpl
                    GtfsReader reader = new GtfsReader();
                    reader.setInputLocation(new File(gtfsFeed.getFeedLocation()));

                    GtfsDaoImpl store = new GtfsDaoImpl();
                    reader.setEntityStore(store);
                    reader.run();

                    //Store GtfsDaoImpl to Map
                    GtfsDaoMap.put(gtfsModel.getFeedId(), store);

                    StopLocationTypeValidator StopLocationTypeValidator = new StopLocationTypeValidator();
                    validateGtfs(gtfsModel.getFeedId(), store, StopLocationTypeValidator);

                } catch (Exception ex) {
                    System.out.println("Unable to read from downloaded GTFS feed");
                }
            }
        }
        return gtfsModel;
    }

    //helper method to validate GTFS feed according to a given rule
    private void validateGtfs(int gtfsId, GtfsDaoImpl gtfsData, GtfsFeedValidator feedEntityValidator) {
        ErrorListHelperModel errorList = feedEntityValidator.validate(gtfsData);

        if (errorList != null && !errorList.getOccurrenceList().isEmpty()) {
            //Set Gtfs feed id
            errorList.getErrorMessage().setIterationId(gtfsId);
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
