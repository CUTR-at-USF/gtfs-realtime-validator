/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.usf.cutr.gtfsrtvalidator.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
 
public class HibernateUtil {
    private static SessionFactory sessionFactory = null;  
       
    public static void configureSessionFactory() throws HibernateException {
        // Set jboss logging provider to use slf4j configuration provided in 'simplelogger.properties' file
        System.setProperty("org.jboss.logging.provider", "slf4j");

        sessionFactory = new Configuration().configure().buildSessionFactory();
    }
 
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
 
    public static void shutdown() {
        // Close caches and connection pools
        getSessionFactory().close();
    } 
}