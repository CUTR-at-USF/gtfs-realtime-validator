# GTFS Realtime Validator [![Test and Package](https://github.com/MobilityData/gtfs-realtime-validator/actions/workflows/test_package.yml/badge.svg)](https://github.com/MobilityData/gtfs-realtime-validator/actions/workflows/test_package.yml) [![Docker image](https://github.com/MobilityData/gtfs-realtime-validator/actions/workflows/docker.yml/badge.svg)](https://github.com/MobilityData/gtfs-realtime-validator/actions/workflows/docker.yml) [![Join the GTFS Realtime chat](https://img.shields.io/badge/chat-on%20slack-red)](https://bit.ly/mobilitydata-slack)
A tool that validates [General Transit Feed Specification (GTFS)-realtime](https://developers.google.com/transit/gtfs-realtime/) feeds

<img src="https://cloud.githubusercontent.com/assets/928045/25874575/2afaa3b0-34e1-11e7-92a4-b0a68f233748.png" width="1000">

Read more in [this Medium article](https://medium.com/@sjbarbeau/introducing-the-gtfs-realtime-validator-e1aae3185439).

Questions? You can [open an issue](https://github.com/MobilityData/gtfs-realtime-validator/issues), ask the [MobilityData Slack Group](https://bit.ly/mobilitydata-slack) or reach out to the [GTFS Realtime Google Group](https://groups.google.com/forum/#!forum/gtfs-realtime).

## Quick start - Run it yourself

*Please note that this project is still under active development and is in an early alpha state.*

### Prerequisites

1. Install [Java Development Kit (JDK) 11 or higher](https://www.oracle.com/java/technologies/downloads/)

### Run the webapp

1. Download the latest webapp snapshot jar of the validator from the [webapp package](https://github.com/MobilityData/gtfs-realtime-validator/packages/1268975).
1. From the command line run `java -jar {JAR file name}`, where `{JAR file name}` is the name of the file you downloaded in the previous step. For example, if the JAR file name is `gtfs-realtime-validator-webapp-1.0.0-20220223.003109-1.jar`, you would run `java -jar gtfs-realtime-validator-webapp-1.0.0-20220223.003109-1.jar`.
1. When prompted, in your browser go to `http://localhost:8080`
1. Enter your [General Transit Feed Specification (GTFS)-realtime](https://developers.google.com/transit/gtfs-realtime/) and [GTFS](https://developers.google.com/transit/gtfs/) feed URLs and click "Start".  Example feeds:
    * HART (Tampa, FL)
        * GTFS Realtime - http://api.tampa.onebusaway.org:8088/trip-updates
        * GTFS - http://gohart.org/google/google_transit.zip
    * MBTA (Boston, MA)
        * GTFS Realtime - https://cdn.mbta.com/realtime/TripUpdates.pb
        * GTFS - https://cdn.mbta.com/MBTA_GTFS.zip
    * ...more at [OpenMobilityData.org](https://openmobilitydata.org/search?q=gtfsrt)


### Run batch validation

See the [batch processing](gtfs-realtime-validator-lib/README.md#batch-processing) section of the [**gtfs-realtime-validator-lib** README](gtfs-realtime-validator-lib/README.md).

## Rules

* [Implemented rules](RULES.md)
* [Planned future rules](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22)

Have a suggestion for a new rule?  Open an issue with the ["new rule" label](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22).  You can see the complete process for adding new rules on the [Adding new rules](ADDING_NEW_RULES.md) page.
 
## Building the project 

There are two components to this project:
* **gtfs-realtime-validator-lib** - The core library that implements GTFS Realtime [validation rules](RULES.md) as well as [batch processing mode](gtfs-realtime-validator-lib/README.md#batch-processing).  You can use this same library [in your own project](gtfs-realtime-validator-lib/README.md#using-validation-rules-or-the-batch-processor-in-your-project).
* **gtfs-realtime-validator-webapp** - A server and website that allows multiple users to validate GTFS-relatime feeds by simply entering URLs into the website.

The main **gtfs-realtime-validator-webapp** user interface is implemented as a web application, with the backend code written in Java.  An instance of the [Jetty embedded server](https://www.eclipse.org/jetty/) is used to run the application, with [Hibernate](https://hibernate.org/) used for data persistence.

#### Prerequisites 

Following are the requirements to build and run the project from source code: 

* [Java Development Kit (JDK) 11 or higher](https://www.oracle.com/java/technologies/downloads/)
* [Apache Maven](https://maven.apache.org/)

#### 1. Build the project 

From the command-line, run:

`mvn package`

This will generate an executable file in the `gtfs-realtime-validator-webapp/target/` directory with all the dependencies needed to run the web application.

Note that this might take a while - this project also builds and packages the [gtfs-validator](https://github.com/conveyal/gtfs-validator) so a static GTFS validation report can be seen within the GTFS-rt validator tool.
 
If you're going to be rebuilding the project frequently (e.g., editing source code), we suggest you load the project as Maven project in an IDE like [IntelliJ](https://www.jetbrains.com/idea/) or [Netbeans](https://netbeans.org/).

#### 2. Run the application

To start up the server so you can view the web interface, from the command-line, run: 

* `java -jar {JAR file name}`, where `{JAR file name}` is the name of the file you downloaded previously. For example, if the JAR file name is `gtfs-realtime-validator-webapp-1.0.0-20220223.003109-1.jar`, you would run `java -jar gtfs-realtime-validator-webapp-1.0.0-20220223.003109-1.jar`.


You should see some output, and a message saying `Go to http://localhost:8080 in your browser`.

#### 3. View the application 

Once the application has been started, you can enter URLs for the feeds you'd like to have validated at:
 
 http://localhost:8080
 
## Configuration options

See our [Configuration Guide](CONFIG.md) for various configuration options, including changing the port number that the server runs on, what database it connects to, and more.

Note that the validator also has a [batch processing mode](gtfs-realtime-validator-lib/README.md#batch-processing) - see the [**gtfs-realtime-validator-lib** README](gtfs-realtime-validator-lib/README.md).
 
## Docker
### Setup
1. Download and install [Docker](https://docs.docker.com/get-started/)
2. `docker pull ghcr.io/mobilitydata/gtfs-realtime-validator` to get the latest snapshot version of the validator. You can look at other Docker images for this project [here](https://github.com/orgs/MobilityData/packages/container/package/gtfs-realtime-validator).

Or, build the image yourself using the following:

`docker build -t gtfs-realtime-validator .`

### Run

`docker run -p 8080:8080 ghcr.io/mobilitydata/gtfs-realtime-validator`

Then go to `http://localhost:8080` in your web browser.

## Troubleshooting

Having problems?  Check out our [Troubleshooting guide](TROUBLESHOOTING.md).

## MobilityData Release Process

**Snapshots**

We've set up a Maven repository on GitHub Packages to hold the snapshot artifacts from this project - [GTFS Realtime Validator Packages](https://github.com/orgs/MobilityData/packages?repo_name=gtfs-realtime-validator). The GitHub Action [`test_package.yml`](../.github/workflows/test_package.yml) publishes to this repository.

NOTE: You need to [authenticate with GitHub](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token) to download the below artifacts

If you want to include snapshot releases in your project, you'll need to add the following to the `pom.xml` of the project you want to use it in:

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
     <artifactId>gtfs-realtime-validator</artifactId>
     <version>1.0.0-SNAPSHOT</version>
   </dependency>
~~~

## Acknowledgements

This project was initially created by the [Center for Transportation Research](https://www.cutr.usf.edu/) at the University of South Florida. 
In December 2021, the maintenance was transferred to [MobilityData](https://mobilitydata.org/).

The first part of this project was funded by the [National Institute for Transportation Communities (NITC)](https://nitc.trec.pdx.edu/) via the project ["Overcoming Barriers for the Wide-scale Adoption of Standardized Real-time Transit Information"](https://nitc.trec.pdx.edu/research/project/1062/Overcoming_Barriers_for_the_Wide-scale_Adoption_of_Standardized_Real-time_Transit_Information).  It also includes work funded under the [2015 Google Summer of Code](https://www.google-melange.com/archive/gsoc/2015/orgs/osgeo/projects/nipuna777.html).
