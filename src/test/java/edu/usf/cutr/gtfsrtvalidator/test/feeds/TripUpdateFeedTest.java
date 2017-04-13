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
package edu.usf.cutr.gtfsrtvalidator.test.feeds;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopTimeSequenceValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.VehicleIdValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.gtfs.StopLocationTypeValidator;
import org.junit.Test;

/* 
 * Tests all the warnings and rules that validate TripUpdate feed.
 * Tests: w002 - "vehicle_id should be populated in trip_update"
 *        e002 - "stop_time_updates for a given trip_id must be sorted by increasing stop_sequence"
 *        e010 - "If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0"
*/
public class TripUpdateFeedTest extends FeedMessageTest {
    
    public TripUpdateFeedTest() throws Exception {}

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

        results = stopSequenceValidator.validate(gtfsData, feedMessageBuilder.build());
        assertNull(results);
        
        /* Adding stop sequence 3. So, the stop sequence now is 1, 5, 3 which is unordered.
           So, the validation fails and the assertion test passes
        */
        stopTimeUpdateBuilder.setStopSequence(3);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : results) {
            assertEquals(1, error.getOccurrenceList().size());
        }
        
        clearAndInitRequiredFeedFields();
    }
    
    @Test
    public void testVehicleIdValidation() {
        VehicleIdValidator vehicleIdValidator = new VehicleIdValidator();
        
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        
        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        // setting a value for vehicle id = 1
        vehicleDescriptorBuilder.setId("1");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // No results, if vehicle id has a value.
        results = vehicleIdValidator.validate(gtfsData, feedMessageBuilder.build());
        assertNull(results);

        // Test with empty string for Vehicle ID, which should generate warning
        vehicleDescriptorBuilder.setId("");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleIdValidator.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : results) {
            assertEquals(1, error.getOccurrenceList().size());
        }
        
        clearAndInitRequiredFeedFields();
    }

    @Test
    public void testLocationTypeValidation() {
        StopLocationTypeValidator stopLocationValidator = new StopLocationTypeValidator();

        // gtfsData does not contain location_type = 1 for stop_id. Therefore returns 0 results
        results = stopLocationValidator.validate(gtfsData);
        for (ErrorListHelperModel error : results) {
            assertEquals(0, error.getOccurrenceList().size());
        }
        
        // gtfsData2 contains location_type = 1 for stop_ids. Therefore returns errorcount = (number of location_type = 1 for stop_ids)
        results = stopLocationValidator.validate(gtfsData2);
        for (ErrorListHelperModel error : results) {
            assertTrue(error.getOccurrenceList().size() >= 1);
        }
        
        clearAndInitRequiredFeedFields();
    }
}
