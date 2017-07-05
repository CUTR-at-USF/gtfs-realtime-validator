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
package edu.usf.cutr.gtfsrtvalidator.test;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import org.junit.Test;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeFactory;

import java.util.*;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.locationtech.spatial4j.context.SpatialContext.GEO;

/**
 * Test utility methods
 */
public class UtilTest {

    /**
     * Make sure our utility method TestUtils.assertResults() properly asserts number of expected==actual
     * rule occurrences
     */
    @Test
    public void testAssertResults() {
        MessageLogModel modelE001 = new MessageLogModel(ValidationRules.E001);
        OccurrenceModel errorE001 = new OccurrenceModel(String.valueOf(MIN_POSIX_TIME));
        List<OccurrenceModel> errorListE001 = new ArrayList<>();

        List<ErrorListHelperModel> results = new ArrayList<>();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        // Test empty list of error results and empty hashmap
        TestUtils.assertResults(expected, results);

        // Test list of error results, but without a MessageLogModel
        results.add(new ErrorListHelperModel(modelE001, errorListE001));
        TestUtils.assertResults(expected, results);

        // Test list of error results, with one MessageLogModel
        errorListE001.add(errorE001);
        expected.put(ValidationRules.E001, 1);
        TestUtils.assertResults(expected, results);

        // Test list of error results, with two MessageLogModels
        errorListE001.add(errorE001);
        expected.put(ValidationRules.E001, 2);
        TestUtils.assertResults(expected, results);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssertResultsThrowExceptionNullExpected() {
        // Make sure we throw an exception if the expected map is null
        TestUtils.assertResults(null, new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssertResultsThrowExceptionNullResults() {
        // Make sure we throw an exception if the results list is null
        TestUtils.assertResults(new HashMap<>(), null);
    }

    @Test(expected = AssertionError.class)
    public void testAssertResultsThrowExceptionMoreExpected() {
        // Make sure we fail if we have expected occurrences that aren't included in results
        List<ErrorListHelperModel> results = new ArrayList<>();
        MessageLogModel modelE001 = new MessageLogModel(ValidationRules.E001);
        OccurrenceModel errorE001 = new OccurrenceModel(String.valueOf(MIN_POSIX_TIME));
        List<OccurrenceModel> errorListE001 = new ArrayList<>();
        errorListE001.add(errorE001);
        results.add(new ErrorListHelperModel(modelE001, errorListE001));

        Map<ValidationRule, Integer> expected = new HashMap<>();
        expected.put(ValidationRules.E001, 1);

        // We're expecting 1 error for E001 and 1 error for E002, but get one actual error for E001 - this should throw an AssertionError
        expected.put(ValidationRules.E002, 1);
        TestUtils.assertResults(expected, results);
    }

    @Test(expected = AssertionError.class)
    public void testAssertResultsThrowExceptionMoreActual() {
        // Make sure we fail if we have actual results that weren't expected
        List<ErrorListHelperModel> results = new ArrayList<>();
        MessageLogModel modelE001 = new MessageLogModel(ValidationRules.E001);
        OccurrenceModel errorE001 = new OccurrenceModel(String.valueOf(MIN_POSIX_TIME));
        List<OccurrenceModel> errorListE001 = new ArrayList<>();
        errorListE001.add(errorE001);
        results.add(new ErrorListHelperModel(modelE001, errorListE001));

        Map<ValidationRule, Integer> expected = new HashMap<>();
        // No expected results included for E001, but there is one actual error for E001 - this should throw an AssertionError
        TestUtils.assertResults(expected, results);
    }

    @Test(expected = AssertionError.class)
    public void testAssertResultsThrowExceptionMismatchActualExpected() {
        // Make sure we fail if we have actual results that don't match the expected results
        List<ErrorListHelperModel> results = new ArrayList<>();
        MessageLogModel modelE001 = new MessageLogModel(ValidationRules.E001);
        OccurrenceModel errorE001 = new OccurrenceModel(String.valueOf(MIN_POSIX_TIME));
        List<OccurrenceModel> errorListE001 = new ArrayList<>();
        errorListE001.add(errorE001);
        results.add(new ErrorListHelperModel(modelE001, errorListE001));

        Map<ValidationRule, Integer> expected = new HashMap<>();
        expected.put(ValidationRules.E002, 1);
        // We are expecting error for E002, but get one for E001 - this should throw an AssertionError
        TestUtils.assertResults(expected, results);
    }

    @Test
    public void testGetAge() {
        long currentTimeMillis = 1104537600000L;
        long headerTimestampSec = 1104527600L;

        long age = TimestampUtils.getAge(currentTimeMillis, headerTimestampSec);
        assertEquals(10000000L, age);
    }

    @Test
    public void testDateFormat() {
        /**
         * Good dates
         */
        String validDate = "20170101";
        assertEquals(true, TimestampUtils.isValidDateFormat(validDate));

        validDate = "20170427";
        assertEquals(true, TimestampUtils.isValidDateFormat(validDate));

        /**
         * Bad dates
         */
        String badDate = "2017011";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "2017/01/01";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "01/01/2017";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "01-01-2017";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "01012017";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "13012017";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "20171301";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "abcdefgh";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "12345678";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "2017.01.01";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));
    }

    @Test
    public void testTimeFormat() {
        /**
         * Good times
         */
        String validTime = "00:00:00";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        validTime = "02:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        validTime = "22:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        // Time can exceed 24 hrs if service goes into the next service day
        validTime = "25:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        // Time can exceed 24 hrs if service goes into the next service day
        validTime = "29:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        /**
         * Bad times
         */
        String badTime = "5:15:35";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        // Anything of 29hrs will currently fail validation
        badTime = "30:15:35";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        badTime = "12345678";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        badTime = "abcdefgh";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        badTime = "05:5:35";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        badTime = "05:05:5";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));
    }

    @Test
    public void testSecondsAfterMidnightToClock() {
        int time;
        String clockTime;

        time = 59;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("00:00:59", clockTime);

        time = 1200;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("00:20:00", clockTime);

        time = 1250;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("00:20:50", clockTime);

        time = 21600;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("06:00:00", clockTime);

        time = 21901;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("06:05:01", clockTime);

        time = 86399;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("23:59:59", clockTime);

    }

    @Test
    public void testPosixToClock() {
        int time = 1493383886;  // POSIX time
        String timeZoneText = "America/New_York";
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneText);
        String clockTime = TimestampUtils.posixToClock(time, timeZone);
        assertEquals("08:51:26", clockTime);
    }

    @Test
    public void testGetVehicleAndTripId() {
        String text;

        GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1"));
        text = GtfsUtils.getVehicleAndTripIdText(tripUpdateBuilder.build());
        assertEquals(text, "trip_id 1");

        GtfsRealtime.VehiclePosition.Builder vehiclePositionBuilder = GtfsRealtime.VehiclePosition.newBuilder();
        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("A"));
        vehiclePositionBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1"));
        text = GtfsUtils.getVehicleAndTripIdText(vehiclePositionBuilder.build());
        assertEquals(text, "vehicle_id A trip_id 1");
    }

    @Test
    public void testGetVehicleAndRouteId() {
        String text;

        GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("1"));
        text = GtfsUtils.getVehicleAndRouteId(tripUpdateBuilder.build());
        assertEquals(text, "route_id 1");

        GtfsRealtime.VehiclePosition.Builder vehiclePositionBuilder = GtfsRealtime.VehiclePosition.newBuilder();
        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("A"));
        vehiclePositionBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("1"));
        text = GtfsUtils.getVehicleAndRouteId(vehiclePositionBuilder.build());
        assertEquals(text, "vehicle_id A route_id 1");
    }

    @Test
    public void testGetTripIdText() {
        GtfsRealtime.FeedEntity.Builder feedEntityBuilder = GtfsRealtime.FeedEntity.newBuilder();
        feedEntityBuilder.setId("1");
        GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // No trip_id - should get entity ID description back
        tripUpdateBuilder.setTrip(tripBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        String id = GtfsUtils.getTripId(feedEntityBuilder.build(), tripUpdateBuilder.build());
        assertEquals("entity ID 1", id);

        // Add trip_id and test with trip_update - should get trip ID description back
        tripBuilder.setTripId("20");
        tripUpdateBuilder.setTrip(tripBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        id = GtfsUtils.getTripId(feedEntityBuilder.build(), tripUpdateBuilder.build());
        assertEquals("trip_id 20", id);

        // Test with trip directly
        id = GtfsUtils.getTripId(feedEntityBuilder.build(), tripBuilder.build());
        assertEquals("trip_id 20", id);
    }

    @Test
    public void testGetStopTimeUpdateId() {
        String id;
        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();

        stopTimeUpdateBuilder.setStopId("1000");
        id = GtfsUtils.getStopTimeUpdateId(stopTimeUpdateBuilder.build());
        assertEquals("stop_id 1000", id);

        stopTimeUpdateBuilder.setStopSequence(5);
        id = GtfsUtils.getStopTimeUpdateId(stopTimeUpdateBuilder.build());
        assertEquals("stop_sequence 5", id);
    }

    @Test
    public void testGetVehicleId() {
        GtfsRealtime.FeedEntity.Builder feedEntityBuilder = GtfsRealtime.FeedEntity.newBuilder();
        feedEntityBuilder.setId("1");
        GtfsRealtime.VehiclePosition.Builder vehiclePositionBuilder = GtfsRealtime.VehiclePosition.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();

        // No vehicle ID - should get entity ID description back
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        String id = GtfsUtils.getVehicleId(feedEntityBuilder.build(), vehiclePositionBuilder.build());
        assertEquals("entity ID 1", id);

        // Add vehicle ID and test with vehicle - should get vehicle ID description back
        vehicleDescriptorBuilder.setId("20");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        id = GtfsUtils.getVehicleId(feedEntityBuilder.build(), vehiclePositionBuilder.build());
        assertEquals("vehicle.id 20", id);

        // Test with vehicle directly
        id = GtfsUtils.getVehicleId(feedEntityBuilder.build(), vehicleDescriptorBuilder.build());
        assertEquals("vehicle.id 20", id);
    }

    @Test
    public void testValidPosition() {
        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();

        /**
         * Valid lat/longs
         */
        positionBuilder.setLatitude(0);
        positionBuilder.setLongitude(0);

        assertEquals(true, GtfsUtils.isPositionValid(positionBuilder.build()));

        positionBuilder.setLatitude(-90);
        positionBuilder.setLongitude(-180);

        assertEquals(true, GtfsUtils.isPositionValid(positionBuilder.build()));

        positionBuilder.setLatitude(90);
        positionBuilder.setLongitude(180);

        assertEquals(true, GtfsUtils.isPositionValid(positionBuilder.build()));

        /**
         * Bad lat or long
         */
        positionBuilder.setLatitude(-91);
        positionBuilder.setLongitude(0);

        assertEquals(false, GtfsUtils.isPositionValid(positionBuilder.build()));

        positionBuilder.setLatitude(0);
        positionBuilder.setLongitude(-181);

        assertEquals(false, GtfsUtils.isPositionValid(positionBuilder.build()));

        positionBuilder.setLatitude(91);
        positionBuilder.setLongitude(0);

        assertEquals(false, GtfsUtils.isPositionValid(positionBuilder.build()));

        positionBuilder.setLatitude(0);
        positionBuilder.setLongitude(181);

        assertEquals(false, GtfsUtils.isPositionValid(positionBuilder.build()));
    }

    @Test
    public void testValidBearing() {
        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();
        positionBuilder.setLatitude(0);
        positionBuilder.setLongitude(0);

        /**
         * Valid bearing
         */
        positionBuilder.setBearing(0);

        assertEquals(true, GtfsUtils.isBearingValid(positionBuilder.build()));

        positionBuilder.setBearing(360);

        assertEquals(true, GtfsUtils.isBearingValid(positionBuilder.build()));

        /**
         * Bad bearing
         */
        positionBuilder.setBearing(-1);

        assertEquals(false, GtfsUtils.isBearingValid(positionBuilder.build()));

        positionBuilder.setBearing(361);

        assertEquals(false, GtfsUtils.isBearingValid(positionBuilder.build()));
    }

    @Test
    public void testPositionWithinShape() {
        // Create USF Bull Runner bounding box
        ShapeFactory sf = GEO.getShapeFactory();
        ShapeFactory.MultiPointBuilder shapeBuilder = sf.multiPoint();
        shapeBuilder.pointXY(-82.438456, 28.041606);
        shapeBuilder.pointXY(-82.438456, 28.082202);
        shapeBuilder.pointXY(-82.399531, 28.082202);
        shapeBuilder.pointXY(-82.399531, 28.041606);
        Shape boundingBox = shapeBuilder.build().getBoundingBox();

        // Test utility method - USF campus location
        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();
        positionBuilder.setLatitude(28.0587f);
        positionBuilder.setLongitude(-82.4139f);
        boolean result = GtfsUtils.isPositionWithinShape(positionBuilder.build(), boundingBox);
        assertTrue(result);

        // Test utility method -  Downtown Tampa, FL
        positionBuilder.setLatitude(27.9482837f);
        positionBuilder.setLongitude(-82.4655826f);
        result = GtfsUtils.isPositionWithinShape(positionBuilder.build(), boundingBox);
        assertFalse(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssertVehicleAndTripIdThrowException() {
        // Make sure we throw an exception if the method is provided objects other than TripUpdate or VehiclePosition
        GtfsUtils.getVehicleAndTripIdText(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssertVehicleAndRouteIdThrowException() {
        // Make sure we throw an exception if the method is provided objects other than TripUpdate or VehiclePosition
        GtfsUtils.getVehicleAndRouteId(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("1").build());
    }
}
