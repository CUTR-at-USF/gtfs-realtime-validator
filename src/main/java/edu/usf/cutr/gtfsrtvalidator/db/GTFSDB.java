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

import edu.usf.cutr.gtfsrtvalidator.hibernate.HibernateUtil;
import edu.usf.cutr.gtfsrtvalidator.lib.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GTFSDB {

    private static final Logger _log = LoggerFactory.getLogger(GTFSDB.class);

    public static void initializeDB() {
        Session session = initSessionBeginTrans();
        List<ValidationRule> rules = ValidationRules.getRules();
        try {
            for (ValidationRule rule : rules) {
                session.saveOrUpdate(rule);
            }
            commitAndCloseSession(session);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        _log.info("Table initialized successfully");
    }

    public static Session initSessionBeginTrans() {
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

    /**
     * Closes a session opened for an UPDATE operation or single READ-ONLY operation
     * @param session session to be committed and closed
     */
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

    /**
     * Closes a session used for multiple READ-ONLY operations -
     * see https://github.com/CUTR-at-USF/gtfs-realtime-validator/pull/135#discussion_r113005572.
     *
     * @param session session to be closed
     */
    public static void closeSession(Session session) {
        if(session != null) {
            session.close();
        }
    }
}
