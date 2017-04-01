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

    public ValidationRule() {}

    public ValidationRule(String errorId, String severity, String title, String errorDescription) {
        this.setErrorId(errorId);
        this.setSeverity(severity);
        this.setTitle(title);
        this.setErrorDescription(errorDescription);
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
}
