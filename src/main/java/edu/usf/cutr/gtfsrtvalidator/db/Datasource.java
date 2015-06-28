/*
 * **********************************************************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this fileexcept in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * **********************************************************************************************************************
 */

package edu.usf.cutr.gtfsrtvalidator.db;


import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.log4j.Logger;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Datasource {

    /**
     * A singleton that represents a pooled datasource. It is composed of a C3PO
     * pooled datasource. Can be changed to any connect pool provider
     */

    private ComboPooledDataSource cpds;
    private static Datasource datasource;
    private static Logger log = Logger.getLogger(Datasource.class);

    private Datasource() throws IOException, SQLException, PropertyVetoException {
        // load datasource properties
        log.info("Reading datasource.properties from classpath");

        cpds = new ComboPooledDataSource();
        cpds.setDriverClass("org.sqlite.JDBC");
        cpds.setJdbcUrl("jdbc:sqlite:gtfsrt.db");
        cpds.setInitialPoolSize(10);
        cpds.setAcquireIncrement(10);
        cpds.setMaxPoolSize(200);
        cpds.setMinPoolSize(10);
        cpds.setMaxStatements(200);

        Connection testConnection = null;
        Statement testStatement = null;

        // test connectivity and initialize pool
        try {
            testConnection = cpds.getConnection();
            testStatement = testConnection.createStatement();
            testStatement.executeQuery("select 1+1 from FEED_DETAILS");
        } catch (SQLException e) {
            throw e;
        } finally {
            testStatement.close();
            testConnection.close();
        }

    }

    public static Datasource getInstance() throws IOException, SQLException, PropertyVetoException {
        if (datasource == null) {
            datasource = new Datasource();
            return datasource;
        } else {
            return datasource;
        }
    }

    public Connection getConnection() throws SQLException {
        return this.cpds.getConnection();
    }
}
