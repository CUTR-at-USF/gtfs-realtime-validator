package edu.usf.cutr.gtfsrtvalidator.servlets;

import edu.usf.cutr.gtfsrtvalidator.background.BackgroundSingleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by nipuna on 6/17/15.
 */
public class TriggerBackgroundServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BackgroundSingleton monitorTask = BackgroundSingleton.getInstance();
        monitorTask.StartBackgrounProcess();
    }

}
