/*
 * Copyright (C) 2011 Nipuna Gunathilake.
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

package edu.usf.cutr.gtfsrtvalidator.lib.model.helper;

import edu.usf.cutr.gtfsrtvalidator.lib.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.OccurrenceModel;

import java.util.List;

public class ErrorListHelperModel {
    private MessageLogModel errorMessage;
    private List<OccurrenceModel> occurrenceList;

    public ErrorListHelperModel(){}

    public ErrorListHelperModel(MessageLogModel errorMessage, List<OccurrenceModel> occurrenceList) {
        this.errorMessage = errorMessage;
        this.occurrenceList = occurrenceList;
    }

    public MessageLogModel getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(MessageLogModel errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<OccurrenceModel> getOccurrenceList() {
        return occurrenceList;
    }

    public void setOccurrenceList(List<OccurrenceModel> occurrenceList) {
        this.occurrenceList = occurrenceList;
    }
}
