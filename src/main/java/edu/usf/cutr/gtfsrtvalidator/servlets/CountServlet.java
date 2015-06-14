package edu.usf.cutr.gtfsrtvalidator.servlets;

import com.google.gson.Gson;
import edu.usf.cutr.gtfsrtvalidator.db.Database;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by nipuna on 6/14/15.
 */

public class CountServlet extends HttpServlet {

    public URL _tripUpdatesUrl;
    public URL _vehiclePositionsUrl;

    public CountServlet() {
        try {
            _vehiclePositionsUrl = new URL("http://developer.mbta.com/lib/GTRTFS/Alerts/VehiclePositions.pb");
            _tripUpdatesUrl = new URL("http://developer.mbta.com/lib/GTRTFS/Alerts/TripUpdates.pb");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{

        response.setContentType("application/json");
        PrintWriter pw = response.getWriter();

        Gson gson = new Gson();

        String json = gson.toJson(Database.getCount());

        pw.write(json);
    }
}
