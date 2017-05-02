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
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.CheckTripDescriptor;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.VehicleTripDescriptorValidator;
import org.junit.Test;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static org.junit.Assert.assertEquals;

/* 
 * Tests all the warnings and rules that validate both TripUpdate and VehiclePositions feed.
 * Tests: w003 - If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds
 *        e003 - All trip_ids provided in the GTFS-rt feed must appear in the GTFS data unless schedule_relationship is ADDED
 *        e004 - All route_ids provided in the GTFS-rt feed must appear in the GTFS data
*/
public class TripUpdateVehiclePositionTest extends FeedMessageTest {
    
    public TripUpdateVehiclePositionTest() throws Exception {}
    
    @Test
    public void testTripAndVehicleDescriptorValidation() {
        VehicleTripDescriptorValidator vehicleAndTripDescriptorValidator = new VehicleTripDescriptorValidator();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // setting the same trip_id = 1.1 in VehiclePosition too
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        vehicleDescriptorBuilder.setId("1");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // setting same vehicle id = 1 in VehiclePosition too 
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // TripUpdate and VehiclePosition feed have same trip id = 1.1 and same vehicle id = 1. So, no results.
        results = vehicleAndTripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        for (ErrorListHelperModel error : results) {
            assertEquals(0, error.getOccurrenceList().size());
        }
        
        /* If trip_id's and vehicle_id's in TripUpdate and VehiclePosition are not equal, validator should return an error.
           That is, it fails the validation and passes the assertion.
        */
        // set trip id = 100 in VehiclePosition i.e., not equal to trip id = 1.1 in TripUpdate
        tripDescriptorBuilder.setTripId("100");
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // set vehicle id = 44 in VehiclePosition i.e., not equal to vehicle id = "1" in TripUpdate
        vehicleDescriptorBuilder.setId("44");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // 2 results. Unmatched trip id's and vechicle id's in TripUpdate and VehiclePosition feeds
        results = vehicleAndTripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        for (ErrorListHelperModel error : results) {
            assertEquals(2, error.getOccurrenceList().size());
        }

        clearAndInitRequiredFeedFields();
    }

    /**
     * E003 - All trip_ids provided in the GTFS-rt feed must appear in the GTFS data unless schedule_relationship is ADDED
     * E004 - All route_ids provided in the GTFS-rt feed must appear in the GTFS data
     * W006 - trip_update missing trip_id
     */
    @Test
    public void testTripIdAndRouteIdValidation() {
        CheckTripDescriptor tripIdValidator = new CheckTripDescriptor();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // Don't set a trip_id - 2 warnings
        tripDescriptorBuilder.setRouteId("1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W006, results, 2);

        // setting valid trip_id = 1.1, route_id 1.1 that match with IDs in static Gtfs data - no errors
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setRouteId("1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E003, results, 0);
        TestUtils.assertResults(ValidationRules.E004, results, 0);

        // Set invalid route id = 100 that does not match with any route_id in static Gtfs data - two errors
        tripDescriptorBuilder.setRouteId("100");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E004, results, 2);

        // Reset to valid route ID
        tripDescriptorBuilder.setRouteId("1");

        // Set invalid trip_id = 100 that does not match with any trip_id in static Gtfs data - 2 errors
        tripDescriptorBuilder.setTripId("100");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E003, results, 2);

        // Set that trip_id is ADDED - should go back to 0 errors, as it's ok for trip_id to not be in the GTFS data if schedule_relationship is ADDED
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E003, results, 0);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E016 - trip_ids with schedule_relationship ADDED must not be in GTFS data
     */
    @Test
    public void testE016() {
        CheckTripDescriptor tripIdValidator = new CheckTripDescriptor();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // Set trip_id = 1.1 that's in the GTFS data
        tripDescriptorBuilder.setTripId("1.1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        // No schedule relationship - no errors
        TestUtils.assertResults(ValidationRules.E016, results, 0);

        // Set trip_id = 100 that's not in GTFS, and ADDED schedule relationship - no errors
        tripDescriptorBuilder.setTripId("100");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E016, results, 0);

        // Change to trip_id that's in the GTFS, with a ADDED schedule relationship - 2 errors
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E016, results, 2);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E020 - Invalid start_time format
     */
    @Test
    public void testE020() {
        CheckTripDescriptor tripIdValidator = new CheckTripDescriptor();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // Set valid trip_id = 1.1 that's in the Bull Runner GTFS data
        tripDescriptorBuilder.setTripId("1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        // No start_time - no errors
        TestUtils.assertResults(ValidationRules.E020, results, 0);

        // Set valid start_time - no errors
        tripDescriptorBuilder.setStartTime("00:20:00");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E020, results, 0);

        // Set invalid start_time - 2 errors
        tripDescriptorBuilder.setStartTime("5:15:35");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E020, results, 2);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E021 - Invalid start_date format
     */
    @Test
    public void testE021() {
        CheckTripDescriptor tripIdValidator = new CheckTripDescriptor();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // Set valid trip_id = 1.1 that's in the GTFS data
        tripDescriptorBuilder.setTripId("1.1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        // No start_date - no errors
        TestUtils.assertResults(ValidationRules.E021, results, 0);

        // Set valid start_date - no errors
        tripDescriptorBuilder.setStartDate("20170101");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E021, results, 0);

        // Set invalid start_date - 2 errors
        tripDescriptorBuilder.setStartDate("01-01-2017");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E021, results, 2);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E023 - start_time does not match GTFS initial arrival_time
     */
    @Test
    public void testE023() {
        /**
         * In testagency.txt, trip 1.2 has the following:
         *
         * trip_id,arrival_time,departure_time,stop_id,stop_sequence,shape_dist_traveled,pickup_type,drop_off_type
         * 1.2,00:20:00,00:20:00,A,1,,0,0
         * 1.2,00:30:00,00:30:00,B,2,,0,0
         *
         * So, initial arrival_time is 00:20:00.
         */
        CheckTripDescriptor tripIdValidator = new CheckTripDescriptor();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.2");

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        // No start_time - no errors
        TestUtils.assertResults(ValidationRules.E023, results, 0);

        // Set valid start_time - no errors
        tripDescriptorBuilder.setStartTime("00:20:00");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E023, results, 0);

        // Set invalid start_time - 2 errors
        tripDescriptorBuilder.setStartTime("00:30:00");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E023, results, 2);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E024 - trip direction_id does not match GTFS data
     */
    @Test
    public void testE024() {
        /**
         * In testagency2.txt, trips.txt has the following:
         *
         * route_id,service_id,trip_id,shape_id,block_id,wheelchair_accessible,trip_bikes_allowed,direction_id
         * 1,alldays,1.1,,,1,,0
         * 1,alldays,1.2,,,1,,0
         * 1,alldays,1.3,,,1,,0
         * 2,alldays,2.1,,,0,2,1
         * 2,alldays,2.2,,,0,2,1
         * 3,alldays,3.1,,,1,,
         *
         * So, direction_id for trip 1.1 is 0, and direction_id for trip 2.1 is 1.  trip 3.1 has no direction_id
         */
        CheckTripDescriptor tripIdValidator = new CheckTripDescriptor();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        // No GTFS-rt direction_id - no errors
        TestUtils.assertResults(ValidationRules.E024, results, 0);

        /**
         * Correct GTFS-rt direction_id value 0 - no errors
         */
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setDirectionId(0);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E024, results, 0);

        /**
         * Correct GTFS-rt direction_id value 1 - no errors
         */
        tripDescriptorBuilder.setTripId("2.1");
        tripDescriptorBuilder.setDirectionId(1);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E024, results, 0);

        /**
         * Wrong GTFS-rt direction_id value 1 - 2 errors
         */
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setDirectionId(1);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E024, results, 2);

        /**
         * Wrong GTFS-rt direction_id value 0 - 2 errors
         */
        tripDescriptorBuilder.setTripId("2.1");
        tripDescriptorBuilder.setDirectionId(0);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E024, results, 2);

        /**
         * GTFS-rt direction_id = 0, but no GTFS direction_id - 2 errors
         */
        tripDescriptorBuilder.setTripId("3.1");
        tripDescriptorBuilder.setDirectionId(0);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E024, results, 2);

        clearAndInitRequiredFeedFields();
    }
}
