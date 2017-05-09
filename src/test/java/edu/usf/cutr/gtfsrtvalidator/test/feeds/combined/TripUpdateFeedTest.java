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
package edu.usf.cutr.gtfsrtvalidator.test.feeds.combined;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopTimeSequenceValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.VehicleValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.gtfs.StopLocationTypeValidator;
import org.junit.Test;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static org.junit.Assert.assertEquals;

/* 
 * Tests all the warnings and rules that validate TripUpdate feed.
 * Tests: W002 - "vehicle_id should be populated in trip_update"
 *        E002 - "stop_time_updates for a given trip_id must be sorted by increasing stop_sequence"
 *        E010 - "If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0"
*/
public class TripUpdateFeedTest extends FeedMessageTest {
    
    public TripUpdateFeedTest() throws Exception {}

    /**
     * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
     */
    @Test
    public void testStopSequenceValidation() {
        StopTimeSequenceValidator stopSequenceValidator = new StopTimeSequenceValidator();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // ordered stop sequence 1, 5
        stopTimeUpdateBuilder.setStopSequence(1);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E002, results, 0);
        
        /* Adding stop sequence 3. So, the stop sequence now is 1, 5, 3 which is unordered.
           So, the validation fails and the assertion test passes
        */
        stopTimeUpdateBuilder.setStopSequence(3);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E002, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W002 - vehicle_id should be populated in TripUpdate and VehiclePosition feeds
     */
    @Test
    public void testVehicleIdValidation() {
        VehicleValidator vehicleValidator = new VehicleValidator();
        
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        
        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        // setting a value for vehicle id = 1
        vehicleDescriptorBuilder.setId("1");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No errors, if vehicle id has a value.
        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W002, results, 0);

        // Test with empty string for Vehicle ID, which should generate 2 warnings (one for TripUpdates and one for VehiclePositions)
        vehicleDescriptorBuilder.setId("");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W002, results, 2);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W004 - VehiclePosition has unrealistic speed
     */
    @Test
    public void testVehicleSpeedValidation() {
        VehicleValidator vehicleValidator = new VehicleValidator();

        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No warnings, if speed isn't populated
        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W004, results, 0);

        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();

        // Lat and long are required fields (use Tampa, FL)
        positionBuilder.setLatitude(27.9506f);
        positionBuilder.setLongitude(-82.4572f);

        /**
         * Valid speed of ~30 miles per hour
         */
        positionBuilder.setSpeed(13.0f);
        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No warnings, for valid speed
        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W004, results, 0);

        /**
         * Invalid negative speed
         */
        positionBuilder.setSpeed(-13.0f);
        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // One warning for negative speed value
        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W004, results, 1);

        /**
         * Abnormally large speed
         */
        positionBuilder.setSpeed(31.0f); // ~ 70 miles per hour
        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // One warning for abnormally large speed
        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W004, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E026 - Invalid vehicle position
     */
    @Test
    public void testInvalidVehiclePosition() {
        VehicleValidator vehicleValidator = new VehicleValidator();

        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No warnings, if position isn't populated
        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E026, results, 0);

        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();

        // Valid lat and long (Tampa, FL)
        positionBuilder.setLatitude(27.9506f);
        positionBuilder.setLongitude(-82.4572f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E026, results, 0);

        // Invalid lat - 1 error
        positionBuilder.setLatitude(1000f);
        positionBuilder.setLongitude(-82.4572f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E026, results, 1);

        // Invalid long - 1 error
        positionBuilder.setLatitude(27.9506f);
        positionBuilder.setLongitude(-1000);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E026, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E010 - If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0
     */
    @Test
    public void testLocationTypeValidation() {
        StopLocationTypeValidator stopLocationValidator = new StopLocationTypeValidator();

        // gtfsData does not contain location_type = 1 for stop_id. Therefore returns 0 results
        results = stopLocationValidator.validate(gtfsData);
        for (ErrorListHelperModel error : results) {
            assertEquals(0, error.getOccurrenceList().size());
        }

        // gtfsData2 contains location_type = 1 for stop_ids. Therefore returns errorcount = (number of location_type = 1 for stop_ids)
        results = stopLocationValidator.validate(gtfsData2);
        TestUtils.assertResults(ValidationRules.E010, results, 1);

        clearAndInitRequiredFeedFields();
    }
}
