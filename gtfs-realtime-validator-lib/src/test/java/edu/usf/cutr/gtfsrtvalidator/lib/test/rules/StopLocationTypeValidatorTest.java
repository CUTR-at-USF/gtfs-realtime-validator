/*
 * Copyright (C) 2017 University of South Florida
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
package edu.usf.cutr.gtfsrtvalidator.lib.test.rules;

import edu.usf.cutr.gtfsrtvalidator.lib.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.lib.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.gtfs.StopLocationTypeValidator;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.E010;
import static org.junit.Assert.assertEquals;

/**
 * Tests for rules implemented in StopLocationTypeValidator
 */
public class StopLocationTypeValidatorTest extends FeedMessageTest {

    public StopLocationTypeValidatorTest() throws IOException {
    }

    /**
     * E010 - If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0
     */
    @Test
    public void testE010() {
        StopLocationTypeValidator stopLocationValidator = new StopLocationTypeValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        // gtfsData does not contain location_type = 1 for stop_id. Therefore returns 0 results
        results = stopLocationValidator.validate(gtfsData);
        for (ErrorListHelperModel error : results) {
            assertEquals(0, error.getOccurrenceList().size());
        }

        // gtfsData2 contains location_type = 1 for stop_ids. Therefore returns errorcount = (number of location_type = 1 for stop_ids)
        results = stopLocationValidator.validate(gtfsData2);
        expected.put(E010, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }
}
