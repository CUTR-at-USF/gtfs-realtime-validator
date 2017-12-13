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
import edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.rules.StopTimeUpdateValidator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests for rules implemented in StopTimeUpdateValidator
 */
public class StopTimeUpdateValidatorTest extends FeedMessageTest {

    public StopTimeUpdateValidatorTest() throws Exception {
    }

    /**
     * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
     */
    @Test
    public void testE002() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // tripDescriptor is a required field in tripUpdate, and we need schedule_relationship to avoid W009 warning
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // ordered stop sequence 1, 5 - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Adding stop sequence 3. So, the stop sequence now is 1, 5, 3 which is unordered - 1 error
        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E002, 1);
        TestUtils.assertResults(expected, results);

        // Repeat stop_sequence 3, so order is 1, 3, 3, 5, which is 1 error
        stopTimeUpdateBuilder.clear();
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        assertEquals(4, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E036, 1); // Because stop_ids are repeating back-to-back, we'll also get 1 E036 error
        expected.put(E002, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
     * <p>
     * Tests the case when the GTFS-rt feed is missing the stop_sequence field
     */
    @Test
    public void testE002noStopSequenceGtfsRt() {
        /**
         * bullrunner-gtfs.zip (bullRunnerGtfs) has the following in stop_times.txt:
         *
         * trip_id,arrival_time,departure_time,stop_id,stop_sequence
         * 1,07:00:00,07:00:00,222,1
         * 1,07:01:04,07:01:04,230,2
         * 1,07:01:38,07:01:38,214,3
         * 1,07:02:15,07:02:15,204,4
         * 1,07:02:56,07:02:56,102,5
         * 1,07:03:38,07:03:38,101,6
         * 1,07:04:04,07:04:04,108,7
         * 1,07:04:32,07:04:32,110,8
         * 1,07:05:38,07:05:38,166,9
         * 1,07:06:44,07:06:44,162,10
         * 1,07:07:48,07:07:48,158,11
         * 1,07:08:30,07:08:30,154,12
         * 1,07:09:20,07:09:20,150,13
         * 1,07:09:52,07:09:52,446,14
         * 1,07:11:01,07:11:01,432,15
         * 1,07:11:49,07:11:49,430,16
         * 1,07:12:34,07:12:34,426,17
         * 1,07:13:41,07:13:41,418,18
         * 1,07:14:34,07:14:34,401,19
         * 1,07:16:07,07:16:07,414,20
         * 1,07:16:53,07:16:53,330,21
         * 1,07:17:21,07:17:21,328,22
         * 1,07:17:59,07:17:59,326,23
         * 1,07:18:43,07:18:43,226,24
         * 1,07:19:43,07:19:43,222,25
         */
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_sequence and stop_id pairings all correctly match GTFS - no errors
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E009, 1); // This trip visits a stop more than once, and we're not providing stop_sequence, so we'll get 1 E009 error
        TestUtils.assertResults(expected, results);

        // Swap the first and second update - 1 E002 error
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();

        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E009, 1); // This trip visits a stop more than once, and we're not providing stop_sequence, so we'll get 1 E009 error
        expected.put(E002, 1);
        TestUtils.assertResults(expected, results);

        // Repeat the prediction for stop_id 230 - 1 E002 error
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E009, 1); // This trip visits a stop more than once, and we're not providing stop_sequence, so we'll get 1 E009 error
        expected.put(E037, 1); // We're repeating a stop_id back-to-back, so we'll get 1 E037 error
        expected.put(E002, 1);
        TestUtils.assertResults(expected, results);

        // Put stop_id 154 out of order - 1 E002 error
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E009, 1); // This trip visits a stop more than once, and we're not providing stop_sequence, so we'll get 1 E009 error
        expected.put(E002, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E009 - GTFS-rt stop_sequence isn't provided for trip that visits same stop_id more than once
     */
    @Test
    public void testE009() {
        /**
         * bullrunner-gtfs.zip (bullRunnerGtfs) has the following in stop_times.txt:
         *
         * trip_id,arrival_time,departure_time,stop_id,stop_sequence
         * 1,07:00:00,07:00:00,222,1   // <-- stop_id 222 is visited the first time
         * 1,07:01:04,07:01:04,230,2
         * 1,07:01:38,07:01:38,214,3
         * 1,07:02:15,07:02:15,204,4
         * 1,07:02:56,07:02:56,102,5
         * 1,07:03:38,07:03:38,101,6
         * 1,07:04:04,07:04:04,108,7
         * 1,07:04:32,07:04:32,110,8
         * 1,07:05:38,07:05:38,166,9
         * 1,07:06:44,07:06:44,162,10
         * 1,07:07:48,07:07:48,158,11
         * 1,07:08:30,07:08:30,154,12
         * 1,07:09:20,07:09:20,150,13
         * 1,07:09:52,07:09:52,446,14
         * 1,07:11:01,07:11:01,432,15
         * 1,07:11:49,07:11:49,430,16
         * 1,07:12:34,07:12:34,426,17
         * 1,07:13:41,07:13:41,418,18
         * 1,07:14:34,07:14:34,401,19
         * 1,07:16:07,07:16:07,414,20
         * 1,07:16:53,07:16:53,330,21
         * 1,07:17:21,07:17:21,328,22
         * 1,07:17:59,07:17:59,326,23
         * 1,07:18:43,07:18:43,226,24
         * 1,07:19:43,07:19:43,222,25  // <-- stop_id 222 is visited the second time (loop route)
         */
        StopTimeUpdateValidator stopTimeUpdateValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_sequence is provided for trip_id 1 that includes stop_id 222 twice - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopTimeUpdateValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // stop_sequence is NOT provided for trip_id 1 that includes stop_id 222 twice - 1 error
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clearStopSequence();
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopTimeUpdateValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E009, 1);
        TestUtils.assertResults(expected, results);

        /**
         * Switch to a different GTFS feed with a trip_id 1.1 that doesn't visit the same stop_id more than once in the same trip
         *
         * gtfsData (testagency.zip) has the following for trip 1.1:
         *
         * trip_id	arrival_time	departure_time	stop_id	stop_sequence
         * 1.1	    0:00:00	        0:00:00	        A	    1
         * 1.1	    0:10:00	        0:10:00	        B	    2
         * 1.1	    0:20:00	        0:20:00	        C	    3
         */
        tripDescriptorBuilder.setTripId("1.1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // Include stop_sequence and stop_id - no errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("A");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("B");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("C");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopTimeUpdateValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // No stop_sequence - no errors because trip 1.1 doesn't contains any stop_ids visited more than once
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clearStopSequence();

        stopTimeUpdateBuilder.setStopId("A");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopId("B");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopId("C");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopTimeUpdateValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // No stop_id - no errors
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clearStopId();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopTimeUpdateValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E036 - Sequential stop_time_updates have the same stop_sequence
     */
    @Test
    public void testE036() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1234");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_sequences 1, 5 - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_ids - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop sequence 5 twice (and to make sure we support it, no stopId). So, the stop sequence now is 1, 5, 5 - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E002, 1);  // E002 checks for strict ordering, so we'll get 1 of those here too
        expected.put(E036, 1);
        TestUtils.assertResults(expected, results);

        // stop_sequence 5 twice again, but include stop_id for last stop_time_update - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("3000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E002, 1);  // E002 checks for strict ordering, so we'll get 1 of those here too
        expected.put(E036, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E037 - Sequential stop_time_updates have the same stop_id
     */
    @Test
    public void testE037() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1234");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_ids 1000, 2000 - no errors
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_sequence - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_id 2000 twice (and to make sure we support it, no stop_sequence). So, repeating stop_ids 3000 - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E037, 1);
        TestUtils.assertResults(expected, results);

        // stop_id 2000 twice again, but include stop_sequence for last stop_time_update - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E037, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E040 - stop_time_update doesn't contain stop_id or stop_sequence
     */
    @Test
    public void testE40() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1234");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // No stop_id or stop_sequence - 1 error
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E040, 1);
        TestUtils.assertResults(expected, results);

        // Add stop_id but no stop_sequence - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.clear();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_sequence but no stop_id - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        stopTimeUpdateBuilder.setStopSequence(1);
        tripUpdateBuilder.clear();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_sequence and stop_id - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.clear();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E041 - trip doesn't have any stop_time_updates
     */
    @Test
    public void testE41() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // No stop_time_updates - 1 error
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 0
        assertEquals(0, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E041, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update added - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // No stop_time_updates, but trip is CANCELED - 0 errors
        stopTimeUpdateBuilder.clear();
        tripUpdateBuilder.clearStopTimeUpdate();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 0
        assertEquals(0, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E042 - arrival or departure provided for NO_DATA stop_time_update
     */
    @Test
    public void testE42() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // One stop_time_update with schedule_relationship SCHEDULED and a departure - 0 errors
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with schedule_relationship SCHEDULED and an arrival - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with schedule_relationship NO_DATA and a departure - 1 error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E042, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update with schedule_relationship NO_DATA and an arrival - 1 error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E042, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E043 - stop_time_update doesn't have arrival or departure
     */
    @Test
    public void testE43() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // One stop_time_update without arrival or departure - 1 error
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E043, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update without arrival or departure, but schedule_relationship SKIPPED - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED);
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update without arrival or departure, but schedule_relationship NO_DATA - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA);
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with arrival - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with departure - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E044 - stop_time_update arrival/departure doesn't have delay or time
     */
    @Test
    public void testE44() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // One stop_time_update with arrival delay - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with arrival time - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with departure delay - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with departure time - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update without arrival time or delay - 1 error
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E044, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update without departure time or delay - 1 error
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E044, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update without arrival time or delay, but with SKIPPED schedule_relationship - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update without departure time or delay, but with SKIPPED schedule_relationship - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E045 - GTFS-rt stop_time_update stop_sequence and stop_id do not match GTFS
     */
    @Test
    public void testE45() {
        /**
         * bullrunner-gtfs.zip (bullRunnerGtfs) has the following in stop_times.txt:
         *
         * trip_id,arrival_time,departure_time,stop_id,stop_sequence
         * 1,07:00:00,07:00:00,222,1
         * 1,07:01:04,07:01:04,230,2
         * 1,07:01:38,07:01:38,214,3
         * 1,07:02:15,07:02:15,204,4
         * 1,07:02:56,07:02:56,102,5
         * 1,07:03:38,07:03:38,101,6
         * 1,07:04:04,07:04:04,108,7
         * 1,07:04:32,07:04:32,110,8
         * 1,07:05:38,07:05:38,166,9
         * 1,07:06:44,07:06:44,162,10
         * 1,07:07:48,07:07:48,158,11
         * 1,07:08:30,07:08:30,154,12
         * 1,07:09:20,07:09:20,150,13
         * 1,07:09:52,07:09:52,446,14
         * 1,07:11:01,07:11:01,432,15
         * 1,07:11:49,07:11:49,430,16
         * 1,07:12:34,07:12:34,426,17
         * 1,07:13:41,07:13:41,418,18
         * 1,07:14:34,07:14:34,401,19
         * 1,07:16:07,07:16:07,414,20
         * 1,07:16:53,07:16:53,330,21
         * 1,07:17:21,07:17:21,328,22
         * 1,07:17:59,07:17:59,326,23
         * 1,07:18:43,07:18:43,226,24
         * 1,07:19:43,07:19:43,222,25
         */
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_sequence and stop_id pairings all correctly match GTFS - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // first stop_sequence and stop_id pairing is wrong - 1 error
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("204");  // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E045, 1);
        TestUtils.assertResults(expected, results);

        // first two stop_sequence and stop_id pairings are wrong - 2 error
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("204");  // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("222"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E045, 2);
        TestUtils.assertResults(expected, results);

        // first and third stop_sequence and stop_id pairings are wrong - 2 errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("240"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("240"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E045, 2);
        TestUtils.assertResults(expected, results);

        // Third and fourth stop_sequence and stop_id pairings are wrong - 2 errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3); // Wrong
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("201"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E045, 2);
        TestUtils.assertResults(expected, results);

        // start at stop_sequence 2 - stop_sequence and stop_id pairings all correctly match GTFS - no errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // start at stop_sequence 2 - stop_sequence 10 is wrong - 1 error
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("160"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E045, 1);
        TestUtils.assertResults(expected, results);

        // start at stop_sequence 2 - stop_sequence 10 and 25 are wrong - 2 errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("160"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("101");  // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E045, 2);
        TestUtils.assertResults(expected, results);

        // start at stop_sequence 2 - no stop_ids - no errors
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clearStopId();

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // no stop_sequences - no errors for E45, but does include 1 E009 error for not including stop_sequence for loop route
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clearStopSequence();

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E009, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E046 - GTFS-rt stop_time_update without time doesn't have arrival/departure_time in GTFS
     */
    @Test
    public void testE46() {
        /**
         * bullrunner-gtfs-timepoints-only-legacy.zip (bullRunnerGtfsTimepointsOnlyLegacyExactTimes1) has the following in stop_times.txt:
         *
         * trip_id,arrival_time,departure_time,stop_id,stop_sequence
         * 1,7:00:00,7:00:00,222,1
         * 1,,,230,2
         * 1,,,214,3
         * 1,,,204,4
         * 1,,,102,5
         * 1,,,101,6
         * 1,7:04:04,7:04:04,108,7
         * 1,,,110,8
         * 1,,,166,9
         * 1,,,162,10
         * 1,,,158,11
         * 1,,,154,12
         * 1,7:09:20,7:09:20,150,13
         * 1,,,446,14
         * 1,,,432,15
         * 1,,,430,16
         * 1,,,426,17
         * 1,7:13:41,7:13:41,418,18
         * 1,,,401,19
         * 1,,,414,20
         * 1,,,330,21
         * 1,7:17:21,7:17:21,328,22
         * 1,,,326,23
         * 1,,,226,24
         * 1,7:19:43,7:19:43,222,25
         */
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // Times are provided for all stop_time_updates - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 1).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 1).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 2).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 2).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 3).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 3).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 4).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 4).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 5).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 5).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 6).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 6).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 7).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 7).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 8).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 8).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1Metadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Times and delays are provided for all stop_time_updates - no errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 1).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 1).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 2).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 2).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 3).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 3).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 4).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 4).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 5).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 5).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 6).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 6).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 7).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 7).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 8).setDelay(60).build());
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(TimestampUtils.MIN_POSIX_TIME + 8).setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1Metadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Delays are provided, but only for timepoints - no errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(7);
        stopTimeUpdateBuilder.setStopId("108");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(13);
        stopTimeUpdateBuilder.setStopId("150");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1Metadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Delays are provided, include for stop_sequence 3 and 10, which are not timepoints - 4 errors (2 for arrival and 2 for departure)
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(7);
        stopTimeUpdateBuilder.setStopId("108");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(13);
        stopTimeUpdateBuilder.setStopId("150");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1Metadata, feedMessageBuilder.build(), null, null);
        expected.put(E046, 4);
        TestUtils.assertResults(expected, results);

        // Delays are provided, include for stop_sequence 3 and 10, which are not timepoints, and only stop_id is provided - 4 E046 errors (2 for arrival and 2 for departure), and 1 E009 error for missing stop_sequence for loop trip
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clearStopSequence();

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("108");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("150");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1Metadata, feedMessageBuilder.build(), null, null);
        expected.put(E046, 4);
        expected.put(E009, 1);
        TestUtils.assertResults(expected, results);

        // Delays are provided, include for stop_sequence 3 and 10, which are not timepoints, and only stop_sequence is provided - 4 errors (2 for arrival and 2 for departure)
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clearStopId();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(7);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(13);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60));
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1, bullRunnerGtfsTimepointsOnlyLegacyExactTimes1Metadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E046, 4);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E051 - GTFS-rt stop_sequence not found in GTFS data
     */
    @Test
    public void testE051() {
        /**
         * bullrunner-gtfs.zip (bullRunnerGtfs) has the following in stop_times.txt:
         *
         * trip_id,arrival_time,departure_time,stop_id,stop_sequence
         * 1,07:00:00,07:00:00,222,1
         * 1,07:01:04,07:01:04,230,2
         * 1,07:01:38,07:01:38,214,3
         * 1,07:02:15,07:02:15,204,4
         * 1,07:02:56,07:02:56,102,5
         * 1,07:03:38,07:03:38,101,6
         * 1,07:04:04,07:04:04,108,7
         * 1,07:04:32,07:04:32,110,8
         * 1,07:05:38,07:05:38,166,9
         * 1,07:06:44,07:06:44,162,10
         * 1,07:07:48,07:07:48,158,11
         * 1,07:08:30,07:08:30,154,12
         * 1,07:09:20,07:09:20,150,13
         * 1,07:09:52,07:09:52,446,14
         * 1,07:11:01,07:11:01,432,15
         * 1,07:11:49,07:11:49,430,16
         * 1,07:12:34,07:12:34,426,17
         * 1,07:13:41,07:13:41,418,18
         * 1,07:14:34,07:14:34,401,19
         * 1,07:16:07,07:16:07,414,20
         * 1,07:16:53,07:16:53,330,21
         * 1,07:17:21,07:17:21,328,22
         * 1,07:17:59,07:17:59,326,23
         * 1,07:18:43,07:18:43,226,24
         * 1,07:19:43,07:19:43,222,25
         */
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_sequence all correctly match GTFS - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Last stop_sequence is wrong - 1 occurrence of E051
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(26); // Wrong stop_sequence (should be 25)
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E051, 1);
        TestUtils.assertResults(expected, results);

        // Both stop_sequence and stop_id are included, with the last stop_sequence being wrong - 1 occurrence of E051
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(26); // Wrong stop_sequence (should be 25)
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E051, 1);
        TestUtils.assertResults(expected, results);

        // Both stop_sequence and stop_id are included, with the wrong stop_sequence 0 at beginning of trip - 1 occurrence of E051
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();

        stopTimeUpdateBuilder.setStopSequence(0);  // Wrong stop_sequence (should be 1)
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E051, 1);
        TestUtils.assertResults(expected, results);

        // Both stop_sequence and stop_id are included, with the wrong stop_sequence 250 being added in the middle of trip - 1 occurrence of E051
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(250); // Wrong stop_sequence (should be 6)
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E051, 1);
        TestUtils.assertResults(expected, results);

        // Only stop_sequence is included, with the wrong stop_sequence 250 being added in the middle of trip - 1 occurrence of E051
        tripUpdateBuilder.clearStopTimeUpdate();
        stopTimeUpdateBuilder.clear();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(250); // Wrong stop_sequence (should be 6)
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        expected.put(E051, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }
}
