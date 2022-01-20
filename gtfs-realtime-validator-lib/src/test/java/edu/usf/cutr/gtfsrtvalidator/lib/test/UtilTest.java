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
package edu.usf.cutr.gtfsrtvalidator.lib.test;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.SortUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules;
import junit.framework.TestCase;
import org.junit.Test;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.E001;
import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.E002;
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
        MessageLogModel modelE001 = new MessageLogModel(E001);
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
        expected.put(E001, 1);
        TestUtils.assertResults(expected, results);

        // Test list of error results, with two MessageLogModels
        errorListE001.add(errorE001);
        expected.put(E001, 2);
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
        MessageLogModel modelE001 = new MessageLogModel(E001);
        OccurrenceModel errorE001 = new OccurrenceModel(String.valueOf(MIN_POSIX_TIME));
        List<OccurrenceModel> errorListE001 = new ArrayList<>();
        errorListE001.add(errorE001);
        results.add(new ErrorListHelperModel(modelE001, errorListE001));

        Map<ValidationRule, Integer> expected = new HashMap<>();
        expected.put(E001, 1);

        // We're expecting 1 error for E001 and 1 error for E002, but get one actual error for E001 - this should throw an AssertionError
        expected.put(E002, 1);
        TestUtils.assertResults(expected, results);
    }

    @Test(expected = AssertionError.class)
    public void testAssertResultsThrowExceptionMoreActual() {
        // Make sure we fail if we have actual results that weren't expected
        List<ErrorListHelperModel> results = new ArrayList<>();
        MessageLogModel modelE001 = new MessageLogModel(E001);
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
        MessageLogModel modelE001 = new MessageLogModel(E001);
        OccurrenceModel errorE001 = new OccurrenceModel(String.valueOf(MIN_POSIX_TIME));
        List<OccurrenceModel> errorListE001 = new ArrayList<>();
        errorListE001.add(errorE001);
        results.add(new ErrorListHelperModel(modelE001, errorListE001));

        Map<ValidationRule, Integer> expected = new HashMap<>();
        expected.put(E002, 1);
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

        // H:MM:SS is ok
        validTime = "5:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        /**
         * Bad times
         */
        // Anything of 29hrs will currently fail validation
        String badTime = "30:15:35";
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
    public void testGetElapsedTime() {
        long startTime = 1502457651566L, endTime, delta;
        double elapsedTime;

        // 60 seconds
        delta = TimeUnit.SECONDS.toNanos(60);
        endTime = startTime + delta;

        elapsedTime = TimestampUtils.getElapsedTime(startTime, endTime);
        assertEquals(60, elapsedTime, 0.00000001d);

        // 60.5 seconds
        delta = TimeUnit.MILLISECONDS.toNanos(6500);
        endTime = startTime + delta;

        elapsedTime = TimestampUtils.getElapsedTime(startTime, endTime);
        assertEquals(6.5d, elapsedTime, 0.00000001d);

        // 0.5 seconds
        delta = TimeUnit.MILLISECONDS.toNanos(500);
        endTime = startTime + delta;

        elapsedTime = TimestampUtils.getElapsedTime(startTime, endTime);
        assertEquals(0.5d, elapsedTime, 0.00000001d);
    }

    @Test
    public void testGetElapsedTimeString() {
        double elapsedTime;

        elapsedTime = 2.5;
        assertEquals("2.5 seconds", TimestampUtils.getElapsedTimeString(elapsedTime));

        elapsedTime = 3.0;
        assertEquals("3.0 seconds", TimestampUtils.getElapsedTimeString(elapsedTime));

        elapsedTime = 3.25;
        assertEquals("3.25 seconds", TimestampUtils.getElapsedTimeString(elapsedTime));

        elapsedTime = 3.25123412;
        assertEquals("3.251 seconds", TimestampUtils.getElapsedTimeString(elapsedTime));

        elapsedTime = 3.25163412;
        assertEquals("3.252 seconds", TimestampUtils.getElapsedTimeString(elapsedTime));
    }

    @Test
    public void testIsV2orHigher() {
        GtfsRealtime.FeedHeader.Builder builder = GtfsRealtime.FeedHeader.newBuilder();

        builder.setGtfsRealtimeVersion("1.0");
        TestCase.assertFalse(GtfsUtils.isV2orHigher(builder.build()));

        builder.setGtfsRealtimeVersion("2.0");
        assertTrue(GtfsUtils.isV2orHigher(builder.build()));

        builder.setGtfsRealtimeVersion("3.0");
        assertTrue(GtfsUtils.isV2orHigher(builder.build()));
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

    @Test
    public void testIsCombinedFeed() {
        GtfsRealtime.FeedMessage.Builder feedMessageBuilder = GtfsRealtime.FeedMessage.newBuilder();
        GtfsRealtime.FeedHeader.Builder feedHeaderBuilder = GtfsRealtime.FeedHeader.newBuilder();
        feedHeaderBuilder.setGtfsRealtimeVersion("1");
        feedMessageBuilder.setHeader(feedHeaderBuilder);
        GtfsRealtime.FeedEntity.Builder feedEntityBuilder = GtfsRealtime.FeedEntity.newBuilder();
        feedEntityBuilder.setId("test");
        GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();
        GtfsRealtime.VehiclePosition.Builder vehiclePositionBuilder = GtfsRealtime.VehiclePosition.newBuilder();
        GtfsRealtime.Alert.Builder alertBuilder = GtfsRealtime.Alert.newBuilder();
        GtfsRealtime.EntitySelector.Builder entitySelectorBuilder = GtfsRealtime.EntitySelector.newBuilder();

        /**
         * NOT combined feeds
         */

        // Add 3 trips, no vehicle positions or alerts
        tripUpdateBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1"));
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        tripUpdateBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("2"));
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        tripUpdateBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("3"));
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        assertEquals(3, feedMessageBuilder.getEntityList().size());
        assertFalse(GtfsUtils.isCombinedFeed(feedMessageBuilder.build()));

        // Add 3 vehicle positions, no trip updates or alerts
        feedMessageBuilder.clearEntity();
        feedEntityBuilder.clearTripUpdate();
        feedEntityBuilder.clearVehicle();
        feedEntityBuilder.clearAlert();

        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("A"));
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("B"));
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("C"));
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        assertEquals(3, feedMessageBuilder.getEntityList().size());
        assertFalse(GtfsUtils.isCombinedFeed(feedMessageBuilder.build()));

        // Add 3 alerts, no trip updates or vehicle positions
        feedMessageBuilder.clearEntity();
        feedEntityBuilder.clearTripUpdate();
        feedEntityBuilder.clearVehicle();
        feedEntityBuilder.clearAlert();

        entitySelectorBuilder.setStopId("Z");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        entitySelectorBuilder.setStopId("Y");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        entitySelectorBuilder.setStopId("X");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        assertEquals(3, feedMessageBuilder.getEntityList().size());
        assertFalse(GtfsUtils.isCombinedFeed(feedMessageBuilder.build()));

        /**
         * Combined feeds
         */

        // Add 1 trip updates and 1 vehicle positions, no alerts
        feedMessageBuilder.clearEntity();
        feedEntityBuilder.clearTripUpdate();
        feedEntityBuilder.clearVehicle();
        feedEntityBuilder.clearAlert();

        tripUpdateBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1"));
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("A"));
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        assertEquals(2, feedMessageBuilder.getEntityList().size());
        assertTrue(GtfsUtils.isCombinedFeed(feedMessageBuilder.build()));

        // Add 1 trip update and 1 alert, no vehicle positions
        feedMessageBuilder.clearEntity();
        feedEntityBuilder.clearTripUpdate();
        feedEntityBuilder.clearVehicle();
        feedEntityBuilder.clearAlert();

        tripUpdateBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1"));
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        entitySelectorBuilder.setStopId("Z");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        assertEquals(2, feedMessageBuilder.getEntityList().size());
        assertTrue(GtfsUtils.isCombinedFeed(feedMessageBuilder.build()));

        // Add 1 vehicle position and 1 alert, no trip updates
        feedMessageBuilder.clearEntity();
        feedEntityBuilder.clearTripUpdate();
        feedEntityBuilder.clearVehicle();
        feedEntityBuilder.clearAlert();

        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("A"));
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        entitySelectorBuilder.setStopId("Z");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        assertEquals(2, feedMessageBuilder.getEntityList().size());
        assertTrue(GtfsUtils.isCombinedFeed(feedMessageBuilder.build()));

        // Add 1 trip update, 1 vehicle position and 1 alert
        feedMessageBuilder.clearEntity();
        feedEntityBuilder.clearTripUpdate();
        feedEntityBuilder.clearVehicle();
        feedEntityBuilder.clearAlert();

        tripUpdateBuilder.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("1"));
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        vehiclePositionBuilder.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("A"));
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        entitySelectorBuilder.setStopId("Z");
        alertBuilder.addInformedEntity(entitySelectorBuilder.build());
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());

        assertEquals(3, feedMessageBuilder.getEntityList().size());
        assertTrue(GtfsUtils.isCombinedFeed(feedMessageBuilder.build()));
    }

    @Test
    public void testSortDate() throws URISyntaxException, IOException, InterruptedException {
        // Create three temporary files to test sorting order - sleep in between to make sure timestamps differ by seconds
        Path file1 = Files.createTempFile("tempFileOldest", ".tmp");
        Thread.sleep(1500);
        Path file2 = Files.createTempFile("tempFileMiddle", ".tmp");
        Thread.sleep(1500);
        Path file3 = Files.createTempFile("tempFileNewest", ".tmp");

        /**
         * SortUtils.compareByDateModified()
         */
        assertTrue(SortUtils.compareByDateModified(file2, file3) < 0);
        assertTrue(SortUtils.compareByDateModified(file3, file2) > 0);
        assertFalse(SortUtils.compareByDateModified(file2, file3) > 0);
        assertFalse(SortUtils.compareByDateModified(file3, file2) < 0);

        assertTrue(SortUtils.compareByDateModified(file1, file2) < 0);
        assertTrue(SortUtils.compareByDateModified(file2, file1) > 0);
        assertFalse(SortUtils.compareByDateModified(file2, file1) < 0);
        assertFalse(SortUtils.compareByDateModified(file1, file2) > 0);

        /**
         * SortUtils.sortByDateModified()
         */
        final List<File> files = Stream.of(file3, file2, file1).map(Path::toFile)
                .collect(Collectors.toList());

        // Before sorting - should be backwards, newest to oldest
        File[] array = files.toArray(new File[files.size()]);
        assertTrue(array[0].getName().startsWith("tempFileNewest"));
        assertTrue(array[1].getName().startsWith("tempFileMiddle"));
        assertTrue(array[2].getName().startsWith("tempFileOldest"));

        // After sorting, should be in date order (oldest to newest)
        array = SortUtils.sortByDateModified(array);
        assertTrue(array[0].getName().startsWith("tempFileOldest"));
        assertTrue(array[1].getName().startsWith("tempFileMiddle"));
        assertTrue(array[2].getName().startsWith("tempFileNewest"));

        for (File file : array) {
            file.deleteOnExit();
        }
    }

    @Test
    public void testSortFileName() throws URISyntaxException {
        /**
         * SortUtils.compareByFileName()
         */
        // bullrunner-gtfs.zip is after bullrunner-gtfs-no-shapes.zip
        Path bullrunnerGtfs = Paths.get(getClass().getClassLoader().getResource("bullrunner-gtfs.zip").toURI());
        Path bullrunnerGtfsNoShapes = Paths.get(getClass().getClassLoader().getResource("bullrunner-gtfs-no-shapes.zip").toURI());
        assertTrue(SortUtils.compareByFileName(bullrunnerGtfs, bullrunnerGtfsNoShapes) > 0);

        // bullrunner-gtfs.zip is before testagency2.zip
        Path testAgency2 = Paths.get(getClass().getClassLoader().getResource("testagency2.zip").toURI());
        assertTrue(SortUtils.compareByFileName(bullrunnerGtfs, testAgency2) < 0);

        // Should be sorted by date in file name (ascending)
        Path tu1 = Paths.get(getClass().getClassLoader().getResource("TripUpdates-2017-02-18T20-00-08Z.txt").toURI());
        Path tu2 = Paths.get(getClass().getClassLoader().getResource("TripUpdates-2017-02-18T20-00-23Z.txt").toURI());
        Path tu3 = Paths.get(getClass().getClassLoader().getResource("TripUpdates-2017-02-18T20-01-08Z.txt").toURI());

        assertTrue(SortUtils.compareByFileName(tu1, tu2) < 0);
        assertTrue(SortUtils.compareByFileName(tu2, tu3) < 0);
        assertTrue(SortUtils.compareByFileName(tu1, tu3) < 0);
        assertTrue(SortUtils.compareByFileName(tu3, tu1) > 0);
        assertTrue(SortUtils.compareByFileName(tu3, tu2) > 0);
        assertTrue(SortUtils.compareByFileName(tu2, tu1) > 0);

        /**
         * SortUtils.sortByName()
         */
        final List<File> files = Stream.of(tu3, tu2, tu1).map(Path::toFile)
                .collect(Collectors.toList());

        // Before sorting - should be backwards
        File[] array = files.toArray(new File[files.size()]);
        assertEquals("TripUpdates-2017-02-18T20-01-08Z.txt", array[0].getName());
        assertEquals("TripUpdates-2017-02-18T20-00-23Z.txt", array[1].getName());
        assertEquals("TripUpdates-2017-02-18T20-00-08Z.txt", array[2].getName());

        // After sorting, should be in alpha order
        array = SortUtils.sortByName(array);
        assertEquals("TripUpdates-2017-02-18T20-00-08Z.txt", array[0].getName());
        assertEquals("TripUpdates-2017-02-18T20-00-23Z.txt", array[1].getName());
        assertEquals("TripUpdates-2017-02-18T20-01-08Z.txt", array[2].getName());
    }

    @Test
    public void testGetTimestampFromFileName() throws URISyntaxException {
        String fileNameTu = "TripUpdates-2017-02-18T20-01-08Z.pb";
        long timestamp = TimestampUtils.getTimestampFromFileName(fileNameTu);
        assertEquals(1487448068000L, timestamp);

        String fileNameVp = "VehiclePositions-2017-02-18T20-01-08Z.pb";
        timestamp = TimestampUtils.getTimestampFromFileName(fileNameVp);
        assertEquals(1487448068000L, timestamp);

    }

    @Test
    public void testIsInFuture() {
        final long TOLERANCE_SECONDS = 5;
        boolean inFuture;

        // Same time - should return false
        inFuture = TimestampUtils.isInFuture(TimeUnit.SECONDS.toMillis(100), 100, TOLERANCE_SECONDS);
        assertEquals(false, inFuture);

        // Timestamp is in past within tolerance - should return false
        inFuture = TimestampUtils.isInFuture(TimeUnit.SECONDS.toMillis(100), 95, TOLERANCE_SECONDS);
        assertEquals(false, inFuture);

        // Timestamp is in past outside of tolerance - should return false
        inFuture = TimestampUtils.isInFuture(TimeUnit.SECONDS.toMillis(100), 94, TOLERANCE_SECONDS);
        assertEquals(false, inFuture);

        // Timestamp is in future within tolerance - should return false
        inFuture = TimestampUtils.isInFuture(TimeUnit.SECONDS.toMillis(100), 105, TOLERANCE_SECONDS);
        assertEquals(false, inFuture);

        // Timestamp is in future outside of tolerance - should return true
        inFuture = TimestampUtils.isInFuture(TimeUnit.SECONDS.toMillis(100), 106, TOLERANCE_SECONDS);
        assertEquals(true, inFuture);
    }

    @Test
    public void testGetAllRules() {
        List<ValidationRule> rules = ValidationRules.getRules();
        assertEquals(61, rules.size());
    }
}
