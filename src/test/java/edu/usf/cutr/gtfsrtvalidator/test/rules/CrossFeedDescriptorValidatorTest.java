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
import edu.usf.cutr.gtfsrtvalidator.api.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.rules.CrossFeedDescriptorValidator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E047;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.W003;

/**
 * Tests for rules implemented in CrossFeedDescriptorValidator
 */
public class CrossFeedDescriptorValidatorTest extends FeedMessageTest {

    public CrossFeedDescriptorValidatorTest() throws Exception {
    }

    /**
     * W003 - If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds
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

        results = crossFeedDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = crossFeedDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(W003, 4);
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

        results = crossFeedDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = crossFeedDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(W003, 2);
        expected.put(E047, 1);
        TestUtils.assertResults(expected, results);

        /**
         * Change the VehiclePosition to have trip_id 44 and vehicle.id = 1, while TripUpdate still has trip_id 1.1 and vehicle_id 1 - 1 mismatch, so 1 error.
         * Also, 2 warnings for W003.
         */
        vehicleB.setId("1");
        tripB.setTripId("44");
        vehiclePositionBuilder.setVehicle(vehicleB.build());
        vehiclePositionBuilder.setTrip(tripB.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = crossFeedDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(W003, 2);
        expected.put(E047, 1);
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

        results = crossFeedDescriptorValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        expected.put(W003, 4);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }
}
