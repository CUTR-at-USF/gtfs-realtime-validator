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
package edu.usf.cutr.gtfsrtvalidator.test.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.rules.StopValidator;
import org.junit.Test;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;

/**
 * Tests for rules implemented in StopValidator
 */
public class StopValidatorTest extends FeedMessageTest {

    public StopValidatorTest() throws Exception {
    }

    /**
     * E011 - All stop_ids referenced in GTFS-rt feed must appear in the GTFS feed
     */
    @Test
    public void testStopIdValidation() {
        StopValidator locationValidator = new StopValidator();
        
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");
        
        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        // setting stop id = "A" in all the three feeds that matches the stop id in static Gtfs data
        stopTimeUpdateBuilder.setStopId("A");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        vehiclePositionBuilder.setStopId("A");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        entitySelectorBuilder.setStopId("A");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // all the feeds have valid stop id matching that in static Gtfs data. So, returns 0 results
        results = locationValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E011, results, 0);
        
        // setting stop id = "DUMMY" in TripUpdate feed that does not match with any stop id in static Gtfs data
        stopTimeUpdateBuilder.setStopId("DUMMY");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        // one error from TripUpdate feed stop_id = "DUMMY". VehiclePosition and Alert feeds have valid stop id = "A"
        TestUtils.assertResults(ValidationRules.E011, results, 1);
        
        vehiclePositionBuilder.setStopId("DUMMY");
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        // 2 results from TripUpdate and VehiclePosition feeds stop_id="DUMMY". Alert feed have valid stop id ="A"
        TestUtils.assertResults(ValidationRules.E011, results, 2);
        
        entitySelectorBuilder.setStopId("DUMMY");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        // 3 results from TripUpdate, VehiclePosition and alert feeds stop_id="DUMMY"
        TestUtils.assertResults(ValidationRules.E011, results, 3);
        
        clearAndInitRequiredFeedFields();
    }

    /**
     * E015 - All stop_ids referenced in GTFS-rt feeds must have the location_type = 0
     * <p>
     * testagency2.zip has:
     * stop_id A with location_type = 0
     * stop_id B with location_type = 1
     */
    @Test
    public void testStopLocationTypeValidation() {
        StopValidator locationValidator = new StopValidator();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // setting stop id = "A" (which has location_type = 0) in all the three feeds
        stopTimeUpdateBuilder.setStopId("A");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        vehiclePositionBuilder.setStopId("A");
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        entitySelectorBuilder.setStopId("A");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // All the feeds have valid stop id with location_type = 0. So, returns 0 results
        results = locationValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E015, results, 0);

        // Change to use stop_id "B", which has location-type 1
        stopTimeUpdateBuilder.setStopId("B");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        // one error from TripUpdate feed stop_id = "B", which is location_type 1
        TestUtils.assertResults(ValidationRules.E015, results, 1);

        vehiclePositionBuilder.setStopId("B");
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        // 2 results from TripUpdate and VehiclePosition feeds stop_id = "B", which is location_type 1
        TestUtils.assertResults(ValidationRules.E015, results, 2);

        entitySelectorBuilder.setStopId("B");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        // 3 results from TripUpdate, VehiclePosition and alert feeds stop_id = "B", which is location_type 1
        TestUtils.assertResults(ValidationRules.E015, results, 3);

        clearAndInitRequiredFeedFields();
    }
}
