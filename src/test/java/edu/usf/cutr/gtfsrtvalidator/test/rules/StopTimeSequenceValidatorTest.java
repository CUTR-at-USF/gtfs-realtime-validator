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
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopTimeSequenceValidator;
import org.junit.Test;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static org.junit.Assert.assertEquals;

/**
 * Tests for rules implemented in StopTimeSequenceValidator
 */
public class StopTimeSequenceValidatorTest extends FeedMessageTest {

    public StopTimeSequenceValidatorTest() throws Exception {
    }

    /**
     * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
     */
    @Test
    public void testStopSequenceValidation() {
        StopTimeSequenceValidator stopSequenceValidator = new StopTimeSequenceValidator();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // ordered stop sequence 1, 5
        stopTimeUpdateBuilder.setStopSequence(1);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E002, results, 0);

        /* Adding stop sequence 3. So, the stop sequence now is 1, 5, 3 which is unordered.
           So, the validation fails and the assertion test passes
        */
        stopTimeUpdateBuilder.setStopSequence(3);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E002, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E036 - Sequential stop_time_updates have the same stop_sequence
     */
    @Test
    public void testRepeatingStopSequence() {
        StopTimeSequenceValidator stopSequenceValidator = new StopTimeSequenceValidator();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1234");

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_sequences 1, 5 - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E036, results, 0);

        // Add stop_ids - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E036, results, 0);

        // Add stop sequence 5 twice (and to make sure we support it, no stopId). So, the stop sequence now is 1, 5, 5 - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(5);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E036, results, 1);

        // stop_sequence 5 twice again, but include stop_id for last stop_time_update - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("3000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E036, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E037 - Sequential stop_time_updates have the same stop_id
     */
    @Test
    public void testRepeatingStopId() {
        StopTimeSequenceValidator stopSequenceValidator = new StopTimeSequenceValidator();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1234");

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_ids 1000, 2000 - no errors
        stopTimeUpdateBuilder.setStopId("1000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E037, results, 0);

        // Add stop_sequence - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E037, results, 0);

        // Add stop_id 2000 twice (and to make sure we support it, no stop_sequence). So, repeating stop_ids 3000 - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E037, results, 1);

        // stop_id 2000 twice again, but include stop_sequence for last stop_time_update - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("2000");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E037, results, 1);

        clearAndInitRequiredFeedFields();
    }
}
