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
package edu.usf.cutr.gtfsrtvalidator.test.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.rules.StopTimeUpdateValidator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;
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

        // ordered stop sequence 1, 5
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /* Adding stop sequence 3. So, the stop sequence now is 1, 5, 3 which is unordered.
           So, the validation fails and the assertion test passes
        */
        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E002, 1);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }
}
