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
import edu.usf.cutr.gtfsrtvalidator.db.GTFSHibernate;
import edu.usf.cutr.gtfsrtvalidator.helper.GetFile;

import javax.servlet.ServletException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

@Path("/gtfs-feed")
public class GtfsFeed {

    private static final int BUFFER_SIZE = 4096;

    //TODO: DELETE {id} remove feed with the given id
    //TODO: PUT update feed with {id}

    //TODO: GET return list of available gtfs-feeds


    //Add gtfs feed to local storage and database
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postGtfsFeed(@FormParam("gtfsurl") String feedUrl) {

        GtfsFeedModel downloadedFeed;

        try {
            downloadedFeed = downloadFeed(feedUrl);
        } catch (ServletException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid GTFS-Feed URL").build();
        }

        //Return gtfs item on success
        return Response.ok(downloadedFeed).build();
    }

    private GtfsFeedModel downloadFeed(String fileURL) throws ServletException, IOException {
        GtfsFeedModel gtfsModel = null;

        String path = GtfsFeed.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        String saveFilePath;

        System.out.println(decodedPath);

        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.connect();
        System.out.println(connection.getResponseCode());
        //Check if the request is handled successfully
        if (connection.getResponseCode() / 100 == 2) {
            //This gets you the size of the file to download (in bytes)
            System.out.println(connection.getContentLength());
            String fileName = "";
            String disposition = connection.getHeaderField("Content-Disposition");

            if (disposition == null) {
                //Extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
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


            GtfsFeedModel gtfsFeed = GTFSDB.getGtfsFeedFromUrl(fileURL);

            if (gtfsFeed != null ) {
                System.out.println("URL exists in database");
                File f = new File(gtfsFeed.getFeedLocation());

                if(f.exists() && !f.isDirectory()){
                    System.out.println("File in db exists in filesystem");
                    gtfsModel = gtfsFeed;
                }else{
                    //Remove db records and download data
                    System.out.println("File Doesn't Exist in FileSystem");
                    GTFSDB.removeGtfsFeedFromUrl(fileURL);
                    System.out.println("DB entry deleted");

                    InputStream inputStream = connection.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                    int bytesRead;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();
                    System.out.println("File downloaded to file system");

                    gtfsFeed = new GtfsFeedModel();
                    gtfsFeed.setFeedLocation(saveFilePath);
                    gtfsFeed.setGtfsUrl(fileURL);
                    //Create GTFS feed row in database
                    GTFSDB.createGtfsFeed(gtfsFeed);

                    //Get the newly created GTFSfeed model from url
                    gtfsFeed = GTFSDB.getGtfsFeedFromUrl(fileURL);

                    //Return GTFS Model object
                    gtfsModel = gtfsFeed;
                }

            } else {
                System.out.println("File Doesn't Exist");

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
                System.out.println("File downloaded");

                gtfsFeed = new GtfsFeedModel();
                gtfsFeed.setFeedLocation(saveFilePath);
                gtfsFeed.setGtfsUrl(fileURL);

                //Create GTFS feed row in database
                GTFSDB.createGtfsFeed(gtfsFeed);

                //Get the newly created GTFSfeed model from url
                gtfsFeed = GTFSDB.getGtfsFeedFromUrl(fileURL);

                //Return GTFS Model object
                gtfsModel = gtfsFeed;

            }
            //GtfsDaoImpl store use the Hibernate database instead
            GTFSHibernate.readToDatastore(saveFilePath);
        }

        return gtfsModel;
    }

}
