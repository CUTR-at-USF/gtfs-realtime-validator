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
import edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.rules.CrossFeedDescriptorValidator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for rules implemented in CrossFeedDescriptorValidator
 */
public class CrossFeedDescriptorValidatorTest extends FeedMessageTest {

    public CrossFeedDescriptorValidatorTest() throws Exception {
    }

    /**
     * W003 - ID in one feed missing from the other
     */
    @Test
    public void testW003() {
        Map<ValidationRule, Integer> expected = new HashMap<>();

        CrossFeedDescriptorValidator crossFeedDescriptorValidator = new CrossFeedDescriptorValidator();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();

        // Set the same trip and vehicle ID to both TripUpdate and VehiclePosition - no warnings
        vehicleDescriptorBuilder.setId("1");
        tripDescriptorBuilder.setTripId("1.1");

        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Change the VehiclePosition to have trip_id = 100 and vehicle.id = 44, while TripUpdate has trip_id 1.1 and vehicle_id 1.
         * TripUpdate is missing the IDs in VehiclePosition, and VehiclePosition is missing the IDs in TripUpdate - 4 warnings.
         */
        vehicleDescriptorBuilder.setId("44");
        tripDescriptorBuilder.setTripId("100");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.put(ValidationRules.W003, 4);
        TestUtils.assertResults(expected, results);

        /**
         * Clear the VehiclePosition trip_id, and clear the TripUpdates vehicle.id, and add two versions of each
         * (to make sure we catch this case - see
         * https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/241#issuecomment-313194304, although we're no
         * longer using HashBiMaps) - 4 warnings.
         */
        vehicleDescriptorBuilder.clearId();
        tripDescriptorBuilder.setTripId("100");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        vehicleDescriptorBuilder.setId("44");
        tripDescriptorBuilder.clearTripId();
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        vehicleDescriptorBuilder.clearId();
        tripDescriptorBuilder.setTripId("101");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        vehicleDescriptorBuilder.setId("45");
        tripDescriptorBuilder.clearTripId();
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.addEntity(1, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.put(ValidationRules.W003, 4);
        TestUtils.assertResults(expected, results);

        /**
         * Set the VehiclePosition trip_id to empty string, and set the TripUpdates vehicle.id to empty string, and add two versions of each
         * (to make sure we catch this case - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/241#issuecomment-313194304, although we're no
         * longer using HashBiMaps) - 4 warnings.
         */
        vehicleDescriptorBuilder.setId("");
        tripDescriptorBuilder.setTripId("100");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        vehicleDescriptorBuilder.setId("44");
        tripDescriptorBuilder.setTripId("");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        vehicleDescriptorBuilder.setId("");
        tripDescriptorBuilder.setTripId("101");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        vehicleDescriptorBuilder.setId("45");
        tripDescriptorBuilder.setTripId("");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(1, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.put(ValidationRules.W003, 4);
        TestUtils.assertResults(expected, results);

        /**
         * Clear the TripUpdates feed but not the VehiclePositions - no warnings should occur if no TripUpdates feed is provided
         */
        feedEntityBuilder.clearTripUpdate();
        feedMessageBuilder.clearEntity();
        feedMessageBuilder.addEntity(0, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Set the TripUpdates feed and clear the VehiclePositions - no warnings should occur if no VehiclePositions feed is provided
         */
        feedEntityBuilder.clearVehicle();
        feedMessageBuilder.clearEntity();

        vehicleDescriptorBuilder.setId("");
        tripDescriptorBuilder.setTripId("100");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        vehicleDescriptorBuilder.setId("44");
        tripDescriptorBuilder.setTripId("");

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.addEntity(0, feedEntityBuilder.build());

        vehicleDescriptorBuilder.setId("");
        tripDescriptorBuilder.setTripId("101");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        vehicleDescriptorBuilder.setId("45");
        tripDescriptorBuilder.setTripId("");

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.addEntity(1, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }


    /**
     * E047 - VehiclePosition and TripUpdate ID pairing mismatch
     */
    @Test
    public void testE047() {
        Map<ValidationRule, Integer> expected = new HashMap<>();

        CrossFeedDescriptorValidator crossFeedDescriptorValidator = new CrossFeedDescriptorValidator();

        GtfsRealtime.TripDescriptor.Builder tripA = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleA = GtfsRealtime.VehicleDescriptor.newBuilder();

        // Set the same trip and vehicle ID to both TripUpdate and VehiclePosition - no errors
        vehicleA.setId("1");
        tripA.setTripId("1.1");

        tripUpdateBuilder.setVehicle(vehicleA.build());
        tripUpdateBuilder.setTrip(tripA.build());
        vehiclePositionBuilder.setVehicle(vehicleA.build());
        vehiclePositionBuilder.setTrip(tripA.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Change the VehiclePosition to have trip_id 1.1 and vehicle.id = 44, while TripUpdate still has trip_id 1.1 and vehicle_id 1 - 1 mismatch, so 1 error.
         * Also, 2 warnings for W003.
         */
        GtfsRealtime.TripDescriptor.Builder tripB = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleB = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleB.setId("44");
        tripB.setTripId("1.1");
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.put(ValidationRules.W003, 2);
        expected.put(ValidationRules.E047, 1);
        TestUtils.assertResults(expected, results);

        /**
         * Change the VehiclePosition to have trip_id 44 and vehicle.id = 1, while TripUpdate still has trip_id 1.1 and vehicle_id 1.
         * These trips aren't in the same block (same trips.txt block_id), so 1 mismatch, so 1 error.
         * Also, 2 warnings for W003.
         */
        vehicleB.setId("1");
        tripB.setTripId("44");
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.put(ValidationRules.W003, 2);
        expected.put(ValidationRules.E047, 1);
        TestUtils.assertResults(expected, results);

        /**
         * Change the VehiclePosition to have trip_id 44 and vehicle.id = 45, while TripUpdate still has trip_id 1.1 and vehicle_id 1 - 0 mismatch, so 0 errors.
         * Also, 4 warnings for W003.
         */
        vehicleB.setId("45");
        tripB.setTripId("44");
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        expected.put(ValidationRules.W003, 4);
        TestUtils.assertResults(expected, results);

        /**
         * Set the VehiclePosition trip_id to empty string (and create two entities like this, to make sure catch this
         * case - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/241#issuecomment-313194304, although
         * we're no longer using HashBiMaps), and change TripUpdate to trip_id 1.1 and vehicle_id 1 - 0 mismatch, so 0
         * errors. Also, 4 warnings for W003 (2 for TripUpdate, and 1 for each VehiclePosition).
         */
        vehicleB.setId("45");
        tripB.setTripId("");
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        vehicleB.setId("100");
        tripB.setTripId("");
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedEntityBuilder.clearTripUpdate();
        feedMessageBuilder.addEntity(1, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        expected.put(ValidationRules.W003, 4);
        TestUtils.assertResults(expected, results);

        /**
         * Clear the VehiclePosition trip_id (and create two entities like this to make sure we catch this case - see
         * https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/241#issuecomment-313194304, although we're no
         * longer using HashBiMaps), while TripUpdate still has trip_id 1.1 and vehicle_id 1 - 0 mismatch, so 0 errors.
         * Also, 4 warnings for W003 (2 for TripUpdate, and 1 for each VehiclePosition).
         */
        vehicleB.setId("45");
        tripB.clearTripId();
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        vehicleB.setId("100");
        tripB.clearTripId();
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(1, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        expected.put(ValidationRules.W003, 4);
        TestUtils.assertResults(expected, results);

        /**
         * Set the TripUpdate vehicle.id to empty string and VehiclePosition trip_id to empty string (and create two entities like this to make we
         * catch this case - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/241#issuecomment-313194304, although we're no
         * longer using HashBiMaps) - 0 mismatch, so 0 errors.
         * Also, 4 warnings for W003 (two for each entity with empty string IDs).
         */
        vehicleB.setId("45");
        tripB.setTripId("");
        vehiclePositionBuilder.setTrip(tripB);
        vehiclePositionBuilder.setVehicle(vehicleB);

        tripA.setTripId("1");
        vehicleA.setId("");
        tripUpdateBuilder.setTrip(tripA);
        tripUpdateBuilder.setVehicle(vehicleA);

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        vehicleB.setId("46");
        tripB.setTripId("");
        vehiclePositionBuilder.setTrip(tripB);
        vehiclePositionBuilder.setVehicle(vehicleB);

        tripA.setTripId("2");
        vehicleA.setId("");
        tripUpdateBuilder.setTrip(tripA);
        tripUpdateBuilder.setVehicle(vehicleA);

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(1, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        expected.put(ValidationRules.W003, 4);
        TestUtils.assertResults(expected, results);

        /**
         * Clear the TripUpdate vehicle.id and VehiclePosition trip_id (and create two entities like this to make sure
         * we catch this case - see
         * https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/241#issuecomment-313194304, although we're no
         * longer using HashBiMaps) - 0 mismatch, so 0 errors.
         * Also, 4 warnings for W003 (two for each entity with cleared IDs).
         */
        vehicleB.setId("45");
        tripB.clearTripId();
        vehiclePositionBuilder.setTrip(tripB);
        vehiclePositionBuilder.setVehicle(vehicleB);

        tripA.setTripId("1");
        vehicleA.clearId();
        tripUpdateBuilder.setTrip(tripA);
        tripUpdateBuilder.setVehicle(vehicleA);

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        vehicleB.setId("46");
        tripB.clearTripId();
        vehiclePositionBuilder.setTrip(tripB);
        vehiclePositionBuilder.setVehicle(vehicleB);

        tripA.setTripId("2");
        vehicleA.clearId();
        tripUpdateBuilder.setTrip(tripA);
        tripUpdateBuilder.setVehicle(vehicleA);

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(1, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        expected.put(ValidationRules.W003, 4);
        TestUtils.assertResults(expected, results);

        /**
         * Change the TripUpdate to have trip_id 6.1 and vehicle.id = 45, while VehiclePosition is changed to have trip_id 7.1 and vehicle_id 45.
         * Trips 6.1 and 7.1 have the same block_id block.1 (i.e., the same vehicle is going to serve both trips), so having the same vehicle_id is ok - 0 errors.
         * See https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/255 for details on same vehicle running more than one trip in the same block.
         * Also 2 occurrences of W003.
         */
        vehicleA.setId("45");
        tripA.setTripId("6.1");
        tripUpdateBuilder.setVehicle(vehicleA.build());
        tripUpdateBuilder.setTrip(tripA.build());

        vehicleB.setId("45");
        tripB.setTripId("7.1");
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        feedMessageBuilder.removeEntity(1); // Remove the additional entity created in previous tests

        results = crossFeedDescriptorValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, null, null, feedMessageBuilder.build());
        expected.clear();
        expected.put(ValidationRules.W003, 2);
        TestUtils.assertResults(expected, results);
    }
}
