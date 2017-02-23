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
import edu.usf.cutr.gtfsrtvalidator.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.TimestampValidation;
import java.util.Date;
import org.junit.Test;
import static junit.framework.TestCase.assertEquals;

/*
 * Tests all the warnings and rules that validate FeedHeader.
 * Tests: w001 - "Timestamps should be populated for all elements"
*/
public class FeedHeaderTest extends FeedMessageTest {
    
    public final static long POSIX_TIME_PASS_VALIDATION = new Date().getTime()/1000; // current date in seconds
    public final static long POSIX_TIME_FAIL_VALIDATION = new Date().getTime();
    
    public FeedHeaderTest() throws Exception {}
    
    @Test
    public void testTimestampValidation() {
        TimestampValidation timestampValidation = new TimestampValidation();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        
        // Timestamp will be zero initially in FeedHeader, TripUpdate and VehiclePosition. Should return 3 error.
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        assertEquals(3, errors.getOccurrenceList().size());
        
        // Populate timestamp to any value greater than zero in FeedHeader
        feedHeaderBuilder.setTimestamp(12345);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());
        // Invalid timestamp in TripUpdate and VehiclePosition. Should return 2 errors.
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        assertEquals(2, errors.getOccurrenceList().size());
        
        // TripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Populate timestamp to any value greater than zero in TripUpdate.
        tripUpdateBuilder.setTimestamp(12345);
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Invalid timestamp only in VehiclePosition. Should return 1 errors.
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        assertEquals(1, errors.getOccurrenceList().size());
        
        vehiclePositionBuilder.setTimestamp(12345);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Now timestamp is populated in FeedHeader, TripUpdate and VehiclePosition . Should return no error.
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        assertEquals(0, errors.getOccurrenceList().size());
        
        clearAndInitRequiredFeedFields();
    }
}
