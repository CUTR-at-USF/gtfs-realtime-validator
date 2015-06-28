/*
 ***********************************************************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************
*/

package edu.usf.cutr.gtfsrtvalidator.servlets;

import com.google.gson.Gson;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.json.MonitorLog;
import edu.usf.cutr.gtfsrtvalidator.json.StatusMessage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class FeedInfoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        Gson gson = new Gson();
        String json;

        response.setContentType("application/json");
        PrintWriter pw = response.getWriter();

        String parameter = "";
        String feedUrl = request.getParameter("gtfsurl");

        if (feedUrl == "" || feedUrl == null) {
            StatusMessage error = new StatusMessage(1, "GTFS Feed Url not provided");
            json = gson.toJson(error);
        } else {
            MonitorLog details = GTFSDB.getFeedDetails(feedUrl);
            json = gson.toJson(details);
        }

        pw.write(json);
    }
}
