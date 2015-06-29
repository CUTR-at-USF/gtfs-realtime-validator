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

import edu.usf.cutr.gtfsrtvalidator.helper.Count;

import java.sql.*;
import java.util.ArrayList;

public class Database {
    static Connection con = null;
    static Statement stmt = null;

    public static void InitializeDB(){
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:test.db");
            System.out.println("Opened database successfully");

            stmt = con.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS COUNT " +
                    "(ID INTEGER PRIMARY KEY NOT NULL," +
                    " Vehicle_Count INTEGER, " +
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

    public static void setCount(int vehicle, int trip) {
        try {
            PreparedStatement stmt;

            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:test.db");
            con.setAutoCommit(false);
            System.out.println("Opened database successfully");



            stmt = con.prepareStatement("INSERT INTO COUNT (Trip_Count, Vehicle_Count) VALUES ( ? , ? );");
            stmt.setInt(1, vehicle);
            stmt.setInt(2, trip);

            stmt.executeUpdate();

            stmt.close();
            con.commit();
            con.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Records created successfully");
    }

    public static ArrayList<Count> getCount() {

        ArrayList<Count> countItems = new ArrayList<>();

        try {
            Statement stmt;

            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:test.db");
            con.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = con.createStatement();
            String sql = "SELECT * FROM COUNT ORDER BY ID DESC LIMIT 11";

            ResultSet rs = stmt.executeQuery(sql);

            while ( rs.next() ) {
                Count count = new Count();

                count.setTripCountCount(rs.getInt("Trip_Count"));
                count.setTripCountCount(rs.getInt("Vehicle_Count"));

                countItems.add(count);
            }

            stmt.close();
            con.commit();
            con.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }

        return countItems;
    }
}
