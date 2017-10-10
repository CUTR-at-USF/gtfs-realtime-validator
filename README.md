# GTFS-realtime Validator [![Build Status](https://travis-ci.org/CUTR-at-USF/gtfs-realtime-validator.svg?branch=master)](https://travis-ci.org/CUTR-at-USF/gtfs-realtime-validator) [![Join the GTFS-realtime chat](https://gtfs.herokuapp.com/badge.svg)](https://gtfs.herokuapp.com/)

A tool that validates [General Transit Feed Specification (GTFS)-realtime](https://developers.google.com/transit/gtfs-realtime/) feeds


<img src="https://cloud.githubusercontent.com/assets/928045/25874575/2afaa3b0-34e1-11e7-92a4-b0a68f233748.png" width="1000">


## Quick start

*Please note that this project is still under active development and is in an early alpha state.*

1. Install [Java Development Kit (JDK) 1.8 or higher](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html)
2. Download the latest alpha build:
    * [gtfs-realtime-validator-1.0.0-SNAPSHOT.jar](https://s3.amazonaws.com/gtfs-rt-validator/travis_builds/gtfs-realtime-validator-1.0.0-SNAPSHOT.jar)

**To run the validator in default server mode, which provides a web user interface:**

3. From the command line run `java -Djsse.enableSNIExtension=false -jar gtfs-realtime-validator-1.0.0-SNAPSHOT.jar`
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

**To run the validator in batch processing mode, to validate a large number of archived feed files:**

3. From the command line run `java -jar target/gtfs-realtime-validator-1.0.0-SNAPSHOT.jar -batch yes -gtfs "D:\HART\google_transit.zip" -gtfsrealtimepath "D:\HART\gtfs-rt"`
    * `-gtfs` should point to the GTFS zip file 
    * `-gtfsrealtimepath` should point to the directory holding the GTFS-realtime files

See ["Configuration Options -> Batch Processing"](https://github.com/CUTR-at-USF/gtfs-realtime-validator#batch-processing) for more documentation.

## Rules

* [Implemented rules](RULES.md)
* [Planned future rules](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22)

Have a suggestion for a new rule?  Open an issue with the ["new rule" label](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22).
 
## Building the project 

The main GTFS-Realtime Validator user interface is implemented as a web application, with the backend code written in Java.  An instance of the [Jetty embedded server](http://www.eclipse.org/jetty/) is used to run the application, with [Hibernate](http://hibernate.org/) used for data persistence.

#### Prerequisites 

Following are the requirements to build and run the project from source code: 

* [Java Development Kit (JDK) 1.8 or higher](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html)
* [Apache Maven](https://maven.apache.org/)

If you're using `https` URLs for GTFS or GTFS-rt feeds, either:
* Use the `-Djsse.enableSNIExtension=false` parameter when running the tool
* Install the [Java Cryptography Extension (JCE)](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) - You will need to replace the `US_export_policy.jar` and `local_policy.jar` files in your JVM `/security` directory, such as `C:\Program Files\Java\jdk1.8.0_73\jre\lib\security`, with the JAR files in the JCE Extension download.

#### 1. Build the project 

From the command-line, run:

`mvn package`

This will generate an executable file in the `target/` directory with all the dependencies needed to run the application.

Note that this might take a while - this project also builds and packages the [gtfs-validator](https://github.com/conveyal/gtfs-validator) so a static GTFS validation report can be seen within the GTFS-rt validator tool.
 
If you're going to be rebuilding the project frequently (e.g., editing source code), we suggest you load the project as Maven project in an IDE like [IntelliJ](https://www.jetbrains.com/idea/) or [Netbeans](https://netbeans.org/).

#### 2. Run the application

To start up the server so you can view the web interface, from the command-line, run: 

`java -Djsse.enableSNIExtension=false -jar target/gtfs-realtime-validator-1.0.0-SNAPSHOT.jar`

You should see some output, and a message saying `Go to http://localhost:8080 in your browser`.

Note that there is also an option to run the validator as a batch process on archived feeds - see ["Configuration Options -> Batch Processing"](https://github.com/CUTR-at-USF/gtfs-realtime-validator#batch-processing).

#### 3. View the application 

Once the application has been started, you can enter URLs for the feeds you'd like to have validated at:
 
 http://localhost:8080
 
Note that there is also an option for [Batch Processing](https://github.com/CUTR-at-USF/gtfs-realtime-validator#batch-processing) feeds from the command line.

## Configuration options
 
#### Logging

If you'd like to change the logging level, for example to see all debug statements, in `src/main/resources/simplelogger.properties` change the following line to say `DEBUG`:
 
 ~~~
 org.slf4j.simpleLogger.defaultLogLevel=DEBUG
 ~~~

`DEBUG` level will show the output for all rule validation in the log.

`WARN` will show a smaller number of informational messages.

#### Port number
 
 Port `8080` is used by default.  If you'd like to change the port number (e.g., port `80`), you can use the command line parameter `-port 80`:
 
 `java -jar target/gtfs-realtime-validator-1.0.0-SNAPSHOT.jar -port 80`
 
#### Database
 
 We use [Hibernate](http://hibernate.org/) to manage data persistence to a database.  To allow you to get the tool up and running quickly, we use the embedded [HSQLDB](http://hsqldb.org/) by default.  This is not recommended for a production deployment.
 
 Hibernate configuration can be changed in [`src/main/resources/hibernate.cfg.xml`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/src/main/resources/hibernate.cfg.xml) to store data in any relational database.  You might want to check out the following resources for getting started:
 
 * [MySQL](https://docs.jboss.org/hibernate/orm/3.3/reference/en-US/html/session-configuration.html#configuration-xmlconfig)
 * [PostgreSQL](http://stackoverflow.com/a/16572156/937715)
 * [Microsoft SQL Server](http://stackoverflow.com/a/3588652/937715)
 * [Oracle](https://docs.oracle.com/cd/E11035_01/workshop102/ormworkbench/hibernate-tutorial/tutHibernate9.html)
 
 A list of all the dialect properties for specific database versions is shown [here](http://www.tutorialspoint.com/hibernate/hibernate_configuration.htm).
 
#### Batch processing
 
 We support a command-line batch processing mode for archived GTFS-realtime files.
 
 Here's an example of a command to batch process a set of GTFS-realtime files:
 
 `java -jar target/gtfs-realtime-validator-1.0.0-SNAPSHOT.jar -batch yes -gtfs "D:\HART\google_transit.zip" -gtfsrealtimepath "D:\HART\gtfs-rt" -sort date`
 
 Parameters:
 
 * `-batch` - Must be provided for the validator to start in batch processing mode.  If this parameter isn't provided, the server will start in the normal mode.
 * `-gtfs` - The path and file name of the GTFS zip file.  GTFS zip file must cover the time period for the GTFS-rt archived files.  You can combine GTFS zip files if needed using the [Google transitfeed tool's](https://github.com/google/transitfeed/wiki/Merge).
 * `-gtfsrealtimepath` - The path to the folder that contains the individual GTFS-realtime protocol buffer files
 * `-sort` *(Optional)* - `date` if the GTFS-realtime files should be processed chronologically by the "last modified" date of the file (default), or `name` if the files should be ordered by the name of the file. If you use the name of the file to order the files, then the validator will try to parse the date/time from each individual file name and use that date/time as the "current" time.  Date/times in file names must be in the [ISO_DATE_TIME](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_DATE_TIME) format and must be the last 20 characters prior to the file extension - for example, `TripUpdates-2017-02-18T20-00-08Z.pb`.  If a date/time can't be parsed from the file name, then the last modified date is used as the "current" time. GTFS-realtime file order is important for rules such as E012, E018, and W007, which compare the previous feed iteration against the current one.     
 * `-plaintext` *(Optional)* - If this argument is supplied, the validator will output a plain text version of each of the protocol buffer files with the provided file extension.  For example, if the protocol buffer file has the name `trip-update.pb`, and the text `-plaintext txt` is provided as the argument, then the plain text version of this file will be `trip-update.pb.txt`.
 * `-stats` *(Optional)* - If this argument is supplied (e.g., `-stats yes`), the validator will save statistics to memory for each of the validation files that are processed, and will return a list of `ValidationStatistics` objects from `BatchProcessor.processFeeds()` that can be examined to see individual iteration and rule processing times (in decimal seconds).
 * `-ignoreshapes` *(Optional)* - If this argument is supplied (e.g., `-ignoreshapes yes`), the validator will ignore the shapes.txt file for the GTFS feed.  If you are getting OutOfMemoryErrors when processing very large feeds, you should try setting this to true.  Note that setting this to true will prevent the validator from checking rules like E029 that require spatial data.  See [this issue](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/284) for details.  
 
 After execution finishes, the results for each GTFS-realtime protocol buffer file will be output in JSON format with the same file name, but with "results.json" appended to the end.  For example, if one GTFS-realtime procotol buffer file name was `TripUpdates-2017-02-18T20-00-08Z.pb`, the validation results for that file name will be output as `TripUpdates-2017-02-18T20-00-08Z.pb.results.json`. 
 
 It will look something like:
 
 ~~~
 [ {
   "errorMessage" : {
     "messageId" : 0,
     "gtfsRtFeedIterationModel" : null,
     "validationRule" : {
       "errorId" : "W001",
       "severity" : "WARNING",
       "title" : "timestamp not populated",
       "errorDescription" : "Timestamps should be populated for all elements",
       "occurrenceSuffix" : "does not have a timestamp"
     },
     "errorDetails" : null
   },
   "occurrenceList" : [ {
     "occurrenceId" : 0,
     "messageLogModel" : null,
     "prefix" : "trip_id 277716"
   }, {
     "occurrenceId" : 0,
     "messageLogModel" : null,
     "prefix" : "trip_id 277767"
   }, {
     "occurrenceId" : 0,
     "messageLogModel" : null,
     "prefix" : "trip_id 277768"
   }, 
   ...
~~~

In the above example, three `trip_updates` have been validated, and each was missing a timestamp (warning `W001`).  To put together the full message for each occurrence of the warning or error, you add the occurrence `prefix` to the validationRule `occurrenceSuffix`.

For example, in log format the above would look like:
* `trip_id 277716 does not have a timestamp`
* `trip_id 277767 does not have a timestamp`
* `trip_id 277768 does not have a timestamp`
 
#### Docker
 
Want to run this in [Docker](https://www.docker.com/)?  Check out [gtfs-realtime-validator-docker](https://github.com/scrudden/gtfs-realtime-validator-docker).

## CUTR Release Process

**Snapshots**

We've set up a Maven repository to hold the snapshot artifacts from this project in a Github project - [cutr-mvn-repo](https://github.com/CUTR-at-USF/cutr-mvn-repo).

At CUTR, we should run the following at the command-line to create a new artifact:
~~~
mvn -Dgpg.skip -DaltDeploymentRepository=cutr-snapshots::default::file:"/Git Projects/cutr-mvn-repo/snapshots" clean deploy
~~~

Then commit using Git and push new artifacts to Github.

If you want to include snapshot releases in your project, you'll need to add the following to the `pom.xml` of the project you want to use it in:

~~~
<!-- CUTR SNAPSHOTs/RELEASES -->
<repositories>
    <repository>
        <id>cutr-snapshots</id>
        <url>https://raw.githubusercontent.com/CUTR-at-USF/cutr-mvn-repo/master/snapshots</url>
    </repository>        
</repositories>
~~~

## Acknowledgements

This project was funded by the [National Institute for Transportation Communities (NITC)](http://nitc.trec.pdx.edu/) via the project ["Overcoming Barriers for the Wide-scale Adoption of Standardized Real-time Transit Information"](http://nitc.trec.pdx.edu/research/project/1062/Overcoming_Barriers_for_the_Wide-scale_Adoption_of_Standardized_Real-time_Transit_Information).  It also includes work funded under the [2015 Google Summer of Code](https://www.google-melange.com/archive/gsoc/2015/orgs/osgeo/projects/nipuna777.html).