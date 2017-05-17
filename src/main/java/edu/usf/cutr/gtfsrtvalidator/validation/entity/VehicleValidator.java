/*
 * Copyright (C) 2011 Nipuna Gunathilake.
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

package edu.usf.cutr.gtfsrtvalidator.validation.entity;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.hsqldb.lib.StringUtil;
import org.locationtech.spatial4j.shape.Shape;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;


/**
 * E026 - Invalid vehicle position
 * E027 - Invalid vehicle bearing
 * E028 - Vehicle position outside agency coverage area
 * E029 - Vehicle position outside trip shape buffer
 * W002 - vehicle_id should be populated for TripUpdates and VehiclePositions
 * W004 - VehiclePosition has unrealistic speed
 */

public class VehicleValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(VehicleValidator.class);

    public static final float MAX_REALISTIC_SPEED_METERS_PER_SECOND = 26.0f;  // Approx. 60 miles per hour

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
        List<OccurrenceModel> e026List = new ArrayList<>();
        List<OccurrenceModel> e027List = new ArrayList<>();
        List<OccurrenceModel> e028List = new ArrayList<>();
        List<OccurrenceModel> e029List = new ArrayList<>();
        List<OccurrenceModel> w002List = new ArrayList<>();
        List<OccurrenceModel> w004List = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();

                // W002: vehicle_id should be populated in trip_update
                if (StringUtil.isEmpty(tripUpdate.getVehicle().getId())) {
                    OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId());
                    w002List.add(om);
                    _log.debug(om.getPrefix() + " " + W002.getOccurrenceSuffix());
                }
            }
            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition v = entity.getVehicle();

                // W002: vehicle_id should be populated in VehiclePosition
                if (StringUtil.isEmpty(v.getVehicle().getId())) {
                    OccurrenceModel om = new OccurrenceModel("entity ID " + entity.getId());
                    w002List.add(om);
                    _log.debug(om.getPrefix() + " " + W002.getOccurrenceSuffix());
                }

                // W004: VehiclePosition has unrealistic speed
                if (v.hasPosition() && v.getPosition().hasSpeed()) {
                    if (v.getPosition().getSpeed() > MAX_REALISTIC_SPEED_METERS_PER_SECOND ||
                            v.getPosition().getSpeed() < 0f) {
                        OccurrenceModel om = new OccurrenceModel((v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() : "entity ID " + entity.getId()) +
                                " speed of " + v.getPosition().getSpeed() + " m/s (" + String.format("%.2f", GtfsUtils.toMilesPerHour(v.getPosition().getSpeed())) + " mph)");
                        w004List.add(om);
                        _log.debug(om.getPrefix() + " " + W004.getOccurrenceSuffix());
                    }
                }

                if (v.hasPosition()) {
                    GtfsRealtime.Position position = v.getPosition();
                    String id = (v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() : "entity ID " + entity.getId());
                    if (!position.hasLatitude() || !position.hasLongitude()) {
                        // E026: Invalid vehicle position - missing lat/long
                        OccurrenceModel om = new OccurrenceModel(id + " position is missing lat/long");
                        e026List.add(om);
                        _log.debug(om.getPrefix() + " " + E026.getOccurrenceSuffix());
                    } else if (!GtfsUtils.isPositionValid(position)) {
                        // E026: Invalid vehicle position - invalid lat/long
                        OccurrenceModel om = new OccurrenceModel(id + " has latitude/longitude of (" + position.getLatitude() + "," + position.getLongitude() + ")");
                        e026List.add(om);
                        _log.debug(om.getPrefix() + " " + E026.getOccurrenceSuffix());
                    } else {
                        // Position is valid - check E028, if it lies within the agency bounds, using shapes.txt if it exists
                        boolean insideBounds = checkE028(entity, gtfsMetadata, e028List);
                        if (insideBounds) {
                            // Position is within agency bounds - check E029, if it lies within the trip bounds using shapes.txt
                            checkE029(entityList, entity, gtfsMetadata, e029List);
                        }
                    }
                    if (!GtfsUtils.isBearingValid(position)) {
                        // E027: Invalid vehicle bearing
                        OccurrenceModel om = new OccurrenceModel(id + " has bearing of " + position.getBearing());
                        e027List.add(om);
                        _log.debug(om.getPrefix() + " " + E027.getOccurrenceSuffix());
                    }
                }
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!e026List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E026), e026List));
        }
        if (!e027List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E027), e027List));
        }
        if (!e028List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E028), e028List));
        }
        if (!e029List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E029), e029List));
        }
        if (!w002List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W002), w002List));
        }
        if (!w004List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W004), w004List));
        }
        return errors;
    }

    /**
     * Vehicle position outside agency coverage area - E028
     *
     * @param entity       entity that has vehicle positions to check
     * @param gtfsMetadata GTFS metadata for this entity
     * @param errors       list to which any errors can be added
     * @return true if the vehicle position is within agency coverage area, false if it is not
     */
    private boolean checkE028(GtfsRealtime.FeedEntity entity, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        GtfsRealtime.VehiclePosition v = entity.getVehicle();
        GtfsRealtime.Position position = v.getPosition();
        String id = (v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() : "entity ID " + entity.getId());

        // See if position lies within the agency bounds, using shapes.txt if it exists
        Shape boundingBox;
        String boundingDescription;
        if (gtfsMetadata.getShapeBoundingBoxWithBuffer() != null) {
            // Use shapes.txt
            boundingBox = gtfsMetadata.getShapeBoundingBoxWithBuffer();
            boundingDescription = "shapes.txt";
        } else {
            // Use stops.txt
            boundingBox = gtfsMetadata.getStopBoundingBoxWithBuffer();
            boundingDescription = "stops.txt";
        }

        boolean insideBounds = GtfsUtils.isPositionWithinShape(position, boundingBox);
        if (!insideBounds) {
            OccurrenceModel om = new OccurrenceModel(id + " at (" + position.getLatitude() + "," + position.getLongitude() +
                    ") is more than " + GtfsMetadata.REGION_BUFFER_METERS + " meters (" + String.format("%.2f", GtfsUtils.toMiles(GtfsMetadata.REGION_BUFFER_METERS)) + " mile(s)) outside entire GTFS "
                    + boundingDescription + " coverage area");
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E028.getOccurrenceSuffix());
        }
        return insideBounds;
    }

    /**
     * Vehicle position outside trip shape buffer - E029
     *
     * @param entityList   a list of all entities for this feed iteration (needed to check if there are any detour alerts for this trip)
     * @param entity       entity that has a vehicle position to check
     * @param gtfsMetadata GTFS metadata for this entity
     * @param errors       list to which any errors can be added
     */
    private void checkE029(List<GtfsRealtime.FeedEntity> entityList, GtfsRealtime.FeedEntity entity, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        GtfsRealtime.VehiclePosition v = entity.getVehicle();

        // If the vehicle doesn't have a trip_id, we can't check E029 - return
        if (!v.hasTrip() || !v.getTrip().hasTripId()) {
            return;
        }
        String tripId = v.getTrip().getTripId();
        String routeId = null;
        if (v.getTrip().hasRouteId()) {
            routeId = v.getTrip().getRouteId();
        }
        GtfsRealtime.Position position = v.getPosition();
        String id = (v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() : "entity ID " + entity.getId());

        Map<String, Shape> tripShapes = gtfsMetadata.getTripShapes();
        if (tripShapes == null) {
            // No shapes - can't check E029
            return;
        }
        Shape s = tripShapes.get(tripId);
        if (s == null) {
            // No shape for this trip_id - can't check E029
            return;
        }
        if (!GtfsUtils.isPositionWithinShape(position, s)) {
            if (hasDetourAlert(entityList, tripId, routeId)) {
                // There is a DETOUR alert for this vehicle's trip_id or route_id, so it's allowed to be outside the trip shape
                return;
            }

            // E029 - Vehicle position is outside of trip shape buffer and it's not on DETOUR
            OccurrenceModel om = new OccurrenceModel(id + " trip_id " + tripId + " at (" + position.getLatitude() + "," + position.getLongitude() +
                    ") is more than " + GtfsMetadata.TRIP_BUFFER_METERS + " meters (" + String.format("%.2f", GtfsUtils.toMiles(GtfsMetadata.TRIP_BUFFER_METERS)) + " mile(s)) outside GTFS trip shape buffer");
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E029.getOccurrenceSuffix());
        }
    }

    /**
     * Returns true if there is a DETOUR service alert for either the provided trip_id or the provided route_id, or false if there is not
     *
     * @param entityList GTFS-rt entities to check for DETOUR service alerts
     * @param tripId     trip_id to check in the service alerts
     * @param routeId    route_id to check in the service alerts
     * @return true if there is a DETOUR service alert for either the provided trip_id or the provided route_id, or false if there is not
     */
    private boolean hasDetourAlert(List<GtfsRealtime.FeedEntity> entityList, String tripId, String routeId) {
        // This could get expensive for a lot of alerts
        for (GtfsRealtime.FeedEntity e : entityList) {
            if (e.hasAlert()) {
                GtfsRealtime.Alert a = e.getAlert();
                if (a.hasEffect() && a.getEffect().equals(GtfsRealtime.Alert.Effect.DETOUR)) {
                    for (GtfsRealtime.EntitySelector entitySelector : a.getInformedEntityList()) {
                        if (entitySelector.hasTrip()) {
                            if (tripId.equals(entitySelector.getTrip().getTripId())) {
                                // There is a DETOUR alert for this vehicle's trip, so it's allowed to be outside the trip shape
                                return true;
                            }
                            if (routeId != null && routeId.equals(entitySelector.getTrip().getRouteId())) {
                                // There is a DETOUR alert for this vehicle's route, so it's allowed to be outside the trip shape
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
