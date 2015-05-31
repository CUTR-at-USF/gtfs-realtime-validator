package me.nipuna777.gtfsrtvalidator.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by nipuna on 5/31/15.
 */
@SuppressWarnings("serial")
public class DateServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        req.getRequestDispatcher("/test/tag2.jsp").forward(req,resp);
    }
}
