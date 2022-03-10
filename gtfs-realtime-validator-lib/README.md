# gtfs-realtime-validator-lib

The core library that implements GTFS-realtime [validation rules](../RULES.md) as well as [batch processing mode](#batch-processing).

See the main project [README](../README.md) for more details on the [**gtfs-realtime-validator-webapp**](https://github.com/MobilityData/gtfs-realtime-validator/tree/master/gtfs-realtime-validator-webapp) submodule in this repository that implements a web server and website where you can input GTFS and GTFS-realtime URLs for validation. 

## Batch processing
 
As part of the **gtfs-realtime-validator-lib** module we support a command-line batch processing mode for archived GTFS-realtime files.
 
### Prerequisites
 
1. Install [Java Development Kit (JDK) 11 or higher](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html)
 
### Run it yourself
 
1. Download the latest library snapshot jar of the validator from the [library package](https://github.com/MobilityData/gtfs-realtime-validator/packages/1268974).
2. From the command line run `java -jar {JAR file name} -gtfs "D:\HART\google_transit.zip" -gtfsRealtimePath "D:\HART\gtfs-rt"`, where `{JAR file name}` is the name of the file you downloaded in the previous step. For example, if the JAR file name is `gtfs-realtime-validator-webapp-1.0.0-20220223.003109-1.jar`, you would run `java -jar gtfs-realtime-validator-webapp-1.0.0-20220223.003109-1.jar`.
    * `-gtfs` should point to the GTFS zip file 
    * `-gtfsRealtimePath` should point to the directory holding the GTFS-realtime files

### Output
 
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

### Command-line config parameters
 
 * `-gtfs` - The path and file name of the GTFS zip file.  GTFS zip file must cover the time period for the GTFS-rt archived files.  You can combine GTFS zip files if needed using the [Google transitfeed tool's](https://github.com/google/transitfeed/wiki/Merge).
 * `-gtfsRealtimePath` - The path to the folder that contains the individual GTFS-realtime protocol buffer files
 * `-sort` *(Optional)* - `date` if the GTFS-realtime files should be processed chronologically by the "last modified" date of the file (default), or `name` if the files should be ordered by the name of the file. If you use the name of the file to order the files, then the validator will try to parse the date/time from each individual file name and use that date/time as the "current" time.  Date/times in file names must be in the [ISO_DATE_TIME](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_DATE_TIME) format and must be the last 20 characters prior to the file extension - for example, `TripUpdates-2017-02-18T20-00-08Z.pb`.  If a date/time can't be parsed from the file name, then the last modified date is used as the "current" time. GTFS-realtime file order is important for rules such as E012, E018, and W007, which compare the previous feed iteration against the current one.     
 * `-plainText` *(Optional)* - If this argument is supplied, the validator will output a plain text version of each of the protocol buffer files with the provided file extension.  For example, if the protocol buffer file has the name `trip-update.pb`, and the text `-plainText txt` is provided as the argument, then the plain text version of this file will be `trip-update.pb.txt`.
 * `-stats` *(Optional)* - If this argument is supplied (e.g., `-stats yes`), the validator will save statistics to memory for each of the validation files that are processed, and will return a list of `ValidationStatistics` objects from `BatchProcessor.processFeeds()` that can be examined to see individual iteration and rule processing times (in decimal seconds).
 * `-ignoreShapes` *(Optional)* - If this argument is supplied (e.g., `-ignoreShapes yes`), the validator will ignore the shapes.txt file for the GTFS feed.  If you are getting OutOfMemoryErrors when processing very large feeds, you should try setting this to true.  Note that setting this to true will prevent the validator from checking rules like E029 that require spatial data.  See [this issue](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/284) for details.

For logging configuration, see the [Config->Logging](../CONFIG.md#logging) section.

## Building the project

Following are the requirements to build and run the project from source code: 

* [Java Development Kit (JDK) 11 or higher](https://www.oracle.com/java/technologies/downloads/)
* [Apache Maven](https://maven.apache.org/)

#### 1. Build the project 

You can build the **gtfs-realtime-validator-lib** module without building the entire **gtfs-realtime-validator-webapp** project, which speeds up builds significantly.

First, change to the `gtfs-realtime-validator-lib` directory if you're not already there:

`cd gtfs-realtime-validator-lib`

From the command-line, run:

`mvn package`

This will generate a JAR file in the `gtfs-realtime-validator-lib/target/` directory with all the dependencies needed to use the validation rules in your own project or to run batch processing mode.

## Using validation rules or the batch processor in your project

You can use the **gtfs-realtime-validator-lib** library in your own project as well.  

NOTE: You need to [authenticate with GitHub](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token) to download the below artifacts

To include a snapshot JAR via Maven, you'll need to add the below dependency to your `pom.xml`

~~~
<!-- MobilityData SNAPSHOTs/RELEASES -->
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub gtfs-realtime-validator</name>
        <url>https://maven.pkg.github.com/MobilityData/gtfs-realtime-validator</url>
        <releases><enabled>true</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
...
   <dependency>
     <groupId>org.mobilitydata</groupId>
     <artifactId>gtfs-realtime-validator-lib</artifactId>
     <version>1.0.0-SNAPSHOT</version>
   </dependency>
~~~

See the [**transit-feed-quality-calculator**](https://github.com/CUTR-at-USF/transit-feed-quality-calculator) for an example of using this library in another project.