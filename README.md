# GTFS-realtime Validator [![Build Status](https://travis-ci.org/CUTR-at-USF/gtfs-realtime-validator.svg?branch=master)](https://travis-ci.org/CUTR-at-USF/gtfs-realtime-validator) [![Join the GTFS-realtime chat](https://gtfs.herokuapp.com/badge.svg)](https://gtfs.herokuapp.com/)

A tool that validates [General Transit Feed Specification (GTFS)-realtime](https://developers.google.com/transit/gtfs-realtime/) feeds


<img src="https://cloud.githubusercontent.com/assets/928045/25874575/2afaa3b0-34e1-11e7-92a4-b0a68f233748.png" width="1000">


## Quick start

*Please note that this project is still under active development and is in an early alpha state.  It has a limited number of rules and the user interface to view warnings/errors is still under development.*

1. Install [Java Development Kit (JDK) 1.8 or higher](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html)
2. Download the latest alpha build:
    * [gtfs-rt-validator-1.0.0-SNAPSHOT.jar](https://s3.amazonaws.com/gtfs-rt-validator/travis_builds/gtfs-rt-validator-1.0.0-SNAPSHOT.jar)
3. From the command line run `java -Djsse.enableSNIExtension=false -jar gtfs-rt-validator-1.0.0-SNAPSHOT.jar`
4. When prompted, in your browser go to `http://localhost:8080`
5. Enter your [General Transit Feed Specification (GTFS)-realtime](https://developers.google.com/transit/gtfs-realtime/) and [GTFS](https://developers.google.com/transit/gtfs/) feed URLs and click "Start".  Example feeds:
    * HART (Tampa, FL)
        * GTFS-realtime - http://api.tampa.onebusaway.org:8088/trip-updates
        * GTFS - http://gohart.org/google/google_transit.zip
    * MBTA (Boston, MA)
        * GTFS-realtime - http://developer.mbta.com/lib/GTRTFS/Alerts/TripUpdates.pb
        * GTFS - http://www.mbta.com/uploadedfiles/MBTA_GTFS.zip
    * ...more at [Transitfeeds.com](http://transitfeeds.com/search?q=gtfsrt) and [Transitland Feed Registry](https://transit.land/feed-registry/)

Please note that if you're using `https` URLS, you'll need to use the `-Djsse.enableSNIExtension=false` command-line parameter or install the [Java Cryptography Extension (JCE)](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) - see the [Prerequisites](https://github.com/CUTR-at-USF/gtfs-realtime-validator#prerequisites) section for details.

## Rules

* [Implemented rules](RULES.md)
* [Planned future rules](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22)

Have a suggestion for a new rule?  Open an issue with the ["new rule" label](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22).
 
## Building the project 

#### Prerequisites 

The GTFS-Realtime Validator is implemented as a web application, with the backend code written in Java.  An instance of the [Jetty embedded server](http://www.eclipse.org/jetty/) is used to run the application.

Following are the requirements to get the project up and running: 

* [Java Development Kit (JDK) 1.8 or higher](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html)
* [Apache Maven](https://maven.apache.org/)

If you're using HTTPS URLs, either:
* Add the `-Djsse.enableSNIExtension=false` when running the tool - `java -Djsse.enableSNIExtension=false -jar gtfs-rt-validator-1.0.0-SNAPSHOT.jar`
* [Java Cryptography Extension (JCE)](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) - If you're downloading GTFS or GTFS-rt from secure HTTPS URLs, you may need to install the JCE Extension.  You will need to replace the `US_export_policy.jar` and `local_policy.jar` files in your JVM `/security` directory, such as `C:\Program Files\Java\jdk1.8.0_73\jre\lib\security`, with the JAR files in the JCE Extension download. 

#### 1. Build the project 

From the command-line, run:

`mvn package`

This will generate an executable file in the `target/` directory with all the dependencies needed to run the application.

Note that this might take a while - this project also builds and packages the [gtfs-validator](https://github.com/conveyal/gtfs-validator) so a static GTFS validation report can be seen within the GTFS-rt validator tool.
 
If you're going to be rebuilding the project frequently (e.g., editing source code), we suggest you load the project as Maven project in an IDE like [IntelliJ](https://www.jetbrains.com/idea/) or [Netbeans](https://netbeans.org/).

#### 2. Run the application

From the command-line, run: 

`java -Djsse.enableSNIExtension=false -jar target/gtfs-rt-validator-1.0.0-SNAPSHOT.jar`

You should see some output, and a message saying `Go to http://localhost:8080 in your browser`. 

#### 3. View the application 

Once the application has been started, you can enter URLs for the feeds you'd like to have validated at:
 
 http://localhost:8080

## Configuration options
 
**Logging**

If you'd like to change the logging level, for example to see all debug statements, in `src/main/resources/simplelogger.properties` change the following line to say `DEBUG`:
 
 ~~~
 org.slf4j.simpleLogger.defaultLogLevel=DEBUG
 ~~~

`DEBUG` level will show the output for all rule validation in the log.

`WARN` will show a smaller number of informational messages.

 **Port number**
 
 Port `8080` is used by default.  If you'd like to change the port number (e.g., port `80`), you can use the command line parameter `-port 80`:
 
 `java -jar target/gtfs-rt-validator-1.0.0-SNAPSHOT.jar -port 80`
 
 **Database**
 
 We use [Hibernate](http://hibernate.org/) to manage data persistence to a database.  To allow you to get the tool up and running quickly, we use the embedded [HSQLDB](http://hsqldb.org/) by default.  This is not recommended for a production deployment.
 
 Hibernate configuration can be changed in [`src/main/resources/hibernate.cfg.xml`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/src/main/resources/hibernate.cfg.xml) to store data in any relational database.  You might want to check out the following resources for getting started:
 
 * [MySQL](https://docs.jboss.org/hibernate/orm/3.3/reference/en-US/html/session-configuration.html#configuration-xmlconfig)
 * [PostgreSQL](http://stackoverflow.com/a/16572156/937715)
 * [Microsoft SQL Server](http://stackoverflow.com/a/3588652/937715)
 * [Oracle](https://docs.oracle.com/cd/E11035_01/workshop102/ormworkbench/hibernate-tutorial/tutHibernate9.html)
 
 A list of all the dialect properties for specific database versions is shown [here](http://www.tutorialspoint.com/hibernate/hibernate_configuration.htm).
 
 **Docker**
 
 Want to run this in [Docker](https://www.docker.com/)?  Check out [gtfs-realtime-validator-docker](https://github.com/scrudden/gtfs-realtime-validator-docker).

## Acknowledgements

This project was funded by the [National Institute for Transportation Communities (NITC)](http://nitc.trec.pdx.edu/) via the project ["Overcoming Barriers for the Wide-scale Adoption of Standardized Real-time Transit Information"](http://nitc.trec.pdx.edu/research/project/1062/Overcoming_Barriers_for_the_Wide-scale_Adoption_of_Standardized_Real-time_Transit_Information).  It also includes work funded under the [2015 Google Summer of Code](https://www.google-melange.com/archive/gsoc/2015/orgs/osgeo/projects/nipuna777.html).