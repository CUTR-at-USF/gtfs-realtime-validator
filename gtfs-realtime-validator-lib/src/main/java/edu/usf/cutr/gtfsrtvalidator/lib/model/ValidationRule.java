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

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@Entity
@Table(name = "Error")
public class ValidationRule implements Serializable {

    @Id
    @Column(name="errorID")
    private String errorId;

    @Column(name = "severity")
    private String severity;

    @Column(name = "title")
    @Type(type = "text")
    private String title;

    @Column(name="errorDescription")
    @Type(type = "text")
    private String errorDescription;

    /**
     * Text used to help describe an occurrence of this warning/error following the specific elements that it relates to.
     * <p>
     * For example, for E004 "GTFS-rt trip_ids must appear in GTFS data", the error message we want to show the user could be
     * * "trip_id 6234 doesn't appear in the GTFS data"
     * <p>
     * For this message, the occurenceSuffix would be:
     * * "doesn't appear in the GTFS data"
     * <p>
     * And the first part of the text (stored in OccurrenceModel.prefix) would be:
     * * "trip_id 6234"
     *
     * @see OccurrenceModel
     */
    @Column(name = "occurrenceSuffix")
    @Type(type = "text")
    private String occurrenceSuffix;

    public ValidationRule() {
    }

    public ValidationRule(String errorId, String severity, String title, String errorDescription, String occurrenceSuffix) {
        this.setErrorId(errorId);
        this.setSeverity(severity);
        this.setTitle(title);
        this.setErrorDescription(errorDescription);
        this.setOccurrenceSuffix(occurrenceSuffix);
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String errorType) {
        this.severity = errorType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOccurrenceSuffix() {
        return occurrenceSuffix;
    }

    public void setOccurrenceSuffix(String occurenceSuffix) {
        this.occurrenceSuffix = occurenceSuffix;
    }
}
