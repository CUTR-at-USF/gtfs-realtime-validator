package edu.usf.cutr.gtfsrtvalidator.test.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.rules.FrequencyTypeZeroValidator;
import org.junit.Test;

import java.io.IOException;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

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

    @Test
    public void testMissingStartDateAndTimeE006() {
        FrequencyTypeZeroValidator frequencyTypeZeroValidator = new FrequencyTypeZeroValidator();
        // Set valid trip_id
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");

        // Set valid vehicle_id so no warnings for these
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("vehicle_A");

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Start with no start date or time - 4 errors
        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(E006, results, 4);

        // Set start date - 2 errors
        tripDescriptorBuilder.setStartDate("4-24-2016");

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(E006, results, 2);

        // Set start time - 0 error
        tripDescriptorBuilder.setStartTime("08:00:00AM");

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No errors
        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(E006, results, 0);

        clearAndInitRequiredFeedFields();
    }

    @Test
    public void testScheduleRelationshipE013() {
        FrequencyTypeZeroValidator frequencyTypeZeroValidator = new FrequencyTypeZeroValidator();
        // Set valid trip_id, start_date, and start_time so no errors/warnings for these attributes
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setStartDate("4-24-2016");
        tripDescriptorBuilder.setStartTime("08:00:00AM");
        tripDescriptorBuilder.clearScheduleRelationship();  // FIXME - This should result in no SCHEDULE_RELATIONSHIP, but it results in SCHEDULED (see first assertion below)

        // Set valid vehicle_id so no warnings for these
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("vehicle_A");

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
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

        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(E013, results, 0);

        // Change to ADDED schedule relationship - not allowed for exact_times=0 trips - 2 errors
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(E013, results, 2);

        // Change to CANCELED schedule relationship - not allowed for exact_times=0 trips - 2 errors
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(E013, results, 2);

        // Change to SCHEDULED schedule relationship - not allowed for exact_times=0 trips - 2 errors
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(E013, results, 2);

        clearAndInitRequiredFeedFields();
    }

    @Test
    public void testMissingVehicleIdW005() {
        FrequencyTypeZeroValidator frequencyTypeZeroValidator = new FrequencyTypeZeroValidator();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        // Set valid trip_id, start_date, and start_time so no errors/warnings for these attributes
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setStartDate("4-24-2016");
        tripDescriptorBuilder.setStartTime("08:00:00AM");
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No vehicle Id in trip update or vehicle position - 2 warnings
        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(W005, results, 2);

        // Add vehicle_id to vehicle position - 1 warning
        vehicleDescriptorBuilder.setId("1");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No vehicle Id in trip update - 1 warning
        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(W005, results, 1);

        // Add vehicle_id to trip update - no warnings
        vehicleDescriptorBuilder.setId("1");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Both have vehicle_id - no warnings
        results = frequencyTypeZeroValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(W005, results, 0);

        clearAndInitRequiredFeedFields();
    }
}
