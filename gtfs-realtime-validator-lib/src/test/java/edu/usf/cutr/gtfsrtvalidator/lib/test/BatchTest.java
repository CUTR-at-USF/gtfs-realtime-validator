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
package edu.usf.cutr.gtfsrtvalidator.lib.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.usf.cutr.gtfsrtvalidator.lib.batch.BatchProcessor;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the batch processing validation mode
 */
public class BatchTest {

    @Test
    public void testBatchProcessing() throws IOException, NoSuchAlgorithmException {
        // Run batch validation on the bundled USF Bull Runner GTFS and GTFS-realtime data
        BatchProcessor.Builder builder = new BatchProcessor.Builder("src/test/resources/bullrunner-gtfs.zip", "src/test/resources/");
        BatchProcessor processor = builder.build();
        processor.processFeeds();

        // Read in validation results for GTFS-realtime bullrunner-vehicle-positions file
        ObjectMapper mapper = new ObjectMapper();
        ErrorListHelperModel[] allErrorLists = mapper.readValue(new File("src/test/resources/bullrunner-vehicle-positions" + BatchProcessor.RESULTS_FILE_EXTENSION), ErrorListHelperModel[].class);

        // We should have 3 warnings - W001, W006, and W009, with 10 occurrences each.
        // If running on Travis we may have another warning, W008, due to timestamp being older than 65 seconds
        assertTrue(allErrorLists.length == 3 || allErrorLists.length == 4);
        for (ErrorListHelperModel model : allErrorLists) {
            switch (model.getErrorMessage().getValidationRule().getErrorId()) {
                case "W001":
                    assertEquals(10, model.getOccurrenceList().size());
                    break;
                case "W006":
                    assertEquals(10, model.getOccurrenceList().size());
                    break;
                case "W008":
                    assertEquals(1, model.getOccurrenceList().size());
                    break;
                case "W009":
                    assertEquals(10, model.getOccurrenceList().size());
                    break;
            }
        }
    }
}
