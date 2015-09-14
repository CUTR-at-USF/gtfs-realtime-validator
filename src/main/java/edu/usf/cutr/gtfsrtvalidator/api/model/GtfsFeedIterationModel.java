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

package edu.usf.cutr.gtfsrtvalidator.api.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GtfsFeedIterationModel {
    public GtfsFeedIterationModel(int iterationId, long timeStamp, int feedId) {
        this.setIterationId(iterationId);
        this.setTimeStamp(timeStamp);
        this.setFeedId(feedId);
    }

    public GtfsFeedIterationModel(){};

    public static String ITERATIONID = "IterationId";
    public static String ITERATIONTIMESTAMP = "IterationTimestamp";
    public static String FEEDID = "feedID";

    private int IterationId;
    private long timeStamp;
    private int feedId;

    public int getFeedId() {
        return feedId;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    public int getIterationId() {
        return IterationId;
    }

    public void setIterationId(int iterationId) {
        IterationId = iterationId;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

}
