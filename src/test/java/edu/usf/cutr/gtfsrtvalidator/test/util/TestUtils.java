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
package edu.usf.cutr.gtfsrtvalidator.test.util;

import edu.usf.cutr.gtfsrtvalidator.api.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Utilities to help with test execution and assertions
 */
public class TestUtils {

    /**
     * Asserts that for a given rule and error/warning results (results), there should be a certain number of
     * results (totalExpectedErrorsWarnings).  There should be 0 results for all other rules.
     *
     * @param rule                        the rule to assert number of results for
     * @param results                     list of errors or warnings output from validation
     * @param totalExpectedErrorsWarnings total number of expected results for the given rule and results
     */
    public static void assertResults(ValidationRule rule, List<ErrorListHelperModel> results, int totalExpectedErrorsWarnings) {
        if (results == null) {
            throw new IllegalArgumentException("results cannot be null - it must be a list of errors or warnings");
        }
        if (results.isEmpty() && totalExpectedErrorsWarnings > 0) {
            throw new IllegalArgumentException("If at least one error is expected results cannot be empty");
        }
        for (ErrorListHelperModel error : results) {
            if (error.getErrorMessage().getValidationRule().getErrorId().equals(rule.getErrorId())) {
                assertEquals(totalExpectedErrorsWarnings, error.getOccurrenceList().size());
            } else {
                assertEquals(0, error.getOccurrenceList().size());
            }
        }
    }
}
