/*
 * Copyright (C) 2017 University of South Florida.
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
package edu.usf.cutr.gtfsrtvalidator.lib.test;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.serialization.GtfsReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

/**
 * Base class extended by each individual rule test
 */
public abstract class FeedMessageTest {

    public GtfsDaoImpl gtfsData;
    public GtfsMetadata gtfsDataMetadata;

    public GtfsDaoImpl gtfsData2; // gtfsData2 contains location_type = 1 for stop_id
    public GtfsMetadata gtfsData2Metadata;

    public GtfsDaoImpl bullRunnerGtfs; // For Frequency-based exact_times=0 trips
    public GtfsMetadata bullRunnerGtfsMetadata;

    public GtfsDaoImpl bullRunnerGtfsNoShapes; // Missing shapes.txt
    public GtfsMetadata bullRunnerGtfsNoShapesMetadata;

    public GtfsDaoImpl bullRunnerGtfsTimepointsOnlyLegacyExactTimes1; // Only timepoints in stop_times.txt (without timepoint field)
    public GtfsMetadata bullRunnerGtfsTimepointsOnlyLegacyExactTimes1Metadata;

    public GtfsReader reader;
    public final File staticGtfsFile = new File("src/test/resources/testagency.zip");
    public final File staticGtfs2File = new File("src/test/resources/testagency2.zip");
    public final File bullRunnerGtfsFile = new File("src/test/resources/bullrunner-gtfs.zip");
    public final File bullRunnerNoShapesGtfsFile = new File("src/test/resources/bullrunner-gtfs-no-shapes.zip");
    public final File bullRunnerGtfsTimepointsOnlyLegacyExactTimes1GtfsFile = new File("src/test/resources/bullrunner-gtfs-timepoints-only-legacy-exact-times-1.zip");
    public final static String ENTITY_ID = "TEST_ENTITY";

    public List<ErrorListHelperModel> results;

    public GtfsRealtime.FeedMessage.Builder feedMessageBuilder;
    public GtfsRealtime.FeedEntity.Builder feedEntityBuilder;
    public GtfsRealtime.FeedHeader.Builder feedHeaderBuilder;

    public GtfsRealtime.TripUpdate.Builder tripUpdateBuilder;
    public GtfsRealtime.VehiclePosition.Builder vehiclePositionBuilder;
    public GtfsRealtime.Alert.Builder alertBuilder;

    public FeedMessageTest() throws IOException {
        results = Arrays.asList(new ErrorListHelperModel());

        feedMessageBuilder = GtfsRealtime.FeedMessage.newBuilder();
        feedEntityBuilder = GtfsRealtime.FeedEntity.newBuilder();
        feedHeaderBuilder = GtfsRealtime.FeedHeader.newBuilder();

        tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();
        vehiclePositionBuilder = GtfsRealtime.VehiclePosition.newBuilder();
        alertBuilder = GtfsRealtime.Alert.newBuilder();

        String timeZoneText = null;

        // Read GTFS data into a GtfsDaoImpl
        gtfsData = new GtfsDaoImpl();
        reader = new GtfsReader();
        reader.setInputLocation(staticGtfsFile);
        reader.setEntityStore(gtfsData);
        reader.run();
        Collection<Agency> agencies = gtfsData.getAllAgencies();
        for (Agency agency : agencies) {
            timeZoneText = agency.getTimezone();
            break;
        }
        gtfsDataMetadata = new GtfsMetadata("testagency.zip", TimeZone.getTimeZone(timeZoneText), gtfsData, false);

        gtfsData2 = new GtfsDaoImpl();
        reader = new GtfsReader();
        reader.setInputLocation(staticGtfs2File);
        reader.setEntityStore(gtfsData2);
        reader.run();
        agencies = gtfsData2.getAllAgencies();
        for (Agency agency : agencies) {
            timeZoneText = agency.getTimezone();
            break;
        }
        gtfsData2Metadata = new GtfsMetadata("testagency2.zip", TimeZone.getTimeZone(timeZoneText), gtfsData2, false);

        bullRunnerGtfs = new GtfsDaoImpl();
        reader = new GtfsReader();
        reader.setInputLocation(bullRunnerGtfsFile);
        reader.setEntityStore(bullRunnerGtfs);
        reader.run();
        agencies = bullRunnerGtfs.getAllAgencies();
        for (Agency agency : agencies) {
            timeZoneText = agency.getTimezone();
            break;
        }
        bullRunnerGtfsMetadata = new GtfsMetadata("bullrunner-gtfs.zip", TimeZone.getTimeZone(timeZoneText), bullRunnerGtfs, false);

        bullRunnerGtfsNoShapes = new GtfsDaoImpl();
        reader = new GtfsReader();
        reader.setInputLocation(bullRunnerNoShapesGtfsFile);
        reader.setEntityStore(bullRunnerGtfsNoShapes);
        reader.run();
        agencies = bullRunnerGtfsNoShapes.getAllAgencies();
        for (Agency agency : agencies) {
            timeZoneText = agency.getTimezone();
            break;
        }
        bullRunnerGtfsNoShapesMetadata = new GtfsMetadata("bullrunner-gtfs-no-shapes.zip", TimeZone.getTimeZone(timeZoneText), bullRunnerGtfsNoShapes, false);

        bullRunnerGtfsTimepointsOnlyLegacyExactTimes1 = new GtfsDaoImpl();
        reader = new GtfsReader();
        reader.setInputLocation(bullRunnerGtfsTimepointsOnlyLegacyExactTimes1GtfsFile);
        reader.setEntityStore(bullRunnerGtfsTimepointsOnlyLegacyExactTimes1);
        reader.run();
        agencies = bullRunnerGtfsTimepointsOnlyLegacyExactTimes1.getAllAgencies();
        for (Agency agency : agencies) {
            timeZoneText = agency.getTimezone();
            break;
        }
        bullRunnerGtfsTimepointsOnlyLegacyExactTimes1Metadata = new GtfsMetadata("bullrunner-gtfs-timepoints-only-legacy.zip", TimeZone.getTimeZone(timeZoneText), bullRunnerGtfsTimepointsOnlyLegacyExactTimes1, false);

        clearAndInitRequiredFeedFields();
    }

    // Initialization of some required fields in FeedMessage
    public final void clearAndInitRequiredFeedFields() {
        // clear the fields first
        feedEntityBuilder.clear();
        feedMessageBuilder.clear();
        feedHeaderBuilder.clear();

        tripUpdateBuilder.clear();
        vehiclePositionBuilder.clear();
        alertBuilder.clear();

        feedHeaderBuilder.setGtfsRealtimeVersion("1.0");
        feedMessageBuilder.setHeader(feedHeaderBuilder);

        feedEntityBuilder.setId(ENTITY_ID);
        feedMessageBuilder.addEntity(feedEntityBuilder);
    }
}
