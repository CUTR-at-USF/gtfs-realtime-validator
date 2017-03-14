/*
 * Copyright (C) 2011-2017 Nipuna Gunathilake, University of South Florida.
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
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TimestampValidation implements FeedEntityValidator{

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TimestampValidation.class);

    @Override
    public ErrorListHelperModel validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {
        MessageLogModel messageLogModel = new MessageLogModel("w001");
        OccurrenceModel errorOccurrence;
        List<OccurrenceModel> errorOccurrenceList = new ArrayList<>();
        
        long headerTimestamp = feedMessage.getHeader().getTimestamp();
        if (headerTimestamp == 0) {
            _log.debug("Timestamp not present in FeedHeader");
            errorOccurrence = new OccurrenceModel("$.header.timestamp not populated", String.valueOf(headerTimestamp));
            errorOccurrenceList.add(errorOccurrence);
        }
        for(GtfsRealtime.FeedEntity entity: feedMessage.getEntityList()) {
            long tripupdateTimestamp = entity.getTripUpdate().getTimestamp();
            long vehicleTimestamp = entity.getVehicle().getTimestamp();
            if (tripupdateTimestamp == 0) {
                _log.debug("Timestamp not present in TripUpdate");
                errorOccurrence = new OccurrenceModel("$.entity.*.trip_update.timestamp not populated", String.valueOf(tripupdateTimestamp));
                errorOccurrenceList.add(errorOccurrence);
            }
            if (vehicleTimestamp == 0) {
                _log.debug("Timestamp not present in VehiclePosition");
                errorOccurrence = new OccurrenceModel("$.entity.*.vehicle_position.timestamp not populated", String.valueOf(vehicleTimestamp));
                errorOccurrenceList.add(errorOccurrence);
            }
        }
        return new ErrorListHelperModel(messageLogModel, errorOccurrenceList);
    }
}
