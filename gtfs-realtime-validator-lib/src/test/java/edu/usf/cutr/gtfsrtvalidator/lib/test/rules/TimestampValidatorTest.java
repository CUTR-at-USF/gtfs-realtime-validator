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
import edu.usf.cutr.gtfsrtvalidator.lib.validation.rules.TimestampValidator;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.MAX_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.*;
import static org.junit.Assert.assertEquals;

/*
 * Tests all the warnings and rules that validate timestamps in TimestampValidator:
 *  * W001 - Timestamps should be populated for all elements
 *  * W007 - Refresh interval more than 35 seconds
 *  * W008 - Header timestamp is older than 65 seconds
 *  * E001 - Not in POSIX time
 *  * E012 - Header timestamp should be greater than or equal to all other timestamps
*/
public class TimestampValidatorTest extends FeedMessageTest {

    public TimestampValidatorTest() throws IOException {
    }

    @Test
    public void testW001() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

        // Timestamp will be zero initially in FeedHeader, TripUpdate and VehiclePosition. Should return 3 results.
        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W001, 3);
        TestUtils.assertResults(expected, results);

        // Populate timestamp to any value greater than zero in FeedHeader
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());
        // Invalid timestamp in TripUpdate and VehiclePosition. Should return 2 results.
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W001, 2);
        TestUtils.assertResults(expected, results);

        // TripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Populate timestamp to any value greater than zero in TripUpdate.
        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Invalid timestamp only in VehiclePosition. Should return 1 results.
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W001, 1);
        TestUtils.assertResults(expected, results);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Now timestamp is populated in FeedHeader, TripUpdate and VehiclePosition . Should return no error.
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E048 - header` `timestamp` not populated (GTFS-rt v2.0 and higher)
     */
    @Test
    public void testE048() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

        // Set version to v2.0
        feedHeaderBuilder.setGtfsRealtimeVersion("2.0");
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        // Timestamp will be zero initially in FeedHeader, TripUpdate and VehiclePosition. Should return 2 W003 results, and 1 E048 for header
        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(W001, 2);
        expected.put(E048, 1);
        TestUtils.assertResults(expected, results);

        // Populate timestamp to any value greater than zero in FeedHeader
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        // Invalid timestamp in TripUpdate and VehiclePosition. Should return 2 W001 warnings, and no E048 errors
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(W001, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W007 - Refresh interval is more than 35 seconds
     */
    @Test
    public void testW007() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        // Set valid trip_id = 1.1
        tripDescriptorBuilder.setTripId("1.1");

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

        /**
         * No previous feed message (i.e., it's the first iteration) - no warnings
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME + 36);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME + 36);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME + 36);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        GtfsRealtime.FeedMessage currentIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        // No previous iteration - no errors
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Set the previous iteration header timestamp so interval is than TimestampValidator.MINIMUM_REFRESH_INTERVAL_SECONDS - no warnings
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME + 10);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME + 10);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME + 10);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        GtfsRealtime.FeedMessage previousIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, previousIteration, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Set the previous iteration header timestamp so interval is more TimestampValidator.MINIMUM_REFRESH_INTERVAL_SECONDS - 1 warning
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        previousIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, previousIteration, null);
        expected.put(W007, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W008 - Header timestamp is older than 65 seconds
     */
    @Test
    public void testW008() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        // Set valid trip_id = 1.1
        tripDescriptorBuilder.setTripId("1.1");

        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeSec = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis);

        /**
         * Use current time - no warnings
         */
        feedHeaderBuilder.setTimestamp(currentTimeSec);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(currentTimeSec);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(currentTimeSec);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        GtfsRealtime.FeedMessage currentIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        results = timestampValidator.validate(currentTimeMillis, gtfsData, gtfsDataMetadata, currentIteration, null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Use current time minus 70 seconds (feed is 1 min 10 sec old) - 1 warning
         */
        feedHeaderBuilder.setTimestamp(currentTimeSec - 70);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(currentTimeSec - 70);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(currentTimeSec - 70);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        currentIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        results = timestampValidator.validate(currentTimeMillis, gtfsData, gtfsDataMetadata, currentIteration, null, null);
        expected.put(W008, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    @Test
    public void testE001() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

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

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Header isn't POSIX - should be 1 error
         */
        // Convert a valid POSIX time to milliseconds - this is a common error in feeds (providing time in milliseconds past epoch instead of seconds)
        final long BAD_TIME = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

        feedHeaderBuilder.setTimestamp(BAD_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E001, 1);
        TestUtils.assertResults(expected, results);

        /**
         * Header and TripUpdate aren't POSIX - 2 errors
         */
        tripUpdateBuilder.setTimestamp(BAD_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E001, 2);
        TestUtils.assertResults(expected, results);

        /**
         * Header, TripUpdate, and VehiclePosition aren't POSIX - 3 errors
         */
        vehiclePositionBuilder.setTimestamp(BAD_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E001, 3);
        TestUtils.assertResults(expected, results);

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

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * 2 StopTimeUpdates, which each have an arrival AND departure POSIX error - so 4 errors total
         */

        // First StopTimeUpdate
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(BAD_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(BAD_TIME));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // Second StopTimeUpdate
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(BAD_TIME + 1));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(BAD_TIME + 1));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E001, 4);
        TestUtils.assertResults(expected, results);

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
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Alert active_period ranges - neither start nor end are valid POSIX, so 2 errors
         */
        timeRangeBuilder.setStart(BAD_TIME);
        timeRangeBuilder.setEnd(BAD_TIME);

        alertBuilder.addActivePeriod(timeRangeBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder);

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E001, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }


    @Test
    public void testE012() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

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

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Header timestamp equal to other entities - no error
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Header timestamp less than VehiclePosition timestamp - 1 error
         */
        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E012, 1);
        TestUtils.assertResults(expected, results);

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
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E012, 1);
        TestUtils.assertResults(expected, results);

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
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E012, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    @Test
    public void testE017() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        // Set valid trip_id = 1.1
        tripDescriptorBuilder.setTripId("1.1");

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

        /**
         * No previous feed message (i.e., it's the first iteration) - no errors
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        GtfsRealtime.FeedMessage currentIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        // No previous iteration - no errors
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Change the trip_id for the previous iteration, but keep the same timestamp - 1 error
         */

        // Set valid trip_id = 1.2
        tripDescriptorBuilder.setTripId("1.2");

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        GtfsRealtime.FeedMessage previousIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, previousIteration, null);
        expected.put(E017, 1);
        TestUtils.assertResults(expected, results);


        /**
         * Change the header timestamp for the current iteration so both trip_id and header.timestamp are
         * different from previous iteration - no errors
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        currentIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, previousIteration, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    @Test
    public void testE018() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        // Set valid trip_id = 1.1
        tripDescriptorBuilder.setTripId("1.1");

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

        /**
         * No previous feed message (i.e., it's the first iteration) - no errors
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        GtfsRealtime.FeedMessage currentIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        // No previous iteration - no errors
        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Set the previous iteration header timestamp so it's less that the current iteration - no errors
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        GtfsRealtime.FeedMessage previousIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, previousIteration, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Set the previous iteration header timestamp so it's greater than that the current iteration - 1 error
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME + 2);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME + 2);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME + 2);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        previousIteration = feedMessageBuilder.setEntity(0, feedEntityBuilder.build()).build();

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, currentIteration, previousIteration, null);
        expected.put(E018, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E022 - trip stop_time_update times are not increasing
     */
    @Test
    public void testE022() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

        /**
         * Set timestamps on objects (without StopTimeUpdates first) so no errors
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Each StopTimeUpdates have same departures (no arrivals), and StopTimeUpdate A times are less than StopTimeUpdate B - no errors
         */
        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripUpdate.StopTimeEvent.Builder stopTimeEventBuilder = GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder();

        // StopTimeUpdate A
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.addStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        tripUpdateBuilder.addStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Each StopTimeUpdates have same arrivals (no departures), and StopTimeUpdate A times are less than StopTimeUpdate B - no errors
         */

        // StopTimeUpdate A
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Each StopTimeUpdates have same arrivals and departures, and StopTimeUpdate A times are less than StopTimeUpdate B - no errors
         */
        // StopTimeUpdate A
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Each StopTimeUpdate has sequential arrivals and departures, and StopTimeUpdate A times are less than StopTimeUpdate B - no errors
         */

        // StopTimeUpdate A
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 2));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 3));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate A has departure time and arrival time equal to StopTimeUpdate B - 4 errors
         */

        // StopTimeUpdate A
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E022, 4);
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate B has arrival time less than StopTimeUpdate A arrival time and StopTimeUpdate B departure time - 2 errors
         */

        // StopTimeUpdate A
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 3));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E022, 2);
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate B has arrival time equal to StopTimeUpdate A arrival time and StopTimeUpdate B departure time - 2 errors
         */

        // StopTimeUpdate A
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 3));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E022, 2);
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate B has arrival time less than StopTimeUpdate A arrival time and StopTimeUpdate B departure time - 2 errors
         */

        // StopTimeUpdate A
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 3));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E022, 2);
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate B has departure time less than StopTimeUpdate A departure time - 1 error
         */

        // StopTimeUpdate A
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 3));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 2));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 2));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E022, 2);
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate B has arrival and departure time less than StopTimeUpdate A arrival time and StopTimeUpdate A departure time - 4 errors
         */

        // StopTimeUpdate A
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 2));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 3));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        // StopTimeUpdate B
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        tripUpdateBuilder.setStopTimeUpdate(1, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E022, 4);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E025 - stop_time_update departure time is before arrival time
     */
    @Test
    public void testE025() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME);

        /**
         * Set timestamps on objects (without StopTimeUpdates first) so no errors
         */
        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate has departure time equal to arrival time - 0 errors
         */
        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripUpdate.StopTimeEvent.Builder stopTimeEventBuilder = GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder();

        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.addStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate has departure time greater than arrival time - 0 errors
         */
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * StopTimeUpdate has departure time less than arrival time - 1 error
         */
        stopTimeUpdateBuilder.setArrival(stopTimeEventBuilder.setTime(MIN_POSIX_TIME + 1));
        stopTimeUpdateBuilder.setDeparture(stopTimeEventBuilder.setTime(MIN_POSIX_TIME));
        tripUpdateBuilder.setStopTimeUpdate(0, stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E025, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E050 - timestamp is in the future.  Tolerance is defined in TimestampValidator.IN_FUTURE_TOLERANCE_SECONDS.
     */
    @Test
    public void testE050() {
        TimestampValidator timestampValidator = new TimestampValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // "Current time" in milliseconds for test - let's use 100 seconds after the minimum valid POSIX TIME (for our validator)
        final long CURRENT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(MIN_POSIX_TIME + 100);

        // Good timestamp (seconds) - 50 seconds behind "current time"
        final long RECENT = TimeUnit.MILLISECONDS.toSeconds(CURRENT_TIME_MILLIS) - 50;
        // Good timestamp (seconds) - in the future by 60 seconds, but still within the 60 second tolerance, so shouldn't log error
        final long FUTURE_60_SEC = TimeUnit.MILLISECONDS.toSeconds(CURRENT_TIME_MILLIS) + 60;
        // Bad future timestamp (SECONDS) - 60 seconds in the future
        final long FUTURE_61_SEC = TimeUnit.MILLISECONDS.toSeconds(CURRENT_TIME_MILLIS) + 61;

        /**
         * All timestamps are in the past - no errors
         */
        feedHeaderBuilder.setTimestamp(RECENT);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(RECENT);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(RECENT);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * All timestamps are in the future, but only by 60 seconds (within the tolerance) - no errors
         */
        feedHeaderBuilder.setTimestamp(FUTURE_60_SEC);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(FUTURE_60_SEC);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(FUTURE_60_SEC);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /**
         * Header timestamp is 61 seconds in future (outside of 60 second tolerance)- one error
         */
        feedHeaderBuilder.setTimestamp(FUTURE_61_SEC);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E050, 1);
        TestUtils.assertResults(expected, results);

        // Check prefix text for error occurrence
        assertEquals("header.timestamp 19:02:41 (1104537761) is 1 min 1 sec greater than 19:01:40 (1104537700000)", results.get(0).getOccurrenceList().get(0).getPrefix());

        /**
         * Header and TripUpdate are 61 seconds in future- 2 errors
         */
        tripUpdateBuilder.setTimestamp(FUTURE_61_SEC);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E050, 2);
        TestUtils.assertResults(expected, results);

        /**
         * Header, TripUpdate, and VehiclePosition are 61 seconds in future - 3 errors
         */
        vehiclePositionBuilder.setTimestamp(FUTURE_61_SEC);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = timestampValidator.validate(CURRENT_TIME_MILLIS, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E050, 3);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * Make sure we throw an exception if current and previous message are the same.  Some rules like E017 and E018
     * require that the feed content for the current and previous iterations passed into the validate() method are different.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateFeedMessagesThrowException() {
        TimestampValidator timestampValidator = new TimestampValidator();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME + 1);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // This should throw an IllegalArgumentException
        results = timestampValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), feedMessageBuilder.build(), null);
    }
}
