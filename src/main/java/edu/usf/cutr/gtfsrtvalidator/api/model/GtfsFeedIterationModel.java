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
    public GtfsFeedIterationModel(long timeStamp, byte[] feedprotobuf, int rtFeedId) {
        this.timeStamp = timeStamp;
        Feedprotobuf = feedprotobuf;
        this.rtFeedId = rtFeedId;
    }

    public GtfsFeedIterationModel(){};

    public static String ITERATIONID = "IterationId";
    public static String ITERATIONTIMESTAMP = "IterationTimestamp";
    public static String FEEDPROTOBUF = "feedProtobuf";
    public static String RTFEEDID = "rtFeedID";

    private int IterationId;
    private long timeStamp;
    private byte[] Feedprotobuf;
    private int rtFeedId;

    public int getRtFeedId() {
        return rtFeedId;
    }

    public void setRtFeedId(int rtFeedId) {
        this.rtFeedId = rtFeedId;
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

    public byte[] getFeedprotobuf() {
        return Feedprotobuf;
    }

    public void setFeedprotobuf(byte[] feedprotobuf) {
        Feedprotobuf = feedprotobuf;
    }
}
