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
package edu.usf.cutr.gtfsrtvalidator.test.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.rules.HeaderValidator;
import org.junit.Test;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;

/**
 * Tests for rules implemented in HeaderValidator
 */
public class HeaderValidatorTest extends FeedMessageTest {

    public HeaderValidatorTest() throws Exception {
    }

    /**
     * E038 - Invalid header.gtfs_realtime_version
     */
    @Test
    public void testGtfsRealtimeVersion() {
        HeaderValidator headerValidator = new HeaderValidator();

        GtfsRealtime.FeedHeader.Builder headerBuilder = GtfsRealtime.FeedHeader.newBuilder();

        // Valid version - no errors
        headerBuilder.setGtfsRealtimeVersion("1.0");
        feedMessageBuilder.setHeader(headerBuilder.build());

        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E038, results, 0);

        // Bad version - one error
        headerBuilder.setGtfsRealtimeVersion("2.0");
        feedMessageBuilder.setHeader(headerBuilder.build());

        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E038, results, 1);

        // Bad version - one error
        headerBuilder.setGtfsRealtimeVersion("1");
        feedMessageBuilder.setHeader(headerBuilder.build());

        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E038, results, 1);

        // Bad version - one error
        headerBuilder.setGtfsRealtimeVersion("abcd");
        feedMessageBuilder.setHeader(headerBuilder.build());

        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E038, results, 1);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E039 - FULL_DATASET feeds should not include entity.is_deleted
     */
    @Test
    public void testIncrementalIsDeleted() {
        HeaderValidator headerValidator = new HeaderValidator();

        GtfsRealtime.FeedHeader.Builder headerBuilder = GtfsRealtime.FeedHeader.newBuilder();
        headerBuilder.setGtfsRealtimeVersion("1.0");

        // FULL_DATASET feed without any entities - no errors
        headerBuilder.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET);
        feedMessageBuilder.setHeader(headerBuilder.build());

        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E039, results, 0);

        // DIFFERENTIAL feed without any entities - no errors
        headerBuilder.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL);
        feedMessageBuilder.setHeader(headerBuilder.build());

        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E039, results, 0);

        // FULL_DATASET feed without is_deleted field in any entities - no errors
        headerBuilder.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET);
        feedMessageBuilder.setHeader(headerBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());
        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E039, results, 0);

        // DIFFERENTIAL feed without is_deleted field in any entities - no errors
        headerBuilder.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL);
        feedMessageBuilder.setHeader(headerBuilder.build());
        feedMessageBuilder.addEntity(feedEntityBuilder.build());
        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E039, results, 0);

        // FULL_DATASET feed with is_deleted field in an entity - 1 error
        headerBuilder.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET);
        feedMessageBuilder.setHeader(headerBuilder.build());
        feedEntityBuilder.setIsDeleted(true);
        feedMessageBuilder.addEntity(feedEntityBuilder.build());
        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E039, results, 1);

        // DIFFERENTIAL feed with is_deleted field in an entity - 0 errors
        feedMessageBuilder.clearEntity();
        headerBuilder.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL);
        feedMessageBuilder.setHeader(headerBuilder.build());
        feedEntityBuilder.setIsDeleted(true);
        feedMessageBuilder.addEntity(feedEntityBuilder.build());
        results = headerValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        TestUtils.assertResults(ValidationRules.E039, results, 0);

        clearAndInitRequiredFeedFields();
    }
}
