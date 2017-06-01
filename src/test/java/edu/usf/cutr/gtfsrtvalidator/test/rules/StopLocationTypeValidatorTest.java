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
package edu.usf.cutr.gtfsrtvalidator.test.rules;

import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.gtfs.StopLocationTypeValidator;
import org.junit.Test;

import java.io.IOException;

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
    public void testLocationTypeValidation() {
        StopLocationTypeValidator stopLocationValidator = new StopLocationTypeValidator();

        // gtfsData does not contain location_type = 1 for stop_id. Therefore returns 0 results
        results = stopLocationValidator.validate(gtfsData);
        for (ErrorListHelperModel error : results) {
            assertEquals(0, error.getOccurrenceList().size());
        }

        // gtfsData2 contains location_type = 1 for stop_ids. Therefore returns errorcount = (number of location_type = 1 for stop_ids)
        results = stopLocationValidator.validate(gtfsData2);
        TestUtils.assertResults(ValidationRules.E010, results, 1);

        clearAndInitRequiredFeedFields();
    }
}
