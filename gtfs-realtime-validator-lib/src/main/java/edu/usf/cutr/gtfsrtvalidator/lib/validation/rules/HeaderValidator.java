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
package edu.usf.cutr.gtfsrtvalidator.lib.validation.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.RuleUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.*;

/**
 * E038 - Invalid header.gtfs_realtime_version
 * E039 - FULL_DATASET feeds should not include entity.is_deleted
 * E049 - header incrementality not populated
 */
public class HeaderValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(HeaderValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
        List<OccurrenceModel> errorListE038 = new ArrayList<>();
        List<OccurrenceModel> errorListE039 = new ArrayList<>();
        List<OccurrenceModel> errorListE049 = new ArrayList<>();

        if (!GtfsUtils.isValidVersion(feedMessage.getHeader())) {
            // E038 - Invalid header.gtfs_realtime_version
            RuleUtils.addOccurrence(E038, "header.gtfs_realtime_version of " + feedMessage.getHeader().getGtfsRealtimeVersion(), errorListE038, _log);
        }

        try {
            if (GtfsUtils.isV2orHigher(feedMessage.getHeader()) && !feedMessage.getHeader().hasIncrementality()) {
                // E049 - header incrementality not populated
                RuleUtils.addOccurrence(E049, "", errorListE049, _log);
            }
        } catch (Exception e) {
            _log.error("Error checking header version for E049: " + e);
        }

        if (feedMessage.getHeader().getIncrementality().equals(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET)) {
            for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
                if (entity.hasIsDeleted()) {
                    // E039 - FULL_DATASET feeds should not include entity.is_deleted
                    RuleUtils.addOccurrence(E039, "entity ID " + entity.getId() + " has is_deleted=" + entity.getIsDeleted(), errorListE039, _log);
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
        if (!errorListE049.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E049), errorListE049));
        }
        return errors;
    }
}
