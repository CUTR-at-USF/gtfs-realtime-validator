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

package edu.usf.cutr.gtfsrtvalidator.lib.model;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@Entity
@Table(name="MessageLog")
public class MessageLogModel implements Serializable {

    public MessageLogModel(){};

    public MessageLogModel(ValidationRule validationRule) {
        this.setValidationRule(validationRule);
    }

    @Id
    @Column(name="messageID")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int messageId;
    @ManyToOne
    @JoinColumn(name = "iterationID")
    private GtfsRtFeedIterationModel gtfsRtFeedIterationModel;
    @ManyToOne
    @JoinColumn(name = "errorID")
    private ValidationRule validationRule;
    @Column(name = "errorDetails")
    private String errorDetails;

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public GtfsRtFeedIterationModel getGtfsRtFeedIterationModel() {
        return gtfsRtFeedIterationModel;
    }

    public void setGtfsRtFeedIterationModel(GtfsRtFeedIterationModel gtfsRtFeedIterationModel) {
        this.gtfsRtFeedIterationModel = gtfsRtFeedIterationModel;
    }

    public ValidationRule getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(ValidationRule validationRule) {
        this.validationRule = validationRule;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}