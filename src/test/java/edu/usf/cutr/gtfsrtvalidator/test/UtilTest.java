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
package edu.usf.cutr.gtfsrtvalidator.test;

import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;

/**
 * Tests the utility meethods used in tests
 */
public class UtilTest {

    @Test
    public void testAssertResults() {
        MessageLogModel modelE001 = new MessageLogModel(ValidationRules.E001);
        OccurrenceModel errorE001 = new OccurrenceModel(String.valueOf(MIN_POSIX_TIME));
        List<OccurrenceModel> errorListE001 = new ArrayList<>();


        List<ErrorListHelperModel> results = new ArrayList<>();
        // Test empty list of error results
        TestUtils.assertResults(ValidationRules.E001, results, 0);

        // Test list of error results, but without a MessageLogModel
        results.add(new ErrorListHelperModel(modelE001, errorListE001));
        TestUtils.assertResults(ValidationRules.E001, results, 0);

        // Test list of error results, with one MessageLogModel
        errorListE001.add(errorE001);
        TestUtils.assertResults(ValidationRules.E001, results, 1);

        // Test list of error results, with two MessageLogModels
        errorListE001.add(errorE001);
        TestUtils.assertResults(ValidationRules.E001, results, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssertResultsThrowException() {
        // Make sure we throw an exception if the results list is null
        TestUtils.assertResults(ValidationRules.E001, null, 0);
    }
}
