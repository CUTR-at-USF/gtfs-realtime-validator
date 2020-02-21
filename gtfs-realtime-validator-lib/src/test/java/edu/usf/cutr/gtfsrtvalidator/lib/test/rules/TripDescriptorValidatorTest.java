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
package edu.usf.cutr.gtfsrtvalidator.lib.test.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.lib.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.lib.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.rules.TripDescriptorValidator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests related to rules implemented in TripDescriptorValidator
 */
public class TripDescriptorValidatorTest extends FeedMessageTest {

    public TripDescriptorValidatorTest() throws Exception {
    }

    /**
     * E003 - All trip_ids provided in the GTFS-rt feed must appear in the GTFS data unless schedule_relationship is ADDED
     * E004 - All route_ids provided in the GTFS-rt feed must appear in the GTFS data
     * W006 - trip_update missing trip_id
     */
    @Test
    public void testE003E004W006() {
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // Don't set a trip_id - 2 warnings
        tripDescriptorBuilder.setRouteId("1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 2);
        TestUtils.assertResults(expected, results);

        // setting valid trip_id = 1.1, route_id 1.1 that match with IDs in static Gtfs data - no errors
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setRouteId("1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set invalid route id = 100 that does not match with any route_id in static Gtfs data - two errors
        tripDescriptorBuilder.setRouteId("100");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E004, 2);
        TestUtils.assertResults(expected, results);

        // Reset to valid route ID
        tripDescriptorBuilder.setRouteId("1");

        // Set invalid trip_id = 100 that does not match with any trip_id in static Gtfs data - 2 errors
        tripDescriptorBuilder.setTripId("100");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E003, 2);
        TestUtils.assertResults(expected, results);

        // Set that trip_id is ADDED - should go back to 0 errors, as it's ok for trip_id to not be in the GTFS data if schedule_relationship is ADDED
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E016 - trip_ids with schedule_relationship ADDED must not be in GTFS data
     */
    @Test
    public void testE016() {
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // Set trip_id = 1.1 that's in the GTFS data
        tripDescriptorBuilder.setTripId("1.1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        // No schedule relationship - no errors
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set trip_id = 100 that's not in GTFS, and ADDED schedule relationship - no errors
        tripDescriptorBuilder.setTripId("100");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Change to trip_id that's in the GTFS, with a ADDED schedule relationship - 2 errors
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E016, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E020 - Invalid start_time format
     */
    @Test
    public void testE020() {
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // Set valid trip_id = 1.1 that's in the Bull Runner GTFS data
        tripDescriptorBuilder.setTripId("1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        // No start_time - no errors
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set valid start_time (HH:MM:SS) - no errors
        tripDescriptorBuilder.setStartTime("00:20:00");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set valid start_time (HH:MM:SS) - no errors
        tripDescriptorBuilder.setStartTime("26:59:59");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set valid start_time (H:MM:SS is ok) - 0 errors
        tripDescriptorBuilder.setStartTime("5:15:35");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set invalid start_time - 2 errors
        tripDescriptorBuilder.setStartTime("005:15:35");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E020, 2);
        TestUtils.assertResults(expected, results);

        // Set invalid start_time - 2 errors
        tripDescriptorBuilder.setStartTime("00:60:60");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E020, 2);
        TestUtils.assertResults(expected, results);

        // Set invalid start_time - 2 errors
        tripDescriptorBuilder.setStartTime("30:00:00");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E020, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E021 - Invalid start_date format
     */
    @Test
    public void testE021() {
        TripDescriptorValidator tripIdValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // Set valid trip_id = 1.1 that's in the GTFS data
        tripDescriptorBuilder.setTripId("1.1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        // No start_date - no errors
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set valid start_date - no errors
        tripDescriptorBuilder.setStartDate("20170101");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set invalid start_date - 2 errors
        tripDescriptorBuilder.setStartDate("01-01-2017");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E021, 2);
        TestUtils.assertResults(expected, results);

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
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.2");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        // No start_time - no errors
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set valid start_time - no errors
        tripDescriptorBuilder.setStartTime("00:20:00");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set invalid start_time - 2 errors
        tripDescriptorBuilder.setStartTime("00:30:00");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E023, 2);
        TestUtils.assertResults(expected, results);

        // Set valid start_time, but with a trip_id that doesn't exist in GTFS data (to make sure no NPE - see #217) - no errors for E023, but 2 errors for E003 missing trip_id in GTFS)
        tripDescriptorBuilder.setStartTime("00:20:00");
        tripDescriptorBuilder.setTripId("100000000");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E003, 2);
        TestUtils.assertResults(expected, results);

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
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null, null);
        // No GTFS-rt direction_id - no errors
        expected.clear();
        TestUtils.assertResults(expected, results);

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

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

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

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

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

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null, null);
        expected.put(E024, 2);
        TestUtils.assertResults(expected, results);

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

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null, null);
        expected.put(E024, 2);
        TestUtils.assertResults(expected, results);

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

        results = tripIdValidator.validate(MIN_POSIX_TIME, gtfsData2, gtfsData2Metadata, feedMessageBuilder.build(), null, null);
        expected.put(E024, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E030 - GTFS-rt alert trip_id does not belong to GTFS-rt alert route_id in GTFS trips.txt
     */
    @Test
    public void testE030() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        // In bullrunner-gtfs.zip trips.txt, trip_id=1 belongs to route_id=A
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Don't set route_id or trip_id (but set stop_id - we need at least one specifier) - 1 warning for missing trip_id
        entitySelectorBuilder.setStopId("1234");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 1);  // Missing trip_id
        TestUtils.assertResults(expected, results);

        // Set trip_id but not route_id - no errors
        tripDescriptorBuilder.setTripId("1");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set route_id but not trip_id - 1 warning for missing trip_id
        tripDescriptorBuilder.clear();
        entitySelectorBuilder.setRouteId("A");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(W006, 1);  // Missing trip_id
        TestUtils.assertResults(expected, results);

        // Set route_id and trip_id to correct values according to GTFS trips.txt - no errors
        tripDescriptorBuilder.setTripId("1");
        entitySelectorBuilder.setRouteId("A");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set route_id to something other than the correct value ("A") - 1 error
        tripDescriptorBuilder.setTripId("1");
        entitySelectorBuilder.setRouteId("B");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E030, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E031 - Alert informed_entity.route_id does not match informed_entity.trip.route_id
     */
    @Test
    public void testE031() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        // In bullrunner-gtfs.zip routes.txt, route_id=A is a valid route
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Don't set either route_id (but set stop_id - we need at least one specifier) - W006 warning for no trip_id
        entitySelectorBuilder.setStopId("1234");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 1);
        TestUtils.assertResults(expected, results);

        // Set informed_entity.route_id but not informed_entity.trip.route_id - no errors - W006 warning for no trip_id
        entitySelectorBuilder.setRouteId("A");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 1);
        TestUtils.assertResults(expected, results);

        // Set informed_entity.trip.route_id but not informed_entity.route_id - W006 warning for no trip_id
        tripDescriptorBuilder.setRouteId("A");
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 1);
        TestUtils.assertResults(expected, results);

        // Set informed_entity.trip.route_id and informed_entity.route_id to the same values - W006 warning for no trip_id
        tripDescriptorBuilder.setRouteId("A");
        entitySelectorBuilder.setRouteId("A");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 1);
        TestUtils.assertResults(expected, results);

        // Set informed_entity.trip.route_id and informed_entity.route_id to they don't match - 1 error
        tripDescriptorBuilder.setRouteId("A");
        entitySelectorBuilder.setRouteId("B");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E031, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E032 - Alert does not have an informed_entity
     */
    @Test
    public void testE032() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Don't set an informed_entity - 1 error
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E032, 1);
        TestUtils.assertResults(expected, results);

        // Add an informed_entity with at least one specifier - no errors
        entitySelectorBuilder.setRouteId("A");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E033 - Alert informed_entity does not have any specifiers
     */
    @Test
    public void testE033() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Add an informed_entity without any specifiers - 1 error
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E033, 1);
        TestUtils.assertResults(expected, results);

        // Set stop_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setStopId("1234");
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set informed_entity.route_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setRouteId("A");
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set agency_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setAgencyId("agency");
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set route_type as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setRouteType(0);
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set trip_id as specifier - 0 errors
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1").setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED).build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set informed_entity.trip.route_id as specifier - 1 warning W006 for missing trip_id
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("A").setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED).build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 1);
        TestUtils.assertResults(expected, results);

        // Clear all entity selectors again and don't set any - 1 error
        entitySelectorBuilder.clear();
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E033, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E034 - GTFS-rt agency_id does not exist in GTFS data
     */
    @Test
    public void testE034() {
        // testagency.zip has agency_id=agency
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Add an informed_entity with an agency_id that exists in GTFS - 0 errors
        entitySelectorBuilder.setAgencyId("agency");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Change to agency_id that is NOT in GTFS - 1 error
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setAgencyId("bad");
        alertBuilder.clear();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E034, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E035 - GTFS-rt trip.trip_id does not belong to GTFS-rt trip.route_id in GTFS trips.txt
     */
    @Test
    public void testE035() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        // In bullrunner-gtfs.zip trips.txt, trip_id=1 belongs to route_id=A
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // Don't set either trip_id or route_id - 1 warning W006 for missing trip_id
        entitySelectorBuilder.setStopId("1234");  // We need at least one specifier
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 1);
        TestUtils.assertResults(expected, results);

        // Set route_id but not trip_id - 1 warning W006 for missing trip_id
        tripDescriptorBuilder.setRouteId("A");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W006, 1);
        TestUtils.assertResults(expected, results);

        // Set trip_id but not route_id - no errors
        tripDescriptorBuilder.clear();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set trip_id to the correct route - no errors
        tripDescriptorBuilder.clear();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setRouteId("A");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Set trip_id to the wrong route - 3 errors
        tripDescriptorBuilder.clear();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setRouteId("B");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        entitySelectorBuilder.clear();
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E035, 3);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W009 - schedule_relationship not populated (for TripDescriptor)
     */
    @Test
    public void testW009TripDescriptor() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        // In bullrunner-gtfs.zip trips.txt, trip_id=1 exists
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        // No schedule_relationship for trip - 3 warnings
        tripDescriptorBuilder.setTripId("1");
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W009, 3);
        TestUtils.assertResults(expected, results);

        // Add schedule_relationship of SCHEDULED for trip - 0 warnings
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        entitySelectorBuilder.setTrip(tripDescriptorBuilder.build());
        alertBuilder.clearInformedEntity();
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W009 - schedule_relationship not populated (for StopTimeUpdate)
     */
    @Test
    public void testW009StopTimeUpdate() {
        TripDescriptorValidator tripDescriptorValidator = new TripDescriptorValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // Missing schedule_relationship for StopTimeUpdate - 1 warning
        stopTimeUpdateBuilder.setStopId("1000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W009, 1);
        TestUtils.assertResults(expected, results);

        // Missing another schedule_relationship for StopTimeUpdate - however, we only flag one occurrence warning per trip, so still 1 warning
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W009, 1);
        TestUtils.assertResults(expected, results);

        // Add schedule_relationship of SCHEDULED for StopTimeUpdates - no warnings
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = tripDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }
}
