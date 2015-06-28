/*
 * **********************************************************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this fileexcept in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * **********************************************************************************************************************
 */

package edu.usf.cutr.gtfsrtvalidator.json;

import java.util.ArrayList;
import java.util.List;

public class MonitorDetails {
    private int itteration;
    private long timestamp;

    private int vehicleCount;
    private int alertCount;
    private int updateCount;

    private List<ProblemDetails> warnings = new ArrayList<>();
    private List<ProblemDetails> errors = new ArrayList<>();

    private class ProblemDetails {
        private int problemID;
        private String problemDetails;

        //<editor-fold desc="ProblemDetails: Setters and Getters">
        public String getProblemDetails() {
            return problemDetails;
        }

        public void setProblemDetails(String problemDetails) {
            this.problemDetails = problemDetails;
        }

        public int getProblemID() {
            return problemID;
        }

        public void setProblemID(int problemID) {
            this.problemID = problemID;
        }
        //</editor-fold>
    }

    //<editor-fold desc="MonitorDetails: Setters and Getters">
    public int getItteration() {
        return itteration;
    }

    public void setItteration(int itteration) {
        this.itteration = itteration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }

    public void setVehicleCount(int vehicleCount) {
        this.vehicleCount = vehicleCount;
    }

    public int getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(int alertCount) {
        this.alertCount = alertCount;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public List<ProblemDetails> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ProblemDetails> warnings) {
        this.warnings = warnings;
    }

    public List<ProblemDetails> getErrors() {
        return errors;
    }

    public void setErrors(List<ProblemDetails> errors) {
        this.errors = errors;
    }
    //</editor-fold>
}