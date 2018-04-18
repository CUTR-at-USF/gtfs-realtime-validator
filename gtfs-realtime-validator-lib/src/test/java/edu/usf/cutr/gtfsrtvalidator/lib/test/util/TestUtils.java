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
package edu.usf.cutr.gtfsrtvalidator.lib.test.util;

import edu.usf.cutr.gtfsrtvalidator.lib.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Utilities to help with test execution and assertions
 */
public class TestUtils {

    /**
     * Asserts that for a given map of rules to expected number of warnings/errors (expected) and
     * error/warning results (results), there should be a certain number of errors warnings for each rule.  There should
     * be 0 errors/warnings for all other rules not included in the map.  In expected, the key is the
     * ValidationRule, and the value is the number of expected warnings/errors for that rule.
     *
     * @param expected      A map of the ValidationRules to the number of expected warnings/errors for each rule.  If a ValidationRule isn't included in this map, it is expected that there are 0 errors/warnings for that rule.
     * @param results       list of errors or warnings output from validation
     */
    public static void assertResults(@NotNull Map<ValidationRule, Integer> expected, @NotNull List<ErrorListHelperModel> results) {
        if (expected == null) {
            throw new IllegalArgumentException("expected cannot be null - it must be a list of expected errors or warnings");
        }
        if (results == null) {
            throw new IllegalArgumentException("results cannot be null - it must be a list of actual errors or warnings");
        }

        // We need to create a map of actual results to actual count, so we can loop through
        Map<ValidationRule, Integer> actual = new HashMap<>();

        /**
         * First, confirm that all actual count for all rules in results match the expected count
         */
        for (ErrorListHelperModel error : results) {
            // Save the actual count to a map for quick access in the next FOR loop
            ValidationRule rule = error.getErrorMessage().getValidationRule();
            Integer actualCount = error.getOccurrenceList().size();
            actual.put(rule, actualCount);

            // Get the expected count for this rule
            Integer expectedCount = expected.get(rule);
            if (expectedCount != null) {
                // Make sure we have expected number of errors/warnings
                assertEquals(expectedCount, actualCount);
            } else {
                // Make sure there aren't any errors/warnings for this rule, as it wasn't in the expected HashMap
                assertEquals(0, actualCount.intValue());
            }
        }

        /**
         * Second, make sure that all expected counts for all rules in expected match the actual count.  We need this loop
         * to make sure that there isn't an expected rule in expected that's not included in results.
         */
        for (Map.Entry<ValidationRule, Integer> entry : expected.entrySet()) {
            ValidationRule rule = entry.getKey();
            Integer expectedCount = entry.getValue();

            // Get the actual count for this rule
            Integer actualCount = actual.get(rule);
            if (actualCount != null) {
                // Make sure we have expected number of errors/warnings for this rule
                assertEquals(actualCount, expectedCount);
            } else {
                // We're expecting errors/warnings for a rule that wasn't included in the actual results
                fail("Expected " + expectedCount + " occurrences for " + rule.getErrorId() + " but found 0 occurrences in actual results");
            }
        }
    }
}
