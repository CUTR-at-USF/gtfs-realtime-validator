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

import java.util.Arrays;

public class ViewMessageDetailsModel {
    public static final String FEED_PROTOCOL_BUFFER = "feedProtobuf";
    public static final String MESSAGE_ID = "messageId";
    public static final String ITERATION_ID = "itterationId";
    public static final String ERROR_ID = "errorId";
    public static final String ERROR_DESC = "errorDescription";
    public static final String OCCURRENCE_ID = "occurrenceId";
    public static final String ELEMENT_PATH = "elementPath";
    public static final String ELEMENT_VALUE = "elementValue";

    private byte[] feedProtobuf;
    private int messageId;
    private int iterationId;
    private String errorId;
    private String errorDescription;
    private int occurrenceId;
    private String elementPath;
    private String elementValue;

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getIterationId() {
        return iterationId;
    }

    public void setIterationId(int iterationId) {
        this.iterationId = iterationId;
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public int getOccurrenceId() {
        return occurrenceId;
    }

    public void setOccurrenceId(int occurrenceId) {
        this.occurrenceId = occurrenceId;
    }

    public String getElementPath() {
        return elementPath;
    }

    public void setElementPath(String elementPath) {
        this.elementPath = elementPath;
    }

    public String getElementValue() {
        return elementValue;
    }

    public void setElementValue(String elementValue) {
        this.elementValue = elementValue;
    }

    public byte[] getFeedProtobuf() {
        return feedProtobuf;
    }

    public void setFeedProtobuf(byte[] feedProtobuf) {
        this.feedProtobuf = feedProtobuf;
    }

    @Override
    public String toString() {
        return "MessageDetailsModel{" +
                "feedProtobuf=" + Arrays.toString(feedProtobuf) +
                ", messageId=" + messageId +
                ", iterationId=" + iterationId +
                ", errorId='" + errorId + '\'' +
                ", errorDescription='" + errorDescription + '\'' +
                ", occurrenceId=" + occurrenceId +
                ", elementPath='" + elementPath + '\'' +
                ", elementValue='" + elementValue + '\'' +
                '}';
    }
}
