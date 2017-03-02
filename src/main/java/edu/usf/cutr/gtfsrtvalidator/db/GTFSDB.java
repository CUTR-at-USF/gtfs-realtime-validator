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

import edu.usf.cutr.gtfsrtvalidator.api.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.hibernate.HibernateUtil;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GTFSDB {

    public static void InitializeDB() {
        Statement stmt;
        String workingDir = System.getProperty("user.dir");
        String createTablePath = workingDir + "/target/classes/createTables.sql";

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(createTablePath));
            String createTableQuerry = new String(encoded, "UTF-8");

            String[] createStatements = createTableQuerry.split(";");

            for (String createStatement : createStatements) {
                Class.forName("org.hsqldb.jdbcDriver");
                Connection con = DriverManager.getConnection("jdbc:hsqldb:file:gtfsrthsql", "sa", "");

                stmt = con.createStatement();
                stmt.executeUpdate(createStatement);
                stmt.close();
                con.close();
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        //Use reflection to get the list of rules from the ValidataionRules class
        Field[] fields = ValidationRules.class.getDeclaredFields();

        Session session = InitSessionBeginTrans();

        List<ValidationRule> rulesInClass = new ArrayList<>();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                Class classType = field.getType();
                if (classType == ValidationRule.class) {
                    ValidationRule rule = new ValidationRule();
                    try {
                        Object value = field.get(rule);
                        rule = (ValidationRule)value;
                        System.out.println(rule.getErrorDescription());
                        rulesInClass.add(rule);
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        try {
            for (ValidationRule rule : rulesInClass) {
                session.update(rule);
            }
            commitAndCloseSession(session);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("Table initialized successfully");
    }
    public static Session InitSessionBeginTrans() {
        Session session = null;
        Transaction tx = null;
        try{
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return session;
    }
    public static void commitAndCloseSession(Session session) {
        Transaction tx = null;
        try{
            session.flush();
            tx = session.getTransaction();
            tx.commit();
        } catch(Exception ex) {
            ex.printStackTrace();
            if(tx != null) tx.rollback();
        } finally {
                if(session != null)
                    session.close();
        }   
    }
}
