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
package edu.usf.cutr.gtfsrtvalidator.validation.entity.combined;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.hsqldb.lib.StringUtil;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils.isAddedTrip;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

/**
 * E003 - All trip_ids provided in the GTFS-rt feed must appear in the GTFS data
 * (unless schedule_relationship is ADDED)
 *
 * E004 - All route_ids provided in the GTFS-rt feed must appear in the GTFS data
 *
 * E016 - trip_ids with schedule_relationship ADDED must not be in GTFS data
 *
 * W006 - trip_update missing trip_id
 */
public class CheckRouteAndTripIds implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(CheckRouteAndTripIds.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<OccurrenceModel> errorListE003 = new ArrayList<>();
        List<OccurrenceModel> errorListE004 = new ArrayList<>();
        List<OccurrenceModel> errorListE016 = new ArrayList<>();
        List<OccurrenceModel> errorListW006 = new ArrayList<>();

        // Check the route_id values against the values from the GTFS feed
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                if (!entity.getTripUpdate().getTrip().hasTripId()) {
                    // W006 - No trip_id
                    OccurrenceModel om = new OccurrenceModel("entity ID " + entity.getId());
                    errorListW006.add(om);
                    _log.debug(om.getPrefix() + " " + W006.getOccurrenceSuffix());
                } else {
                    String tripId = entity.getTripUpdate().getTrip().getTripId();
                    if (!gtfsMetadata.getTripIds().contains(tripId)) {
                        if (!isAddedTrip(entity.getTripUpdate().getTrip())) {
                            // Trip isn't in GTFS data and isn't an ADDED trip - E003
                            OccurrenceModel om = new OccurrenceModel("trip_id " + tripId);
                            errorListE003.add(om);
                            _log.debug(om.getPrefix() + " " + E003.getOccurrenceSuffix());
                        }
                    } else {
                        if (isAddedTrip(entity.getTripUpdate().getTrip())) {
                            // Trip is in GTFS data and is an ADDED trip - E016
                            OccurrenceModel om = new OccurrenceModel("trip_id " + tripId);
                            errorListE016.add(om);
                            _log.debug(om.getPrefix() + " " + E016.getOccurrenceSuffix());
                        }
                    }
                }

                String routeId = entity.getTripUpdate().getTrip().getRouteId();
                if (!StringUtil.isEmpty(routeId) && !gtfsMetadata.getRouteIds().contains(routeId)) {
                    // E004 - route_id not in GTFS data
                    OccurrenceModel om = new OccurrenceModel("route_id " + routeId);
                    errorListE004.add(om);
                    _log.debug(om.getPrefix() + " " + E004.getOccurrenceSuffix());
                }
            }
            if (entity.hasVehicle() && entity.getVehicle().hasTrip()) {
                if (!entity.getVehicle().getTrip().hasTripId()) {
                    // W006 - No trip_id
                    OccurrenceModel om = new OccurrenceModel("entity ID " + entity.getId());
                    errorListW006.add(om);
                    _log.debug(om.getPrefix() + " " + W006.getOccurrenceSuffix());
                } else {
                    String tripId = entity.getTripUpdate().getTrip().getTripId();
                    if (!StringUtil.isEmpty(tripId)) {
                        if (!gtfsMetadata.getTripIds().contains(tripId)) {
                            if (!isAddedTrip(entity.getTripUpdate().getTrip())) {
                                // Trip isn't in GTFS data and isn't an ADDED trip - E003
                                OccurrenceModel om = new OccurrenceModel("vehicle_id " + entity.getVehicle().getVehicle().getId() + " trip_id " + tripId);
                                errorListE003.add(om);
                                _log.debug(om.getPrefix() + " " + E003.getOccurrenceSuffix());
                            }
                        } else {
                            if (isAddedTrip(entity.getTripUpdate().getTrip())) {
                                // Trip is in GTFS data and is an ADDED trip - E016
                                OccurrenceModel om = new OccurrenceModel("vehicle_id " + entity.getVehicle().getVehicle().getId() + " trip_id " + tripId);
                                errorListE016.add(om);
                                _log.debug(om.getPrefix() + " " + E016.getOccurrenceSuffix());
                            }
                        }
                    }
                }
                String routeId = entity.getVehicle().getTrip().getRouteId();
                if (!StringUtil.isEmpty(routeId) && !gtfsMetadata.getRouteIds().contains(routeId)) {
                    // E004 - route_id not in GTFS data
                    OccurrenceModel om = new OccurrenceModel("vehicle_id " + entity.getVehicle().getVehicle().getId() + " route_id " + routeId);
                    errorListE004.add(om);
                    _log.debug(om.getPrefix() + " " + E004.getOccurrenceSuffix());
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListE003.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E003), errorListE003));
        }
        if (!errorListE004.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E004), errorListE004));
        }
        if (!errorListE016.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E016), errorListE016));
        }
        if (!errorListW006.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W006), errorListW006));
        }
        return errors;
    }
}
