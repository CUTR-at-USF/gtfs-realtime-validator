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

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@SuppressWarnings("serial")
public class RTFeedValidatorServlet extends HttpServlet {

    private static final int INVALID_FEED = 0;
    private static final int VALID_FEED = 1;


    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        String feedURL = getParameter(request, "gtfsrturl");

        int feedType = checkFeedType(feedURL);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        //Creates simple json object giving the feed type
        //Should be changed to a java object if more complexities occur.
        response.getWriter().println("{\"feedStatus\" : "+ feedType +"}");
    }

    private String getParameter(HttpServletRequest request, String paramName){

        String parameter = "";
        String value = request.getParameter(paramName);

        if (!(value == null || "".equals(value))) {
            parameter = value;

            try {
                parameter = java.net.URLDecoder.decode(parameter, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return parameter;
    }

    private int checkFeedType(String FeedURL) {

        FeedMessage feed;
        try {
            System.out.println(FeedURL);
            URI FeedURI = new URI(FeedURL);
            URL url = FeedURI.toURL();
            feed = FeedMessage.parseFrom(url.openStream());
        } catch (URISyntaxException | IllegalArgumentException | IOException e ) {
            return INVALID_FEED;
        }

        if (feed.hasHeader()) {
            return VALID_FEED;
        }

        return INVALID_FEED;
    }

}

