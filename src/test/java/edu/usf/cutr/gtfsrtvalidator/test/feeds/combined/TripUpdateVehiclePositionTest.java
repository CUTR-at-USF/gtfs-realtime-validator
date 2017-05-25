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
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.CrossFeedDescriptorValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.TripDescriptorValidator;
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
        CrossFeedDescriptorValidator vehicleAndTripDescriptorValidator = new CrossFeedDescriptorValidator();

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
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();

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
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();

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
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();

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
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();

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
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();

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
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();

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

    /**
     * E030 - GTFS-rt alert trip_id does not belong to GTFS-rt alert route_id in GTFS trips.txt
     */
    @Test
    public void testE030() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();

        // In bullrunner-gtfs.zip trips.txt, trip_id=1 belongs to route_id=A
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Don't set route_id or trip_id (but set stop_id - we need at least one specifier) - no errors
        entitySelectorBuilder.setStopId("1234");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E030, results, 0);

        // Set trip_id but not route_id - no errors
        tripDescriptorBuilder.setTripId("1");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E030, results, 0);

        // Set route_id but not trip_id - no errors
        tripDescriptorBuilder.clear();
        entitySelectorBuilder.setRouteId("A");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E030, results, 0);

        // Set route_id and trip_id to correct values according to GTFS trips.txt - no errors
        tripDescriptorBuilder.setTripId("1");
        entitySelectorBuilder.setRouteId("A");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E030, results, 0);

        // Set route_id to something other than the correct value ("A") - 1 error
        tripDescriptorBuilder.setTripId("1");
        entitySelectorBuilder.setRouteId("B");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E030, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E031 - Alert informed_entity.route_id does not match informed_entity.trip.route_id
     */
    @Test
    public void testE031() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();

        // In bullrunner-gtfs.zip routes.txt, route_id=A is a valid route
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Don't set either route_id (but set stop_id - we need at least one specifier) - no errors
        entitySelectorBuilder.setStopId("1234");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E031, results, 0);

        // Set informed_entity.route_id but not informed_entity.trip.route_id - no errors
        entitySelectorBuilder.setRouteId("A");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E031, results, 0);

        // Set informed_entity.trip.route_id but not informed_entity.route_id - no errors
        tripDescriptorBuilder.setRouteId("A");
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E031, results, 0);

        // Set informed_entity.trip.route_id and informed_entity.route_id to the same values - no errors
        tripDescriptorBuilder.setRouteId("A");
        entitySelectorBuilder.setRouteId("A");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E031, results, 0);

        // Set informed_entity.trip.route_id and informed_entity.route_id to they don't match - 1 error
        tripDescriptorBuilder.setRouteId("A");
        entitySelectorBuilder.setRouteId("B");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E031, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E032 - Alert does not have an informed_entity
     */
    @Test
    public void testE032() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();

        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Don't set an informed_entity - 1 error
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E032, results, 1);

        // Add an informed_entity with at least one specifier - no errors
        entitySelectorBuilder.setRouteId("A");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E032, results, 0);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E033 - Alert informed_entity does not have any specifiers
     */
    @Test
    public void testE033() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();

        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Add an informed_entity without any specifiers - 1 error
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E033, results, 1);

        // Set stop_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setStopId("1234");
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E033, results, 0);

        // Set informed_entity.route_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setRouteId("A");
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E033, results, 0);

        // Set agency_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setAgencyId("agency");
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E033, results, 0);

        // Set route_type as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setRouteType(0);
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E033, results, 0);

        // Set trip_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1").build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E033, results, 0);

        // Set informed_entity.trip.route_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("A").build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E033, results, 0);

        // Clear all entity selectors again and don't set any - 1 error
        entitySelectorBuilder.clear();
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E033, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E034 - GTFS-rt agency_id does not exist in GTFS data
     */
    @Test
    public void testE034() {
        // testagency.zip has agency_id=agency
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();

        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Add an informed_entity with an agency_id that exists in GTFS - 0 errors
        entitySelectorBuilder.setAgencyId("agency");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E034, results, 0);

        // Change to agency_id that is NOT in GTFS - 1 error
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setAgencyId("bad");
        alertBuilder.clear();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E034, results, 1);

        clearAndInitRequiredFeedFields();
    }
}
