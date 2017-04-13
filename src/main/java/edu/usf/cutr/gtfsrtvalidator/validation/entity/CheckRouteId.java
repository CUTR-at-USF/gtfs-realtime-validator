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
package edu.usf.cutr.gtfsrtvalidator.validation.entity;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.hsqldb.lib.StringUtil;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Route;

import java.util.*;

/**
 * ID: E004
 * Description: All route_ids provided in the GTFS-rt feed must appear in the GTFS data
 */
public class CheckRouteId implements FeedEntityValidator {

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {
        Collection<Route> gtfsRouteList = gtfsData.getAllRoutes();

        MessageLogModel messageLogModel = new MessageLogModel(ValidationRules.E004);
        List<OccurrenceModel> errorOccurrenceList = new ArrayList<>();

        Set<String> routeIdList = new HashSet<>();

        // Get a list of route_ids from the GTFS feed
        for (Route r : gtfsRouteList) {
            routeIdList.add(r.getId().getId());
        }

        // Check the route_id values against the values from the GTFS feed
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                String routeId = entity.getTripUpdate().getTrip().getRouteId();
                String tripId = entity.getTripUpdate().getTrip().getTripId();
                if (!StringUtil.isEmpty(routeId) && !routeIdList.contains(routeId)) {
                    OccurrenceModel occurrenceModel = new OccurrenceModel("$.entity.*.trip_update.trip[?(@.route_id==\"" + routeId + "\")]", tripId);
                    errorOccurrenceList.add(occurrenceModel);
                }
            }
            if (entity.hasVehicle() && entity.getVehicle().hasTrip()) {
                String routeId = entity.getVehicle().getTrip().getRouteId();
                String tripId = entity.getTripUpdate().getTrip().getTripId();
                if (!StringUtil.isEmpty(routeId) && !routeIdList.contains(routeId)) {
                    OccurrenceModel occurrenceModel = new OccurrenceModel("$.entity.*.vehicle.trip[?(@.route_id==\"" + routeId + "\")]", tripId);
                    errorOccurrenceList.add(occurrenceModel);
                }
            }
        }
        return Arrays.asList(new ErrorListHelperModel(messageLogModel, errorOccurrenceList));
    }
}
