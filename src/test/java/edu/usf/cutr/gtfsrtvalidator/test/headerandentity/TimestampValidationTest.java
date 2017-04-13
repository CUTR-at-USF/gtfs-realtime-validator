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
package edu.usf.cutr.gtfsrtvalidator.test.headerandentity;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.TimestampValidation;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MAX_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

/*
 * Tests all the warnings and rules that validate timestamps:
 *  * W001 - Timestamps should be populated for all elements
 *  * E001 - Not in POSIX time
 *  * E012 - Header timestamp should be greater than or equal to all other timestamps
*/
public class TimestampValidationTest extends FeedMessageTest {

    public TimestampValidationTest() throws IOException {
    }

    @Test
    public void testTimestampValidationW001() {
        TimestampValidation timestampValidation = new TimestampValidation();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // Timestamp will be zero initially in FeedHeader, TripUpdate and VehiclePosition. Should return 3 results.
        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(W001, results, 3);

        // Populate timestamp to any value greater than zero in FeedHeader
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());
        // Invalid timestamp in TripUpdate and VehiclePosition. Should return 2 results.
        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(W001, results, 2);

        // TripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Populate timestamp to any value greater than zero in TripUpdate.
        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Invalid timestamp only in VehiclePosition. Should return 1 results.
        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(W001, results, 1);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Now timestamp is populated in FeedHeader, TripUpdate and VehiclePosition . Should return no error.
        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(W001, results, 0);

        clearAndInitRequiredFeedFields();
    }

    @Test
    public void testTimestampValidationE001() {
        TimestampValidation timestampValidation = new TimestampValidation();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        /**
         * All times are POSIX - no errors
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E001, results, 0);

        /**
         * Header isn't POSIX - should be 1 error
         */
        feedHeaderBuilder.setTimestamp(TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME));
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        TestUtils.assertResults(E001, results, 1);

        /**
         * Header and TripUpdate aren't POSIX - 2 errors
         */
        tripUpdateBuilder.setTimestamp(TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME));
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E001, results, 2);

        /**
         * Header, TripUpdate, and VehiclePosition aren't POSIX - 3 errors
         */
        vehiclePositionBuilder.setTimestamp(TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME));
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E001, results, 3);

        /**
         * StopTimeUpdates are all POSIX - no errors
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripUpdate.StopTimeEvent.Builder stopTimeEventBuilder = GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder();

        // First StopTimeUpdate
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.addStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // Second StopTimeUpdate
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MAX_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MAX_POSIX_TIME));
        tripUpdateBuilder.addStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E001, results, 0);

        /**
         * 2 StopTimeUpdates, which each have an arrival AND departure POSIX error - so 4 errors total
         */

        // First StopTimeUpdate
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME)));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME)));
        tripUpdateBuilder.addStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // Second StopTimeUpdate
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(TimeUnit.SECONDS.toMillis(MAX_POSIX_TIME)));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(TimeUnit.SECONDS.toMillis(MAX_POSIX_TIME)));
        tripUpdateBuilder.addStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E001, results, 4);

        // Remove bad POSIX StopTimeUpdates to prep for next assertion
        tripUpdateBuilder.clearStopTimeUpdate();
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        /**
         * Alert active_period ranges - both start and end are valid POSIX, so 0 errors
         */
        GtfsRealtime.Alert.Builder alertBuilder = GtfsRealtime.Alert.newBuilder();
        GtfsRealtime.TimeRange.Builder timeRangeBuilder = GtfsRealtime.TimeRange.newBuilder();
        timeRangeBuilder.setStart(MIN_POSIX_TIME);
        timeRangeBuilder.setEnd(MIN_POSIX_TIME);

        alertBuilder.addActivePeriod(timeRangeBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder);

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E001, results, 0);

        /**
         * Alert active_period ranges - neither start nor end are valid POSIX, so 2 errors
         */
        timeRangeBuilder.setStart(MIN_POSIX_TIME - 1);
        timeRangeBuilder.setEnd(MIN_POSIX_TIME - 1);

        alertBuilder.addActivePeriod(timeRangeBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder);

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E001, results, 2);

        clearAndInitRequiredFeedFields();
    }


    @Test
    public void testTimestampValidationE012() {
        TimestampValidation timestampValidation = new TimestampValidation();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        /**
         * Header timestamp greater than other entities - no error
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E012, results, 0);

        /**
         * Header timestamp equal to other entities - no error
         */
        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E012, results, 0);

        /**
         * Header timestamp less than VehiclePosition timestamp - 1 error
         */
        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E012, results, 1);

        /**
         * Header timestamp less than TripUpdate timestamp - 1 error
         */
        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Feed header timestamp is less than TripUpdate - we should see one error of type E012
        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E012, results, 1);

        /**
         * Header timestamp less than TripUpdate and VehiclePosition timestamps - 2 results
         */
        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Feed header timestamp is less than VehiclePosition and TripUpdate - we should see two results of type E012
        results = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        TestUtils.assertResults(E012, results, 2);

        clearAndInitRequiredFeedFields();
    }


}
