package org.opentripplanner.gtfsrtvalidator.servlets;

import com.google.transit.realtime.GtfsRealtime.*;

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
    private static final int TRIP_FEED = 1;
    private static final int ALERT_FEED = 2;
    private static final int UPDATE_FEED = 3;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        String feedURL = getParamter(request, "gtfsrturl");

        int feedType = checkFeedType(feedURL);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        //Creates simple json object giving the feed type
        //Should be changed to a java object if more complexities occur.
        response.getWriter().println("{\"feedStatus\" : "+ feedType +"}");
    }

    private String getParamter(HttpServletRequest request, String paramName){

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
        System.out.println(parameter);
        return parameter;
    }

    private int checkFeedType(String FeedURL) {

        FeedMessage feed;
        try {
            URI FeedURI = new URI(FeedURL);
            URL url = FeedURI.toURL();
            feed = FeedMessage.parseFrom(url.openStream());
        } catch (URISyntaxException | IllegalArgumentException | IOException e ) {
            return INVALID_FEED;
        }

        FeedEntity  firstItem;

        try {
            firstItem = feed.getEntity(0);
        } catch (ArrayIndexOutOfBoundsException e) {
            //Empty feed
            return INVALID_FEED;
        }

        if (firstItem.hasTripUpdate()) {
            return TRIP_FEED;
        } else if (firstItem.hasAlert()) {
            return ALERT_FEED;
        } else if (firstItem.hasVehicle()) {
            return UPDATE_FEED;
        }else {
            return INVALID_FEED;
        }
    }

}

