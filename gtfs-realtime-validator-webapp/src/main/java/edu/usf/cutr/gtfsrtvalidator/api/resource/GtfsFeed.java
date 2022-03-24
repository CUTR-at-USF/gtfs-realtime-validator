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

import com.conveyal.gtfs.model.InvalidValue;
import com.conveyal.gtfs.validator.json.FeedProcessor;
import com.conveyal.gtfs.validator.json.FeedValidationResult;
import com.conveyal.gtfs.validator.json.FeedValidationResultSet;
import com.conveyal.gtfs.validator.json.backends.FileSystemFeedBackend;
import com.conveyal.gtfs.validator.json.serialization.JsonSerializer;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.lib.model.GtfsFeedModel;
import edu.usf.cutr.gtfsrtvalidator.util.FileUtil;
import org.hibernate.Session;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.usf.cutr.gtfsrtvalidator.helper.HttpMessageHelper.generateError;

@Path("/gtfs-feed")
public class GtfsFeed {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(GtfsFeed.class);

    private static final int BUFFER_SIZE = 4096;
    public static Map<Integer, GtfsMutableDao> GtfsDaoMap = new ConcurrentHashMap<>();

    //DELETE {id} remove feed with the given id
    @DELETE
    @Path("/{id}")
    public Response deleteGtfsFeed(@PathParam("id") String id) {
        Session session = GTFSDB.initSessionBeginTrans();
        session.createQuery("DELETE FROM GtfsFeedModel WHERE feedID = :feedID")
                .setParameter("feedID", id)
                .executeUpdate();
        GTFSDB.commitAndCloseSession(session);
        return Response.accepted().build();
    }

    //GET return list of available gtfs-feeds
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGtfsFeeds() {
        List<GtfsFeedModel> gtfsFeeds = new ArrayList<>();
        try {
            Session session = GTFSDB.initSessionBeginTrans();
            List<GtfsFeedModel> tempGtfsFeeds = session.createQuery(" FROM GtfsFeedModel").list();
            GTFSDB.commitAndCloseSession(session);
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

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postGtfsFeed(@FormParam("gtfsurl") String gtfsFeedUrl,
                                 @FormParam("enablevalidation") String enableValidation) {
        // Parse URL from the string provided by the web user
        URL url = getUrlFromString(gtfsFeedUrl);
        if (url == null) {
            return generateError("Malformed URL", "Malformed URL for the GTFS feed.", Response.Status.BAD_REQUEST);
        }

        _log.info(String.format("Downloading GTFS data from %s...", url));

        // Open a connection for the given URL
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (connection == null) {
            return generateError("Can't read from URL", "Failed to establish a connection to the GTFS URL.", Response.Status.BAD_REQUEST);
        }

        String gtfsFileName = FileUtil.getGtfsFileName(gtfsFeedUrl);
        // Download GTFS data
        Response.Status response = downloadGtfsFeed(gtfsFileName, connection);
        if (response == Response.Status.BAD_REQUEST) {
            return generateError("Download Failed", "Downloading static GTFS feed from provided Url failed.", Response.Status.BAD_REQUEST);
        } else if (response == Response.Status.FORBIDDEN) {
            // TODO - Needs further testing to determine better error message on Java 11 and up (JCE Extension is no longer required there)
            return generateError("SSL Handshake Failed", "SSL handshake failed.  Try installing the JCE Extension if you're running Java 8 - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/fca9c73b3d3b377c606065648750b777d36ad553/README.md#prerequisites-1", Response.Status.FORBIDDEN);
        }

        _log.info("GTFS zip file downloaded successfully");

        // Get validation request state and history
        boolean validationRequested = "checked".equalsIgnoreCase(enableValidation);
        String projectPath = FileUtil.getJarLocation(this).getParentFile().getAbsolutePath();
        boolean validationFileExists = new File(projectPath + File.separator + FileUtil.GTFS_VALIDATOR_OUTPUT_FILE_PATH + File.separator + gtfsFileName + "_out.json").exists();

        // See if a GTFS feed with the same URL exists in the database
        Session session = GTFSDB.initSessionBeginTrans();
        GtfsFeedModel gtfsFeedModel = (GtfsFeedModel) session.createQuery("FROM GtfsFeedModel "
                + "WHERE gtfsUrl = :gtfsFeedUrl")
                .setParameter("gtfsFeedUrl", gtfsFeedUrl)
                .uniqueResult();

        boolean gtfsChangedOrNew;
        if (gtfsFeedModel == null) {
            _log.info("GTFS URL is new - saving metadata to database...");
            gtfsFeedModel = createGtfsFeedModel(gtfsFeedUrl, gtfsFileName);
            gtfsChangedOrNew = true;
        } else {
            _log.info("GTFS URL already exists exists in database - checking if GTFS data has changed...");
            byte[] newChecksum = calculateMD5checksum(gtfsFeedModel.getFeedLocation());
            byte[] oldChecksum = gtfsFeedModel.getChecksum();
            if (MessageDigest.isEqual(newChecksum, oldChecksum)) {
                _log.info("GTFS data hasn't changed since last execution");
                gtfsChangedOrNew = false;
            } else {
                _log.info("GTFS data has changed, updating metadata in database...");
                gtfsFeedModel.setChecksum(newChecksum);
                updateGtfsFeedModel(gtfsFeedModel);
                gtfsChangedOrNew = true;
            }
        }

        // If the GTFS data isn't loaded into memory, or it's changed, then load it into memory
        GtfsMutableDao gtfsMutableDao = null;
        if (!GtfsDaoMap.containsKey(gtfsFeedModel.getFeedId()) || gtfsChangedOrNew) {
            _log.info("Loading GTFS from downloaded zip file on disk to memory...");
            gtfsMutableDao = loadGtfsFeedFromDisk(gtfsFeedModel);
            if (gtfsMutableDao == null) {
                return generateError("Can't read content", "Can't read GTFS zip file from disk", Response.Status.NOT_FOUND);
            }
            // Keep reference in memory to loaded GTFS data
            GtfsDaoMap.put(gtfsFeedModel.getFeedId(), gtfsMutableDao);
        }

        if (gtfsChangedOrNew) {
            _log.info("Writing GTFS data to database...");
            gtfsFeedModel.setAgency(gtfsMutableDao.getAllAgencies().iterator().next().getTimezone());
            session.update(gtfsFeedModel);
            GTFSDB.commitAndCloseSession(session);
        }

        if (validationRequested && (gtfsChangedOrNew || !validationFileExists)) {
            _log.info("Validating GTFS data...");
            return runStaticGtfsValidation(gtfsFileName, gtfsFeedUrl, gtfsFeedModel);
        }
       return Response.ok(gtfsFeedModel).build();
    }

    private Response runStaticGtfsValidation(String gtfsFileName, String gtfsFeedUrl, GtfsFeedModel gtfsFeed) {
        FileSystemFeedBackend backend = new FileSystemFeedBackend();
        FeedValidationResultSet results = new FeedValidationResultSet();
        File input = backend.getFeed(gtfsFileName);
        FeedProcessor processor = new FeedProcessor(input);
        try {
            _log.info("Running static GTFS validation on " + gtfsFeedUrl + "...");
            processor.run();
        } catch (IOException ex) {
            Logger.getLogger(GtfsFeed.class.getName()).log(Level.SEVERE, null, ex);
            return generateError("Unable to access input GTFS " + input.getPath() + ".", "Does the file " + gtfsFileName + "exist and do I have permission to read it?", Response.Status.NOT_FOUND);
        }
        results.add(processor.getOutput());
        saveGtfsErrorCount(gtfsFeed, processor.getOutput());
        JsonSerializer serializer = new JsonSerializer(results);
        File validationFile = FileUtil.getGtfsValidationOutputFile(this, gtfsFileName);
        try {
            serializer.serializeToFile(validationFile);
            _log.info("Static GTFS validation data written to " + validationFile.getAbsolutePath());
        } catch (Exception e) {
            _log.error("Exception running static GTFS validation on " + gtfsFeedUrl + ": " + e.getMessage());
        }
        return Response.ok(gtfsFeed).build();
    }

    //Gets URL from string returns null if failed to parse URL
    private URL getUrlFromString(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            _log.error("Invalid URL", ex);
            url = null;
        }

        return url;
    }

    private GtfsFeedModel createGtfsFeedModel(String gtfsFeedUrl, String saveFilePath) {
        GtfsFeedModel gtfsFeed;
        gtfsFeed = new GtfsFeedModel();
        gtfsFeed.setFeedLocation(saveFilePath);
        gtfsFeed.setGtfsUrl(gtfsFeedUrl);
        gtfsFeed.setStartTime(System.currentTimeMillis());
        
        byte[] checksum = calculateMD5checksum(saveFilePath);
        gtfsFeed.setChecksum(checksum);

        // Create GTFS feed row in database
        Session session = GTFSDB.initSessionBeginTrans();
        session.save(gtfsFeed);
        GTFSDB.commitAndCloseSession(session);
        return gtfsFeed;
    }

    private GtfsFeedModel updateGtfsFeedModel(GtfsFeedModel gtfsFeed) {        
        //Update GTFS feed row in database
        Session session = GTFSDB.initSessionBeginTrans();
        session.update(gtfsFeed);
        GTFSDB.commitAndCloseSession(session);
        return gtfsFeed;
    }

    private byte[] calculateMD5checksum(String inputFile) {
        byte[] digest = null;
        byte[] dataBytes = new byte[1024];
        int nread;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try {
                InputStream is = Files.newInputStream(Paths.get(inputFile));
                while ((nread = is.read(dataBytes)) != -1)
                    md.update(dataBytes, 0, nread);
                } catch (IOException ex) {
                    Logger.getLogger(GtfsFeed.class.getName()).log(Level.SEVERE, null, ex);
                }
            digest = md.digest();
            }   catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(GtfsFeed.class.getName()).log(Level.SEVERE, null, ex);
            }
        return digest;
    }

    private GtfsMutableDao loadGtfsFeedFromDisk(GtfsFeedModel gtfsFeed) {
        GtfsMutableDao store = new GtfsDaoImpl();

        try {
            //Read GTFS data into a GtfsDaoImpl
            GtfsReader reader = new GtfsReader();
            reader.setInputLocation(new File(gtfsFeed.getFeedLocation()));

            reader.setEntityStore(store);
            reader.run();
        } catch (Exception ex) {
            Logger.getLogger(GtfsFeed.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return store;
    }

    private Response.Status downloadGtfsFeed(String saveFilePath, HttpURLConnection connection) {
        try {
            // Set user agent (#320)
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

            // Check for HTTP 301 redirect
            String redirect = connection.getHeaderField("Location");
            if (redirect != null) {
                _log.warn("Redirecting to " + redirect);
                connection = (HttpURLConnection) new URL(redirect).openConnection();
            }

            // Opens input stream from the HTTP(S) connection
            InputStream inputStream;
            try {
                inputStream = connection.getInputStream();
            } catch (SSLHandshakeException sslEx) {
                // TODO - Needs further testing to determine better error message on Java 11 and up (JCE Extension is no longer required there)
                _log.error("SSL handshake failed.  Try installing the JCE Extension if you're running Java 8 - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/fca9c73b3d3b377c606065648750b777d36ad553/README.md#prerequisites-1", sslEx);
                return Response.Status.FORBIDDEN;
            }

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
            _log.error("Downloading GTFS Feed Failed", ex);
            return Response.Status.BAD_REQUEST;
        }
        return Response.Status.OK;
    }

    private void saveGtfsErrorCount(GtfsFeedModel gtfsFeedModel, FeedValidationResult result) {

        int errorCount = 0;
        for (InvalidValue invalidValue: result.routes.invalidValues) {
            errorCount++;
        }
        for (InvalidValue invalidValue: result.shapes.invalidValues) {
            errorCount++;
        }
        for (InvalidValue invalidValue: result.stops.invalidValues) {
            errorCount++;
        }
        for (InvalidValue invalidValue: result.trips.invalidValues) {
            errorCount++;
        }

        gtfsFeedModel.setErrorCount(errorCount);
        Session session = GTFSDB.initSessionBeginTrans();
        session.update(gtfsFeedModel);
        GTFSDB.commitAndCloseSession(session);
    }

    @GET
    @Path("/{id : \\d+}/errorCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFeedErrorCount(@PathParam("id") int id) {
        Session session = GTFSDB.initSessionBeginTrans();
        GtfsFeedModel gtfsFeed = (GtfsFeedModel) session.createQuery(" FROM GtfsFeedModel WHERE feedId = :id")
                .setParameter("id", id)
                .uniqueResult();
        GTFSDB.commitAndCloseSession(session);

        return Response.ok(gtfsFeed).build();
    }
}
