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

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs.services.HibernateGtfsFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class GTFSHibernate {

    private static final String KEY_CLASSPATH = "classpath:";
    private static final String KEY_FILE = "file:";

    public static GtfsDaoImpl readToDatastore(String saveFilePath) throws IOException {

        GtfsReader reader = new GtfsReader();
        reader.setInputLocation(new File(saveFilePath));

        GtfsDaoImpl store = new GtfsDaoImpl();
        reader.setEntityStore(store);

        reader.run();

        return store;
    }

    public static void saveToDatabase(String saveFilePath) throws IOException {

        String resource = "classpath:edu/usf/cutr/gtfsrtvalidator/db/configuration.xml";

        HibernateGtfsFactory factory = createHibernateGtfsFactory(resource);

        GtfsReader reader = new GtfsReader();

        reader.setInputLocation(new File(saveFilePath));

        GtfsMutableRelationalDao dao = factory.getDao();

        reader.setEntityStore(dao);
        reader.run();

        Collection<Stop> stops = dao.getAllStops();

        for (Stop stop : stops)
            System.out.println(stop.getName());


    }

    private static HibernateGtfsFactory createHibernateGtfsFactory(String resource) {
        Configuration config = new Configuration();

        if (resource.startsWith(KEY_CLASSPATH)) {
            resource = resource.substring(KEY_CLASSPATH.length());
            config = config.configure(resource);
        } else if (resource.startsWith(KEY_FILE)) {
            resource = resource.substring(KEY_FILE.length());
            config = config.configure(new File(resource));
        } else {
            config = config.configure(new File(resource));
        }

        SessionFactory sessionFactory = config.buildSessionFactory();
        return new HibernateGtfsFactory(sessionFactory);
    }
}
