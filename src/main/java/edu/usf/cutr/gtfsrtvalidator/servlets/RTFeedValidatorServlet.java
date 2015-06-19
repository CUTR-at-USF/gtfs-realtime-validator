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
            System.out.println("Exception");
            return INVALID_FEED;
        }

        if (feed.hasHeader()) {
            System.out.println("");
            return VALID_FEED;
        }

        System.out.println("Default");
        return INVALID_FEED;
    }

}

