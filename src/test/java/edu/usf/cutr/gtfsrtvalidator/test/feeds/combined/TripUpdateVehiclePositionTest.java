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
import edu.usf.cutr.gtfsrtvalidator.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.VehicleTripDescriptorValidator;
import org.junit.Test;

/* 
 * Tests all the warnings and rules that validate both TripUpdate and VehiclePositions feed.
 * Tests: w003 - "If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds"
*/
public class TripUpdateVehiclePositionTest extends FeedMessageTest {
    
    public TripUpdateVehiclePositionTest() throws Exception {}
    
    @Test
    public void testTripAndVehicleDescriptorValidation() {
        VehicleTripDescriptorValidator vehicleAndTripDescriptorValidator = new VehicleTripDescriptorValidator();
        
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        
        tripDescriptorBuilder.setTripId("1.1");
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // setting the same trip_id = 1.1 in VehiclePosition too
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        vehicleDescriptorBuilder.setId("1");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // setting same vehicle id = 1 in VehiclePosition too 
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        // TripUpdate and VehiclePosition feed have same trip id = 1.1 and same vehicle id = 1. So, no errors.
        errors = vehicleAndTripDescriptorValidator.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : errors) {
            assertEquals(0, error.getOccurrenceList().size());
        }
        
        /* If trip_id's and vehicle_id's in TripUpdate and VehiclePosition are not equal, validator should return an error.
           That is, it fails the validation and passes the assertion.
        */        
        // set trip id = 100 in VehiclePosition i.e., not equal to trip id = 1.1 in TripUpdate
        tripDescriptorBuilder.setTripId("100");
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        // set vehicle id = 44 in VehiclePosition i.e., not equal to vehicle id = "1" in TripUpdate
        vehicleDescriptorBuilder.setId("44");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // 2 errors. Unmatched trip id's and vechicle id's in TripUpdate and VehiclePosition feeds
        errors = vehicleAndTripDescriptorValidator.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : errors) {
            assertEquals(2, error.getOccurrenceList().size());
        }
        
        clearAndInitRequiredFeedFields();
    }
}
