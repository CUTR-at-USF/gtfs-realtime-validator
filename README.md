# GTFS-realtime Validator [![Build Status](https://travis-ci.org/CUTR-at-USF/gtfs-realtime-validator.svg?branch=master)](https://travis-ci.org/CUTR-at-USF/gtfs-realtime-validator) [![Join the GTFS-realtime chat](https://gtfs.herokuapp.com/badge.svg)](https://gtfs.herokuapp.com/)

A tool that validates [General Transit Feed Specification (GTFS)-realtime](https://developers.google.com/transit/gtfs-realtime/) feeds


<img src="https://cloud.githubusercontent.com/assets/928045/25874575/2afaa3b0-34e1-11e7-92a4-b0a68f233748.png" width="1000">

## Live demo

We have a bleeding-edge alpha version running here as a demo:
 
 [http://transittools.forest.usf.edu/](http://transittools.forest.usf.edu/)
 
 Feel free to check it out, but just be aware that it may be unavailable at any time, or prone to hiccups.

## Quick start - Run it yourself

*Please note that this project is still under active development and is in an early alpha state.*

### Prerequisites

1. Install [Java Development Kit (JDK) 1.8 or higher](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html)

### Run the webapp

1. Download the latest webapp alpha build:
    * [gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar](https://s3.amazonaws.com/gtfs-rt-validator/travis_builds/gtfs-realtime-validator-webapp/1.0.0-SNAPSHOT/gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar)
1. From the command line run `java -Djsse.enableSNIExtension=false -jar gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar`
    * Note that if you're running on Java 9, you'll need to instead run `java -Djsee.enableSNIExtension=false --add-modules java.xml.bind -jar gtfs-realtime-validator-webapp/target/gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar`
1. When prompted, in your browser go to `http://localhost:8080`
1. Enter your [General Transit Feed Specification (GTFS)-realtime](https://developers.google.com/transit/gtfs-realtime/) and [GTFS](https://developers.google.com/transit/gtfs/) feed URLs and click "Start".  Example feeds:
    * HART (Tampa, FL)
        * GTFS-realtime - http://api.tampa.onebusaway.org:8088/trip-updates
        * GTFS - http://gohart.org/google/google_transit.zip
    * MBTA (Boston, MA)
        * GTFS-realtime - http://developer.mbta.com/lib/GTRTFS/Alerts/TripUpdates.pb
        * GTFS - http://www.mbta.com/uploadedfiles/MBTA_GTFS.zip
    * ...more at [Transitfeeds.com](http://transitfeeds.com/search?q=gtfsrt)

Please note that if you're using `https` URLS, you'll need to use the `-Djsse.enableSNIExtension=false` command-line parameter or install the [Java Cryptography Extension (JCE)](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) - see the [Prerequisites](https://github.com/CUTR-at-USF/gtfs-realtime-validator#prerequisites) section for details.

### Run batch validation

See the [batch processing](gtfs-realtime-validator-lib/README.md#batch-processing) section of the [**gtfs-realtime-validator-lib** README](gtfs-realtime-validator-lib/README.md).

## Rules

* [Implemented rules](RULES.md)
* [Planned future rules](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22)

Have a suggestion for a new rule?  Open an issue with the ["new rule" label](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22).  You can see the complete process for adding new rules on the [Adding new rules](ADDING_NEW_RULES.md) page.
 
## Building the project 

There are two components to this project:
* **gtfs-realtime-validator-lib** - The core library that implements GTFS-realtime [validation rules](RULES.md) as well as [batch processing mode](BATCH.md).  You can use this same library [in your own project](gtfs-realtime-validator-lib/README.md#using-validation-rules-or-the-batch-processor-in-your-project).
* **gtfs-realtime-validator-webapp** - A server and website that allows multiple users to validate GTFS-relatime feeds by simply entering URLs into the website.

The main **gtfs-realtime-validator-webapp** user interface is implemented as a web application, with the backend code written in Java.  An instance of the [Jetty embedded server](http://www.eclipse.org/jetty/) is used to run the application, with [Hibernate](http://hibernate.org/) used for data persistence.

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

This will generate an executable file in the `gtfs-realtime-validator-webapp/target/` directory with all the dependencies needed to run the web application.

Note that this might take a while - this project also builds and packages the [gtfs-validator](https://github.com/conveyal/gtfs-validator) so a static GTFS validation report can be seen within the GTFS-rt validator tool.
 
If you're going to be rebuilding the project frequently (e.g., editing source code), we suggest you load the project as Maven project in an IDE like [IntelliJ](https://www.jetbrains.com/idea/) or [Netbeans](https://netbeans.org/).

#### 2. Run the application

To start up the server so you can view the web interface, from the command-line, run: 

* *Java 8 and lower*: `java -Djsse.enableSNIExtension=false -jar gtfs-realtime-validator-webapp/target/gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar`

* *Java 9 and above*: `java -Djsee.enableSNIExtension=false --add-modules java.xml.bind -jar gtfs-realtime-validator-webapp/target/gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar`

You should see some output, and a message saying `Go to http://localhost:8080 in your browser`.

#### 3. View the application 

Once the application has been started, you can enter URLs for the feeds you'd like to have validated at:
 
 http://localhost:8080
 
## Configuration options

See our [Configuration Guide](CONFIG.md) for various configuration options, including changing the port number that the server runs on, what database it connects to, and more.

Note that the validator also has a [batch processing mode](gtfs-realtime-validator-lib/README.md#batch-processing) - see the [**gtfs-realtime-validator-lib** README](gtfs-realtime-validator-lib/README.md).
 
## Docker
 
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