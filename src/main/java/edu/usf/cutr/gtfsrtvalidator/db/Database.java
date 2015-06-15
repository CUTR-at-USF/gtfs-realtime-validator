package edu.usf.cutr.gtfsrtvalidator.db;

import edu.usf.cutr.gtfsrtvalidator.helper.Count;

import java.sql.*;
import java.util.ArrayList;

/**
 * Created by nipuna on 6/14/15.
 */
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
