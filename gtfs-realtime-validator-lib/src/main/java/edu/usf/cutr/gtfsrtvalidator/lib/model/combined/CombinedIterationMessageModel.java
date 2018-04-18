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

package edu.usf.cutr.gtfsrtvalidator.lib.model.combined;

import edu.usf.cutr.gtfsrtvalidator.lib.model.GtfsRtFeedIterationString;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class CombinedIterationMessageModel {

    public CombinedIterationMessageModel() {}

    private GtfsRtFeedIterationString gtfsFeedIterationModel;
    private List<CombinedMessageOccurrenceModel> messageOccurrenceList;

    public GtfsRtFeedIterationString getGtfsFeedIterationModel() {
        return gtfsFeedIterationModel;
    }

    public void setGtfsFeedIterationModel(GtfsRtFeedIterationString gtfsFeedIterationModel) {
        this.gtfsFeedIterationModel = gtfsFeedIterationModel;
    }

    public List<CombinedMessageOccurrenceModel> getMessageOccurrenceList() {
        return messageOccurrenceList;
    }

    public void setMessageOccurrenceList(List<CombinedMessageOccurrenceModel> messageOccurrenceList) {
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

