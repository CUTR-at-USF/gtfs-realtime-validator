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

package edu.usf.cutr.gtfsrtvalidator.api.model.combined;

import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsFeedIterationModel;

import java.util.List;

public class IterationMessageModel {
    private GtfsFeedIterationModel gtfsFeedIterationModel;
    private List<MessageOccurrenceModel> messageOccurrenceList;

    public GtfsFeedIterationModel getGtfsFeedIterationModel() {
        return gtfsFeedIterationModel;
    }

    public void setGtfsFeedIterationModel(GtfsFeedIterationModel gtfsFeedIterationModel) {
        this.gtfsFeedIterationModel = gtfsFeedIterationModel;
    }

    public List<MessageOccurrenceModel> getMessageOccurrenceList() {
        return messageOccurrenceList;
    }

    public void setMessageOccurrenceList(List<MessageOccurrenceModel> messageOccurrenceList) {
        this.messageOccurrenceList = messageOccurrenceList;
    }

    @Override
    public String toString() {
        return "IterationMessageModel{" +
                "gtfsFeedIterationModel=" + gtfsFeedIterationModel +
                ", messageOccurrenceList=" + messageOccurrenceList +
                '}';
    }
}

/*
gtfsIteration{
        gtfsId:
        gtfsUrl:
        protobuf:

        errorMessage[{
        messageId:
        errorId:
        errorDescription:

        errorOccurences[{
        occurenceId:
        occurencePath:
        occurenceValue:
        }]
        }]
}*/
