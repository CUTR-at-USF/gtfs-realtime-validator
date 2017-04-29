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
import edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

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
        assertEquals(true, TimestampUtils.isValidDateFormat(validDate));

        validDate = "20170427";
        assertEquals(true, TimestampUtils.isValidDateFormat(validDate));

        /**
         * Bad dates
         */
        String badDate = "2017011";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "2017/01/01";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "01/01/2017";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "01-01-2017";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "01012017";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "13012017";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "20171301";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "abcdefgh";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "12345678";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));

        badDate = "2017.01.01";
        assertEquals(false, TimestampUtils.isValidDateFormat(badDate));
    }

    @Test
    public void testTimeFormat() {
        /**
         * Good times
         */
        String validTime = "00:00:00";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        validTime = "02:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        validTime = "22:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        // Time can exceed 24 hrs if service goes into the next service day
        validTime = "25:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        // Time can exceed 24 hrs if service goes into the next service day
        validTime = "29:15:35";
        assertEquals(true, TimestampUtils.isValidTimeFormat(validTime));

        /**
         * Bad times
         */
        String badTime = "5:15:35";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        // Anything of 29hrs will currently fail validation
        badTime = "30:15:35";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        badTime = "12345678";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        badTime = "abcdefgh";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        badTime = "05:5:35";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));

        badTime = "05:05:5";
        assertEquals(false, TimestampUtils.isValidTimeFormat(badTime));
    }

    @Test
    public void testSecondsAfterMidnightToClock() {
        int time;
        String clockTime;

        time = 59;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("00:00:59", clockTime);

        time = 1200;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("00:20:00", clockTime);

        time = 1250;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("00:20:50", clockTime);

        time = 21600;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("06:00:00", clockTime);

        time = 21901;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("06:05:01", clockTime);

        time = 86399;  // Seconds after midnight
        clockTime = TimestampUtils.secondsAfterMidnightToClock(time);
        assertEquals("23:59:59", clockTime);

    }

    @Test
    public void testPosixToClock() {
        int time = 1493383886;  // POSIX time
        String timeZoneText = "America/New_York";
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneText);
        String clockTime = TimestampUtils.posixToClock(time, timeZone);
        assertEquals("08:51:26", clockTime);
    }
}
