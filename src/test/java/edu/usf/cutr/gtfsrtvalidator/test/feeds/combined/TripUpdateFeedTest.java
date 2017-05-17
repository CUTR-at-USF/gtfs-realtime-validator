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
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopTimeSequenceValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.VehicleValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.gtfs.StopLocationTypeValidator;
import org.junit.Test;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.io.ShapeIO;
import org.locationtech.spatial4j.io.ShapeWriter;
import org.locationtech.spatial4j.shape.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.locationtech.spatial4j.context.SpatialContext.GEO;

/* 
 * Tests all the warnings and rules that validate TripUpdate feed.
 * Tests: W002 - "vehicle_id should be populated in trip_update"
 *        E002 - "stop_time_updates for a given trip_id must be sorted by increasing stop_sequence"
 *        E010 - "If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0"
*/
public class TripUpdateFeedTest extends FeedMessageTest {
    
    public TripUpdateFeedTest() throws Exception {}

    /**
     * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
     */
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

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E002, results, 0);
        
        /* Adding stop sequence 3. So, the stop sequence now is 1, 5, 3 which is unordered.
           So, the validation fails and the assertion test passes
        */
        stopTimeUpdateBuilder.setStopSequence(3);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E002, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W002 - vehicle_id should be populated in TripUpdate and VehiclePosition feeds
     */
    @Test
    public void testVehicleIdValidation() {
        VehicleValidator vehicleValidator = new VehicleValidator();
        
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        
        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        
        // setting a value for vehicle id = 1
        vehicleDescriptorBuilder.setId("1");
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No errors, if vehicle id has a value.
        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W002, results, 0);

        // Test with empty string for Vehicle ID, which should generate 2 warnings (one for TripUpdates and one for VehiclePositions)
        vehicleDescriptorBuilder.setId("");
        tripUpdateBuilder.setVehicle(vehicleDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W002, results, 2);

        clearAndInitRequiredFeedFields();
    }

    /**
     * W004 - VehiclePosition has unrealistic speed
     */
    @Test
    public void testVehicleSpeedValidation() {
        VehicleValidator vehicleValidator = new VehicleValidator();

        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No warnings, if speed isn't populated
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W004, results, 0);

        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();

        // Valid lat and long (USF Campus in Tampa, FL), as they are required fields
        positionBuilder.setLatitude(28.0587f);
        positionBuilder.setLongitude(-82.4139f);

        /**
         * Valid speed of ~30 miles per hour
         */
        positionBuilder.setSpeed(13.0f);
        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No warnings, for valid speed
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W004, results, 0);

        /**
         * Invalid negative speed
         */
        positionBuilder.setSpeed(-13.0f);
        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // One warning for negative speed value
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W004, results, 1);

        /**
         * Abnormally large speed
         */
        positionBuilder.setSpeed(31.0f); // ~ 70 miles per hour
        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // One warning for abnormally large speed
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.W004, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E026 - Invalid vehicle position
     */
    @Test
    public void testInvalidVehiclePosition() {
        VehicleValidator vehicleValidator = new VehicleValidator();

        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No warnings, if position isn't populated
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E026, results, 0);

        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();

        // Valid lat and long (USF Campus in Tampa, FL)
        positionBuilder.setLatitude(28.0587f);
        positionBuilder.setLongitude(-82.4139f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E026, results, 0);

        // Invalid lat - 1 error
        positionBuilder.setLatitude(1000f);
        positionBuilder.setLongitude(-82.4572f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E026, results, 1);

        // Invalid long - 1 error
        positionBuilder.setLatitude(27.9506f);
        positionBuilder.setLongitude(-1000);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E026, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E027 - Invalid vehicle bearing
     */
    @Test
    public void testInvalidVehicleBearing() {
        VehicleValidator vehicleValidator = new VehicleValidator();

        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No warnings, if position isn't populated
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E027, results, 0);

        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();

        // Set valid lat and long (USF Campus in Tampa, FL), as they are required fields
        positionBuilder.setLatitude(28.0587f);
        positionBuilder.setLongitude(-82.4139f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // No warnings, if bearing isn't populated
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E027, results, 0);

        // Valid bearing - no errors
        positionBuilder.setBearing(15);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E027, results, 0);

        // Invalid bearing - 1 error
        positionBuilder.setBearing(-1);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E027, results, 1);

        // Invalid bearing - 1 error
        positionBuilder.setBearing(361);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E027, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E010 - If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0
     */
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
        TestUtils.assertResults(ValidationRules.E010, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * Used as part of:
     * E028 - Vehicle position outside agency coverage area
     * E029 - Vehicle position outside trip shape buffer
     *
     * @throws IOException
     */
    @Test
    public void testStopAndTripShapeBounds() throws IOException {
        Rectangle stopBoundingBox = bullRunnerGtfsMetadata.getStopBoundingBox();
        Rectangle stopBoundingBoxWithBuffer = bullRunnerGtfsMetadata.getStopBoundingBoxWithBuffer();
        Rectangle shapeBoundingBox = bullRunnerGtfsMetadata.getShapeBoundingBox();
        Rectangle shapeBoundingBoxWithBuffer = bullRunnerGtfsMetadata.getShapeBoundingBoxWithBuffer();

        ShapeFactory sf = GEO.getShapeFactory();
        Point p;
        SpatialRelation spatialRelation;

        /**
         * Point is inside GTFS stops.txt bounding box
         */
        p = sf.pointXY(-82.4139, 28.0587); // USF Campus in Tampa, FL
        spatialRelation = stopBoundingBox.relate(p);
        assertEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = stopBoundingBoxWithBuffer.relate(p);
        assertEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = shapeBoundingBox.relate(p);
        assertEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = shapeBoundingBoxWithBuffer.relate(p);
        assertEquals(SpatialRelation.CONTAINS, spatialRelation);

        /**
         * Point is outside of GTFS stops.txt bounding box
         */
        p = sf.pointXY(-82.4655826, 27.9482837); // Downtown Tampa, FL
        spatialRelation = stopBoundingBox.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = stopBoundingBoxWithBuffer.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = shapeBoundingBox.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = shapeBoundingBoxWithBuffer.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);

        p = sf.pointXY(-74.0059, 40.7128); // NYC
        spatialRelation = stopBoundingBox.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = stopBoundingBoxWithBuffer.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = shapeBoundingBox.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);
        spatialRelation = shapeBoundingBoxWithBuffer.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);

        /**
         * Point is inside of USF Bull Runner Route A polygon (buffer surrounding shapes.txt shape)
         */
        Map<String, Shape> tripShapes = bullRunnerGtfsMetadata.getTripShapes();
        Shape routeA = tripShapes.get("2");

        p = sf.pointXY(-82.4131679534912, 28.064065878608385);  // USF Marshall Center
        spatialRelation = routeA.relate(p);
        assertEquals(SpatialRelation.CONTAINS, spatialRelation);

        /**
         * Point is outside of USF Bull Runner Route A polygon (buffer surrounding shapes.txt shape)
         */
        p = sf.pointXY(-82.43475437164307, 28.057438520876673);  // University Mall
        spatialRelation = routeA.relate(p);
        assertNotEquals(SpatialRelation.CONTAINS, spatialRelation);

        /**
         * Test GeoJSON output - for troubleshooting and visualizing using http://geojson.io/
         */
        PrintWriter writer = new PrintWriter(System.out);

        ShapeFactory jtsSf = JtsSpatialContext.GEO.getShapeFactory();
        ShapeWriter shpWriter = jtsSf.getSpatialContext().getFormats().getWriter(ShapeIO.GeoJSON);
        writer.append("USF Bull Runner stops.txt bounding box --------------\n");
        shpWriter.write(writer, stopBoundingBox);
        writer.append("\nUSF Bull Runner stops.txt bounding box with buffer --------------\n");
        shpWriter.write(writer, stopBoundingBoxWithBuffer);
        writer.flush();

        writer.append("\nUSF Bull Runner shapes.txt bounding box --------------\n");
        shpWriter.write(writer, shapeBoundingBox);
        writer.append("\nUSF Bull Runner shapes.txt bounding box with buffer --------------\n");
        shpWriter.write(writer, shapeBoundingBoxWithBuffer);
        writer.flush();

        writer.append("\nUSF Bull Runner Route A trip shape output --------------\n");
        shpWriter.write(writer, routeA);
        writer.flush();

        Rectangle gtfsBoundingBox = gtfsDataMetadata.getStopBoundingBox();
        writer.append("\ntestagency.zip bounding box output --------------\n");
        shpWriter.write(writer, gtfsBoundingBox);
        writer.flush();
    }

    /**
     * E028 - Vehicle position outside agency coverage area
     */
    @Test
    public void testE028VehiclePositionOutsideAgencyBounds() {
        VehicleValidator vehicleValidator = new VehicleValidator();

        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        /**
         * Using normal USF Bull Runner feed, which includes GTFS shapes.txt
         */

        // No errors, if position isn't populated
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E028, results, 0);

        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();

        // Valid lat and long (USF Campus in Tampa, FL)
        positionBuilder.setLatitude(28.0587f);
        positionBuilder.setLongitude(-82.4139f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E028, results, 0);

        // Valid lat and long, but outside agency bounding box (Downtown Tampa, FL) - one error
        positionBuilder.setLatitude(27.9482837f);
        positionBuilder.setLongitude(-82.4655826f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E028, results, 1);

        /**
         * Using modified USF Bull Runner feed WITHOUT shapes.txt
         */
        // Valid lat and long (USF Campus in Tampa, FL)
        positionBuilder.setLatitude(28.0587f);
        positionBuilder.setLongitude(-82.4139f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfsNoShapes, bullRunnerGtfsNoShapesMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E028, results, 0);

        // Valid lat and long, but outside agency bounding box (Downtown Tampa, FL) - one error
        positionBuilder.setLatitude(27.9482837f);
        positionBuilder.setLongitude(-82.4655826f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfsNoShapes, bullRunnerGtfsNoShapesMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E028, results, 1);
    }

    /**
     * E029 - Vehicle position outside trip shape buffer
     */
    @Test
    public void testE029VehiclePositionOutsideTripBounds() {
        VehicleValidator vehicleValidator = new VehicleValidator();

        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");

        // USF Bull Runner - route_id=A, trip_id=2
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("2");
        tripDescriptorBuilder.setRouteId("A");

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        /**
         * Using modified USF Bull Runner feed WITHOUT shapes.txt - shouldn't throw any errors, as we don't have trip shapes to match against
         */
        // Valid lat and long (USF Campus in Tampa, FL)
        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder();
        positionBuilder.setLatitude(28.0587f);
        positionBuilder.setLongitude(-82.4139f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfsNoShapes, bullRunnerGtfsNoShapesMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        // Valid lat and long, inside agency bounding box but outside trip path (Downtown Tampa, FL) - would be an error if we had shapes.txt, but we don't for this assertion, so no errors
        positionBuilder.setLatitude(28.057438520876673f);
        positionBuilder.setLongitude(-82.43475437164307f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfsNoShapes, bullRunnerGtfsNoShapesMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        // Add trip_id - still no errors, because we don't have shapes.txt
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());

        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfsNoShapes, bullRunnerGtfsNoShapesMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        /**
         * Using normal USF Bull Runner feed, which includes GTFS shapes.txt - now we can test for this error
         */

        // No errors, if position isn't populated
        vehiclePositionBuilder.clear();
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        // Valid lat and long (USF Marshal Center, which is within Route A path), but no trip_id in GTFS-rt - wouldn't be an error anyway, but we should still check this test case
        positionBuilder.setLatitude(28.064065878608385f);
        positionBuilder.setLongitude(-82.4131679534912f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        // Now add trip_id=2 - still no error (point is within trip polygon)
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        // Point is outside of USF Bull Runner route_id=A trip_id=2 polygon (buffer surrounding shapes.txt shape) - at University Mall, but no trip_id is set - so, no errors
        vehiclePositionBuilder.clearTrip();
        positionBuilder.setLatitude(28.057438520876673f);
        positionBuilder.setLongitude(-82.43475437164307f);

        vehiclePositionBuilder.setPosition(positionBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        // Add the trip_id=2 to GTFS-rt message - now we can match against trip_id=2 in shapes.txt, and this should generate one error
        vehiclePositionBuilder.setTrip(tripDescriptorBuilder.build());
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 1);

        // Now add an alert saying there is a detour on trip_id=2 - point is allowed to be outside shape, so no error
        GtfsRealtime.Alert.Builder alertBuilder = GtfsRealtime.Alert.newBuilder();
        alertBuilder.setEffect(GtfsRealtime.Alert.Effect.DETOUR);
        alertBuilder.addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setTrip(tripDescriptorBuilder.build()));
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        // Make the alert to reference route_id=A instead of trip_id=2 - point is allowed to be outside shape, so still no error
        alertBuilder = GtfsRealtime.Alert.newBuilder();
        alertBuilder.setEffect(GtfsRealtime.Alert.Effect.DETOUR);
        alertBuilder.addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("A").build()));
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 0);

        // Change the alert to something other than a detour with trip_id=2 - point is not allowed outside shape, one error
        alertBuilder = GtfsRealtime.Alert.newBuilder();
        alertBuilder.setEffect(GtfsRealtime.Alert.Effect.UNKNOWN_EFFECT);
        alertBuilder.addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setTrip(tripDescriptorBuilder.build()));
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 1);

        // Change the alert to something other than a detour with route_id=A instead of trip_id=2 - point is allowed to be outside shape, so still no error
        alertBuilder = GtfsRealtime.Alert.newBuilder();
        alertBuilder.setEffect(GtfsRealtime.Alert.Effect.UNKNOWN_EFFECT);
        alertBuilder.addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("A").build()));
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 1);

        // Change the alert back to DETOUR, but change the route_id to a different route - one error again, as Route A is no longer on detour
        alertBuilder = GtfsRealtime.Alert.newBuilder();
        alertBuilder.setEffect(GtfsRealtime.Alert.Effect.DETOUR);
        alertBuilder.addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("C").build()));
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 1);

        // Alert is DETOUR again, but change the trip_id to a different trip - one error again, as trip_id=2 is no longer on detour
        alertBuilder = GtfsRealtime.Alert.newBuilder();
        alertBuilder.setEffect(GtfsRealtime.Alert.Effect.DETOUR);
        alertBuilder.addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("10").build()));
        feedEntityBuilder.setAlert(alertBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E029, results, 1);
    }
}
