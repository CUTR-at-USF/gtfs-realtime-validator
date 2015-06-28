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

public class MonitorLog {
    private String url;
    private int itterations;
    private List<MonitorDetails> monitorDetails = new ArrayList<>();

    //<editor-fold desc="MonitorDetails: Setters and Getters">
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getItterations() {
        return itterations;
    }

    public void setItterations(int itterations) {
        this.itterations = itterations;
    }

    public List<MonitorDetails> getMonitorDetails() {
        return monitorDetails;
    }

    public void setMonitorDetails(List<MonitorDetails> monitorDetails) {
        this.monitorDetails = monitorDetails;
    }
    //</editor-fold>
}


