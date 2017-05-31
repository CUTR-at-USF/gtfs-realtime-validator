/*
 * Copyright (C) 2011-2017 Nipuna Gunathilake, University of South Florida
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
import edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

/**
 * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
 * E036 - Sequential stop_time_updates have the same stop_sequence
 * E037 - Sequential stop_time_updates have the same stop_id
 */
public class StopTimeSequenceValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(StopTimeSequenceValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
        List<OccurrenceModel> e002List = new ArrayList<>();
        List<OccurrenceModel> e036List = new ArrayList<>();
        List<OccurrenceModel> e037List = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdateList = tripUpdate.getStopTimeUpdateList();

                List<Integer> stopSequenceList = new ArrayList<>();
                Integer previousStopSequence = null;
                String previousStopId = null;
                for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdateList) {
                    if (previousStopSequence != null) {
                        checkE036(entity, previousStopSequence, stopTimeUpdate, e036List);
                    }
                    if (previousStopId != null) {
                        checkE037(entity, previousStopId, stopTimeUpdate, e037List);
                    }
                    previousStopSequence = stopTimeUpdate.getStopSequence();
                    previousStopId = stopTimeUpdate.getStopId();
                    if (stopTimeUpdate.hasStopSequence()) {
                        stopSequenceList.add(stopTimeUpdate.getStopSequence());
                    }
                }

                boolean sorted = Ordering.natural().isOrdered(stopSequenceList);
                if (!sorted) {
                    String id = GtfsUtils.getTripId(entity, tripUpdate);
                    OccurrenceModel om = new OccurrenceModel(id + " stop_sequence " + stopSequenceList.toString());
                    e002List.add(om);
                    _log.debug(om.getPrefix() + " " + E002.getOccurrenceSuffix());
                }

                // TODO - detect out-of-order stops when stop_sequence isn't provided - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/159
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!e002List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E002), e002List));
        }
        if (!e036List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E036), e036List));
        }
        if (!e037List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E037), e037List));
        }
        return errors;
    }

    /**
     * Checks E036 - if the provided previousStopSequence value is the same as the current stopTimeUpdate stop_sequence
     * it adds an error to the provided error list.
     *
     * @param entity               entity that the stopTimeUpdate is from
     * @param previousStopSequence the stop_sequence for the previous StopTimeUpdate
     * @param stopTimeUpdate       the current stopTimeUpdate
     * @param errors               the list to add the errors to
     */
    private void checkE036(GtfsRealtime.FeedEntity entity, Integer previousStopSequence, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> errors) {
        if (stopTimeUpdate.hasStopSequence() &&
                previousStopSequence == stopTimeUpdate.getStopSequence()) {
            String id = GtfsUtils.getTripId(entity, entity.getTripUpdate());
            OccurrenceModel om = new OccurrenceModel(id + " has repeating stop_sequence " + previousStopSequence);
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E036.getOccurrenceSuffix());
        }
    }

    /**
     * Checks E037 - if the provided previousStopId value is the same as the current stopTimeUpdate stop_id it adds
     * an error to the provided error list.
     *
     * @param entity         entity that the stopTimeUpdate is from
     * @param previousStopId the stop_id for the previous StopTimeUpdate
     * @param stopTimeUpdate the current stopTimeUpdate
     * @param errors         the list to add the errors to
     */
    private void checkE037(GtfsRealtime.FeedEntity entity, String previousStopId, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> errors) {
        if (!previousStopId.isEmpty() && stopTimeUpdate.hasStopId() &&
                previousStopId.equals(stopTimeUpdate.getStopId())) {
            String id = GtfsUtils.getTripId(entity, entity.getTripUpdate());
            StringBuilder prefix = new StringBuilder();
            prefix.append(id);
            prefix.append(" has repeating stop_id ");
            prefix.append(previousStopId);
            if (stopTimeUpdate.hasStopSequence()) {
                prefix.append(" at stop_sequence ");
                prefix.append(stopTimeUpdate.getStopSequence());
            }
            OccurrenceModel om = new OccurrenceModel(prefix.toString());
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E036.getOccurrenceSuffix());
        }
    }
}
