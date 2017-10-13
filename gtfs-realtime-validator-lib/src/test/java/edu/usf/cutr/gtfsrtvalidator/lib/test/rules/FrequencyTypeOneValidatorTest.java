/*
 * Copyright (C) 2017 University of South Florida
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
import edu.usf.cutr.gtfsrtvalidator.lib.validation.rules.FrequencyTypeOneValidator;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.E019;

/**
 * Tests to evaluate rules for Frequency-based exact_times=1 trips
 * <p>
 * Tests:
 * <p>
 * E019 - GTFS-rt frequency type 1 trip start_time must be a multiple of GTFS data start_time
 */
public class FrequencyTypeOneValidatorTest extends FeedMessageTest {


    public FrequencyTypeOneValidatorTest() throws IOException {
    }

    /**
     * E019 - GTFS-rt trip start_date and start_time must match GTFS data
     * <p>
     * testagency.zip (gtfsData) has exact_times = 1 trips for 15.1 with a 1 hr (3600 sec) headway
     */
    @Test
    public void testE019() {
        Map<ValidationRule, Integer> expected = new HashMap<>();

        /**
         * Set start_time to match GTFS start_time exactly - 6am - no errors
         */
        FrequencyTypeOneValidator frequencyTypeOneValidator = new FrequencyTypeOneValidator();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("15.1");
        tripDescriptorBuilder.setStartTime("06:00:00");

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeOneValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Set start_time to be a multiple of headway_secs - 1 hr later at 7:00am - 0 errors
         */

        tripDescriptorBuilder.setTripId("15.1");
        tripDescriptorBuilder.setStartTime("07:00:00");

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeOneValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Set start_time to NOT be a multiple of headway_secs - 2.5 hr later at 7:30am - 2 errors
         */

        tripDescriptorBuilder.setTripId("15.1");
        tripDescriptorBuilder.setStartTime("07:30:00");

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeOneValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E019, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }
}
