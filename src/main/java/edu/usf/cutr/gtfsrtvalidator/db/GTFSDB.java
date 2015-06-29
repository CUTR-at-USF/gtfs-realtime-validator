/*
 * Copyright (C) 2015 Nipuna Gunathilake.
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

package edu.usf.cutr.gtfsrtvalidator.db;

import edu.usf.cutr.gtfsrtvalidator.json.MonitorDetails;
import edu.usf.cutr.gtfsrtvalidator.json.MonitorLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GTFSDB {
    static Statement stmt = null;

    public static void InitializeDB(){
        try {
            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            System.out.println("Opened database successfully");

            stmt = con.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS FEED_DETAILS " +
                    "(ID INTEGER PRIMARY KEY NOT NULL," +
                    " Time_Stamp INTEGER, " +
                    " Vehicle_Count INTEGER, " +
                    " Alert_Count INTEGER, " +
                    " Feed_Url TEXT, " +
                    " Trip_Count INTEGER)";
            stmt.executeUpdate(sql);
            stmt.close();
            con.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Table created successfully");
    }

    public static synchronized void setFeedDetails(String url, int vehicle, int trip, int alert) {
        try {
            PreparedStatement stmt;

            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            Date currentDate = new Date();
            long unixTIme = currentDate.getTime() / 1000;

            stmt = con.prepareStatement("INSERT INTO FEED_DETAILS (Time_Stamp, Trip_Count, Alert_Count, Vehicle_Count, Feed_Url) VALUES ( ? , ? , ? , ?, ?);");
            stmt.setLong(1, unixTIme);
            stmt.setInt(2, vehicle);
            stmt.setInt(3, alert);
            stmt.setInt(4, trip);
            stmt.setString(5, url);

            stmt.executeUpdate();

            stmt.close();
            con.commit();
            con.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    public static synchronized MonitorLog getFeedDetails(String feedUrl) {
        return getFeedDetails(feedUrl, 11);
    }

    public static synchronized MonitorLog getFeedDetails(String feedUrl, int limit) {

        MonitorLog monitorLog = new MonitorLog();
        List<MonitorDetails> monitorDetails = new ArrayList<>();

        try {
            Datasource ds = Datasource.getInstance();
            Connection con = ds.getConnection();
            con.setAutoCommit(false);

            PreparedStatement stmt;

            String sql = "SELECT * FROM FEED_DETAILS WHERE Feed_Url=? ORDER BY Time_Stamp DESC LIMIT ?;";
            stmt = con.prepareStatement(sql);

            stmt.setString(1, feedUrl);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();

            while ( rs.next() ) {
                MonitorDetails monitor = new MonitorDetails();

                monitor.setVehicleCount(rs.getInt("Vehicle_Count"));
                monitor.setUpdateCount(rs.getInt("Trip_Count"));
                monitor.setAlertCount(rs.getInt("Alert_Count"));

                monitor.setTimestamp(rs.getLong("Time_Stamp"));

                monitorDetails.add(monitor);
            }

            monitorLog.setMonitorDetails(monitorDetails);

            stmt.close();
            con.commit();
            con.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }

        return monitorLog;
    }
}
