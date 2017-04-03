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
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.hsqldb.lib.StringUtil;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * ID: w002
 * Description:vehicle_id should be populated in trip_update
 */

public class VehicleIdValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(VehicleIdValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {
        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
        int entityId = 0;

        MessageLogModel messageLogModel = new MessageLogModel(ValidationRules.W002);

        List<OccurrenceModel> errorOccurrenceList = new ArrayList<>();
        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {

                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                //w002: vehicle_id should be populated in trip_update
                if (StringUtil.isEmpty(tripUpdate.getVehicle().getId())) {
                    OccurrenceModel errorOccurrence = new OccurrenceModel("$.entity["+ entityId +"].trip_update", null);
                    errorOccurrenceList.add(errorOccurrence);
                    _log.debug(ValidationRules.W002.getErrorDescription());
                }
            }
            entityId++;
        }

        if (!errorOccurrenceList.isEmpty()) {
            return Arrays.asList(new ErrorListHelperModel(messageLogModel, errorOccurrenceList));
        }
        return null;
    }
}
