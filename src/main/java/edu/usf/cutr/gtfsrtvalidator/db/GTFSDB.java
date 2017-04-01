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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class GTFSDB {

    private static final Logger _log = LoggerFactory.getLogger(GTFSDB.class);

    public static void InitializeDB() {

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
                        rulesInClass.add(rule);
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        try {
            for (ValidationRule rule : rulesInClass) {
                session.saveOrUpdate(rule);
            }
            commitAndCloseSession(session);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        _log.info("Table initialized successfully");
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
