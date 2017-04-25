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

import com.google.common.collect.Ordering;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E002;

/**
 * ID: E002
 * Description: stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
 */
public class StopTimeSequenceValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(StopTimeSequenceValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
        List<OccurrenceModel> errorOccurrenceList = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdateList = tripUpdate.getStopTimeUpdateList();

                List<Integer> stopSequenceList = new ArrayList<>();
                for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdateList) {
                    stopTimeUpdate.getStopSequence();
                    stopSequenceList.add(stopTimeUpdate.getStopSequence());
                }

                boolean sorted = Ordering.natural().isOrdered(stopSequenceList);
                if (!sorted) {
                    String tripId = null;
                    if (tripUpdate.hasTrip() && tripUpdate.getTrip().hasTripId()) {
                        tripId = tripUpdate.getTrip().getTripId();
                    }

                    OccurrenceModel om = new OccurrenceModel((tripId != null ? "trip_id " + tripId + " " : "") + "stop_sequence " + stopSequenceList.toString());
                    errorOccurrenceList.add(om);
                    _log.debug(om.getPrefix() + " " + E002.getOccurrenceSuffix());
                }

                // TODO - detect out-of-order stops when stop_sequence isn't provided
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorOccurrenceList.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E002), errorOccurrenceList));
        }
        return errors;
    }
}
