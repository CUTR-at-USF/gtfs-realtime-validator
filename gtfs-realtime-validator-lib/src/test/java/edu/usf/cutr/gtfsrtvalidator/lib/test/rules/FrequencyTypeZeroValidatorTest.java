package edu.usf.cutr.gtfsrtvalidator.lib.test.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.lib.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.lib.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.rules.FrequencyTypeZeroValidator;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests to evaluate rules for Frequency-based exact_times=0 trips
 * <p>
 * Tests:
 *
 * E006 - Missing required trip field for frequency-based exact_times = 0
 * E013 - Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty
 * W005 - Missing vehicle_id for frequency-based exact_times = 0
 *
 */
public class FrequencyTypeZeroValidatorTest extends FeedMessageTest {


    public FrequencyTypeZeroValidatorTest() throws IOException {
    }

    /**
     * E006 - Missing required trip field for frequency-based exact_times = 0
     */
    @Test
    public void testE006() {
        FrequencyTypeZeroValidator frequencyTypeZeroValidator = new FrequencyTypeZeroValidator();
        Map<ValidationRule, Integer> expectedErrorsWarnings = new HashMap<>();

        // Set valid trip_id
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");

        // Set valid vehicle_id so no warnings for these
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("vehicle_A");

        feedHeaderBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Start with no start date or time - 4 errors
        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expectedErrorsWarnings.put(ValidationRules.E006, 4);
        TestUtils.assertResults(expectedErrorsWarnings, results);

        // Set start date - 2 errors
        tripDescriptorBuilder.setStartDate("4-24-2016");

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expectedErrorsWarnings.put(ValidationRules.E006, 2);
        TestUtils.assertResults(expectedErrorsWarnings, results);

        // Set start time - 0 error
        tripDescriptorBuilder.setStartTime("08:00:00AM");

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No errors
        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expectedErrorsWarnings.clear();
        TestUtils.assertResults(expectedErrorsWarnings, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E013 - Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty
     */
    @Test
    public void testE013() {
        FrequencyTypeZeroValidator frequencyTypeZeroValidator = new FrequencyTypeZeroValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        // Set valid trip_id, start_date, and start_time so no errors/warnings for these attributes
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setStartDate("4-24-2016");
        tripDescriptorBuilder.setStartTime("08:00:00AM");
        tripDescriptorBuilder.clearScheduleRelationship();  // FIXME - This should result in no SCHEDULE_RELATIONSHIP, but it results in SCHEDULED (see first assertion below)

        // Set valid vehicle_id so no warnings for these
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("vehicle_A");

        feedHeaderBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Start with an empty schedule relationship - that should be fine for exact_times=0 trips - no errors
        // FIXME - For some reason it seems we can't clear the SCHEDULE_RELATIONSHIP, so right now we can't test for this scenario
        //results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        //TestUtils.assertResults(E013, results, 0);

        // Change to UNSCHEDULED schedule relationship - this is also ok for exact_times=0 trips
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Change to ADDED schedule relationship - not allowed for exact_times=0 trips - 2 errors
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(ValidationRules.E013, 2);
        TestUtils.assertResults(expected, results);

        // Change to CANCELED schedule relationship - not allowed for exact_times=0 trips - 2 errors
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(ValidationRules.E013, 2);
        TestUtils.assertResults(expected, results);

        // Change to SCHEDULED schedule relationship - not allowed for exact_times=0 trips - 2 errors
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(ValidationRules.E013, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W005 - Missing vehicle_id for frequency-based exact_times = 0
     */
    @Test
    public void testW005() {
        FrequencyTypeZeroValidator frequencyTypeZeroValidator = new FrequencyTypeZeroValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        // Set valid trip_id, start_date, and start_time so no errors/warnings for these attributes
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setStartDate("4-24-2016");
        tripDescriptorBuilder.setStartTime("08:00:00AM");
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();

        feedHeaderBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(TimestampUtils.MIN_POSIX_TIME);
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No vehicle Id in trip update or vehicle position - 2 warnings
        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(ValidationRules.W005, 2);
        TestUtils.assertResults(expected, results);

        // Add vehicle_id to vehicle position - 1 warning
        vehicleDescriptorBuilder.setId("1");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No vehicle Id in trip update - 1 warning
        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(ValidationRules.W005, 1);
        TestUtils.assertResults(expected, results);

        // Add vehicle_id to trip update - no warnings
        vehicleDescriptorBuilder.setId("1");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Both have vehicle_id - no warnings
        results = frequencyTypeZeroValidator.validate(TimestampUtils.MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }
}
