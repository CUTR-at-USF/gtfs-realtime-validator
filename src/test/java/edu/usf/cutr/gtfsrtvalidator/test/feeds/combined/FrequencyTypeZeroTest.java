package edu.usf.cutr.gtfsrtvalidator.test.feeds.combined;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.FrequencyTypeZero;
import org.junit.Test;

import java.io.IOException;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E013;

/**
 * Tests to evaluate rules for Frequency-based exact_times=0 trips
 * <p>
 * Tests: E013 - "Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty"
 */
public class FrequencyTypeZeroTest extends FeedMessageTest {


    public FrequencyTypeZeroTest() throws IOException {
    }

    @Test
    public void testScheduleRelationshipE013() {
        FrequencyTypeZero frequencyTypeZero = new FrequencyTypeZero();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        feedHeaderBuilder.setTimestamp(MIN_POSIX_TIME);
        feedMessageBuilder.setHeader(feedHeaderBuilder.build());

        tripUpdateBuilder.setTimestamp(MIN_POSIX_TIME);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTimestamp(MIN_POSIX_TIME);
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // Start with an empty schedule relationship - that should be fine for exact_times=0 trips - no errors
        results = frequencyTypeZero.validate(bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build());
        TestUtils.assertResults(E013, results, 0);

        // Change to UNSCHEDULED schedule relationship - this is also ok for exact_times=0 trips
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZero.validate(bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build());
        TestUtils.assertResults(E013, results, 0);

        // Change to ADDED schedule relationship - not allowed for exact_times=0 trips - 1 error
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZero.validate(bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build());
        TestUtils.assertResults(E013, results, 1);

        // Change to CANCELED schedule relationship - not allowed for exact_times=0 trips - 1 error
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZero.validate(bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build());
        TestUtils.assertResults(E013, results, 1);

        // Change to SCHEDULED schedule relationship - not allowed for exact_times=0 trips - 1 error
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder);

        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());

        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = frequencyTypeZero.validate(bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build());
        TestUtils.assertResults(E013, results, 1);
    }
}
