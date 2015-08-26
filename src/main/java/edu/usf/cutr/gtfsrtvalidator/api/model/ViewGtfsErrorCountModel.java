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

public class ViewGtfsErrorCountModel {
    public static final String MESSAGE_ID = "messageID";
    public static final String ITERATION_ID = "itterationID";
    public static final String ERROR_ID = "errorID";
    public static final String ERROR_DESC = "errorDescription";
    public static final String FEED_URL = "feedUrl";
    public static final String FILE_LOCATION = "fileLocation";
    public static final String DOWNLOAD_TIME = "downloadTimestamp";
    public static final String ERROR_COUNT = "errorCount";

    private int messageId;
    private int iterationId;
    private String errorId;
    private String errorDesc;
    private String feedUrl;
    private String fileLocation;
    private long downloadTime;
    private int errorCount;

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

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public long getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(long downloadTime) {
        this.downloadTime = downloadTime;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    @Override
    public String toString() {
        return "ViewGtfsErrorCountModel{" +
                "messageId=" + messageId +
                ", iterationId=" + iterationId +
                ", errorId='" + errorId + '\'' +
                ", errorDesc='" + errorDesc + '\'' +
                ", feedUrl='" + feedUrl + '\'' +
                ", fileLocation='" + fileLocation + '\'' +
                ", downloadTime=" + downloadTime +
                ", errorCount=" + errorCount +
                '}';
    }
}
