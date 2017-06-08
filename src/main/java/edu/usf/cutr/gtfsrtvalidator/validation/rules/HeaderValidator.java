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
package edu.usf.cutr.gtfsrtvalidator.validation.rules;

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

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E038;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E039;

/**
 * E038 - Invalid header.gtfs_realtime_version
 * E039 - FULL_DATASET feeds should not include entity.is_deleted
 */
public class HeaderValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(HeaderValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<OccurrenceModel> errorListE038 = new ArrayList<>();
        List<OccurrenceModel> errorListE039 = new ArrayList<>();

        String version = feedMessage.getHeader().getGtfsRealtimeVersion();
        if (!version.equals("1.0")) {
            // E038 - Invalid header.gtfs_realtime_version
            OccurrenceModel om = new OccurrenceModel("header.gtfs_realtime_version of " + version);
            errorListE038.add(om);
            _log.debug(om.getPrefix() + " " + E038.getOccurrenceSuffix());
        }

        if (feedMessage.getHeader().getIncrementality().equals(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET)) {
            for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
                if (entity.hasIsDeleted()) {
                    // E039 - FULL_DATASET feeds should not include entity.is_deleted
                    OccurrenceModel om = new OccurrenceModel("entity ID " + entity.getId() + " has is_deleted=" + entity.getIsDeleted());
                    errorListE039.add(om);
                    _log.debug(om.getPrefix() + " " + E039.getOccurrenceSuffix());
                }
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListE038.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E038), errorListE038));
        }
        if (!errorListE039.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E039), errorListE039));
        }
        return errors;
    }
}
