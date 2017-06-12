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
package edu.usf.cutr.gtfsrtvalidator.util;

import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import org.slf4j.LoggerFactory;

import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.W009;

/**
 * Utilities related to particular rules
 */
public class RuleUtils {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(RuleUtils.class);

    /**
     * Adds occurrence for rule W009 - "schedule_relationship not populated" to the provided warnings list
     *
     * @param occurrencePrefix prefix to use for the OccurrenceModel constructor
     * @param warnings         list to add occurence for W009 to
     */
    public static void addW009Occurrence(String occurrencePrefix, List<OccurrenceModel> warnings) {
        OccurrenceModel om = new OccurrenceModel(occurrencePrefix);
        warnings.add(om);
        _log.debug(om.getPrefix() + " " + W009.getOccurrenceSuffix());
    }
}
