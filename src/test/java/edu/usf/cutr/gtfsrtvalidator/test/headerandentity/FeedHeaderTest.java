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
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.TimestampValidation;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E012;

/*
 * Tests all the warnings and rules that validate FeedHeader.
 * Tests:
 *  * w001 - "Timestamps should be populated for all elements"
 *  * e012 - "Header timestamp should be greater than or equal to all other timestamps"
 *
*/
public class FeedHeaderTest extends FeedMessageTest {

    public final static long POSIX_TIME_PASS_VALIDATION = new Date().getTime() / 1000; // current date in seconds
    public final static long POSIX_TIME_FAIL_VALIDATION = new Date().getTime();

    public FeedHeaderTest() throws Exception {
    }

    @Test
    public void testTimestampValidationW001() {
        TimestampValidation timestampValidation = new TimestampValidation();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // Timestamp will be zero initially in FeedHeader, TripUpdate and VehiclePosition. Should return 3 error.
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : errors) {
            assertEquals(3, error.getOccurrenceList().size());
        }

        // Populate timestamp to any value greater than zero in FeedHeader
        feedHeaderBuilder.setTimestamp(12345);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());
        // Invalid timestamp in TripUpdate and VehiclePosition. Should return 2 errors.
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : errors) {
            assertEquals(2, error.getOccurrenceList().size());
        }

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
        for (ErrorListHelperModel error : errors) {
            assertEquals(1, error.getOccurrenceList().size());
        }

        vehiclePositionBuilder.setTimestamp(12345);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // Now timestamp is populated in FeedHeader, TripUpdate and VehiclePosition . Should return no error.
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : errors) {
            assertEquals(0, error.getOccurrenceList().size());
        }

        clearAndInitRequiredFeedFields();
    }

    @Test
    public void testTimestampValidationE012() {
        TimestampValidation timestampValidation = new TimestampValidation();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        /**
         * Header timestamp greater than other entities - no error
         */
        feedHeaderBuilder.setTimestamp(5);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(4);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(4);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : errors) {
            assertEquals(0, error.getOccurrenceList().size());
        }

        /**
         * Header timestamp equal to other entities - no error
         */
        tripUpdateBuilder.setTimestamp(5);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(5);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        for (ErrorListHelperModel error : errors) {
            assertEquals(0, error.getOccurrenceList().size());
        }

        /**
         * Header timestamp less than VehiclePosition timestamp - 1 error
         */
        tripUpdateBuilder.setTimestamp(5);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(6);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Feed header timestamp is less than VehiclePosition - we should see one error of type E012
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        _assertE012Errors(errors, 1);

        /**
         * Header timestamp less than TripUpdate timestamp - 1 error
         */
        tripUpdateBuilder.setTimestamp(6);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(5);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Feed header timestamp is less than TripUpdate - we should see one error of type E012
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        _assertE012Errors(errors, 1);

        /**
         * Header timestamp less than TripUpdate and VehiclePosition timestamps - 2 errors
         */
        tripUpdateBuilder.setTimestamp(6);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(6);
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Feed header timestamp is less than VehiclePosition and TripUpdate - we should see two errors of type E012
        errors = timestampValidation.validate(gtfsData, feedMessageBuilder.build());
        _assertE012Errors(errors, 2);

        clearAndInitRequiredFeedFields();
    }

    /**
     * Asserts that errors is found due to an entity having a timestamp greater than the header
     *
     * @param errors              list of errors output from validation
     * @param totalExpectedErrors total number of expected errors
     */
    private void _assertE012Errors(List<ErrorListHelperModel> errors, int totalExpectedErrors) {
        for (ErrorListHelperModel error : errors) {
            if (error.getErrorMessage().getValidationRule().getErrorId().equals(E012.getErrorId())) {
                assertEquals(totalExpectedErrors, error.getOccurrenceList().size());
            } else {
                assertEquals(0, error.getOccurrenceList().size());
            }
        }
    }
}
