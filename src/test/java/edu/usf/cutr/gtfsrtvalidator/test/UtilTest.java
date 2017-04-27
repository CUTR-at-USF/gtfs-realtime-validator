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
import edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static org.junit.Assert.assertEquals;

/**
 * Test utility methods
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
    public void testAssertResultsThrowExceptionNullResults() {
        // Make sure we throw an exception if the results list is null
        TestUtils.assertResults(ValidationRules.E001, null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssertResultsThrowExceptionEmptyResults() {
        // Make sure we throw an exception if the results list is empty but we expect at least one error
        TestUtils.assertResults(ValidationRules.E001, new ArrayList<>(), 1);
    }

    @Test
    public void testGetAge() {
        long currentTimeMillis = 1104537600000L;
        long headerTimestampSec = 1104527600L;

        long age = TimestampUtils.getAge(currentTimeMillis, headerTimestampSec);
        assertEquals(10000000L, age);
    }

    @Test
    public void testDateFormat() {
        /**
         * Good dates
         */
        String validDate = "20170101";
        assertEquals(true, GtfsUtils.isValidDateFormat(validDate));

        validDate = "20170427";
        assertEquals(true, GtfsUtils.isValidDateFormat(validDate));

        /**
         * Bad dates
         */
        String badDate = "2017011";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "2017/01/01";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "01/01/2017";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "01-01-2017";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "01012017";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "13012017";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "20171301";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "abcdefgh";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "12345678";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));

        badDate = "2017.01.01";
        assertEquals(false, GtfsUtils.isValidDateFormat(badDate));
    }

    @Test
    public void testTimeFormat() {
        /**
         * Good times
         */
        String validTime = "00:00:00";
        assertEquals(true, GtfsUtils.isValidTimeFormat(validTime));

        validTime = "02:15:35";
        assertEquals(true, GtfsUtils.isValidTimeFormat(validTime));

        validTime = "22:15:35";
        assertEquals(true, GtfsUtils.isValidTimeFormat(validTime));

        // Time can exceed 24 hrs if service goes into the next service day
        validTime = "25:15:35";
        assertEquals(true, GtfsUtils.isValidTimeFormat(validTime));

        // Time can exceed 24 hrs if service goes into the next service day
        validTime = "29:15:35";
        assertEquals(true, GtfsUtils.isValidTimeFormat(validTime));

        /**
         * Bad times
         */
        String badTime = "5:15:35";
        assertEquals(false, GtfsUtils.isValidTimeFormat(badTime));

        // Anything of 29hrs will currently fail validation
        badTime = "30:15:35";
        assertEquals(false, GtfsUtils.isValidTimeFormat(badTime));

        badTime = "12345678";
        assertEquals(false, GtfsUtils.isValidTimeFormat(badTime));

        badTime = "abcdefgh";
        assertEquals(false, GtfsUtils.isValidTimeFormat(badTime));

        badTime = "05:5:35";
        assertEquals(false, GtfsUtils.isValidTimeFormat(badTime));

        badTime = "05:05:5";
        assertEquals(false, GtfsUtils.isValidTimeFormat(badTime));
    }
}
