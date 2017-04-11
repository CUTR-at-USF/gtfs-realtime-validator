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
package edu.usf.cutr.gtfsrtvalidator.test.feeds.combined;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.LocationTypeReferenceValidator;
import org.junit.Test;

/*
 * Tests all the warnings and rules that validate TripUpdate, VehiclePosition and Alerts feed.
 * Tests: 0e11 - "All stop_ids referenced in GTFS-rt feeds must have the 'location_type' = 0"
*/
public class TripUpdateVehiclePostionAlertTest extends FeedMessageTest {
    
    public TripUpdateVehiclePostionAlertTest() throws Exception {}
    
    @Test
    public void testLocationTypeValidation() {
        
        LocationTypeReferenceValidator locationValidator = new LocationTypeReferenceValidator();
        
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();
        
        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        // setting stop id = "A" in all the three feeds that matches the stop id in static Gtfs data
        stopTimeUpdateBuilder.setStopId("A");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        vehiclePositionBuilder.setStopId("A");
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        entitySelectorBuilder.setStopId("A");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // all the feeds have valid stop id matching that in static Gtfs data. So, returns 0 results
        results = locationValidator.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : results) {
            assertEquals(0, error.getOccurrenceList().size());
        }
        
        // setting stop id = "DUMMY" in TripUpdate feed that does not match with any stop id in static Gtfs data
        stopTimeUpdateBuilder.setStopId("DUMMY");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(gtfsData, feedMessageBuilder.build());
        // one error from TripUpdate feed stop_id = "DUMMY". VehiclePosition and Alert feeds have valid stop id = "A"
        for (ErrorListHelperModel error : results) {
            assertEquals(1, error.getOccurrenceList().size());
        }
        
        vehiclePositionBuilder.setStopId("DUMMY");
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(gtfsData, feedMessageBuilder.build());
        // 2 results from TripUpdate and VehiclePosition feeds stop_id="DUMMY". Alert feed have valid stop id ="A"
        for (ErrorListHelperModel error : results) {
            assertEquals(2, error.getOccurrenceList().size());
        }
        
        entitySelectorBuilder.setStopId("DUMMY");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = locationValidator.validate(gtfsData, feedMessageBuilder.build());
        // 3 results from TripUpdate, VehiclePosition and alert feeds stop_id="DUMMY"
        for (ErrorListHelperModel error : results) {
            assertEquals(3, error.getOccurrenceList().size());
        }
        
        clearAndInitRequiredFeedFields();
    }    
}
