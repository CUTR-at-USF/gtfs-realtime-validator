# GTFS-Realtime Validator [![Build Status](https://travis-ci.org/CUTR-at-USF/gtfs-realtime-validator.svg?branch=master)](https://travis-ci.org/CUTR-at-USF/gtfs-realtime-validator)
Software that validates General Transit Feed Specification (GTFS)-realtime feeds

## Building and Running the project 

*Please note that this project is still under active development and is in an early alpha state.  It has a limited number of rules and the user interface to view warnings/errors is still under development.*

### Prerequisites 

The GTFS-Realtime Validator is built using Java technologies. Maven is the build management tool for this project.
An instance of the Jetty embedded server is used to run the core application.

Following are the requirements to get the project up and running: 

* [Java Development Kit (JDK) 1.7 or higher](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html)
* [Apache Maven](https://maven.apache.org/)
* [Java Cryptography Extension (JCE)](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) - If you're downloading GTFS or GTFS-rt from secure HTTPS URLs, you may need to install the JCE Extension.  You will need to replace the `US_export_policy.jar` and `local_policy.jar` files in your JVM `/security` directory, such as `C:\Program Files\Java\jdk1.8.0_73\jre\lib\security`, with the JAR files in the JCE Extension download. 

### 1. Build the project 

From the command-line, run:

`mvn package`

This will generate an executable file in the `target/` directory with all the dependencies needed to run the application. 

### 3. Run the application

From the command-line, run: 

`java -jar target/gtfs-rt-validator-1.0-SNAPSHOT.jar`

You should see some output, and a message saying `Go to http://localhost:8080 in your browser`. 

### 4. View the application 

Once the application has been started, you can enter URLs for the feeds you'd like to have validated at:
 
 http://localhost:8080

### Configuration
 
**Logging**

If you'd like to change the logging level to see all debug statements, in `src/main/resources/simplelogger.properties` change the following line to say `DEBUG`:
 
 ~~~
 org.slf4j.simpleLogger.defaultLogLevel=DEBUG
 ~~~

 **Port number**
 
 Port `8080` is used by default.  If you'd like to change the port number (e.g., port `80`), you can use the command line parameter `-port 80`:
 
 `java -jar target/gtfs-rt-validator-1.0-SNAPSHOT.jar -port 80`
 


## Acknowledgements

This project was funded by the [National Institute for Transportation Communities (NITC)](http://nitc.trec.pdx.edu/) via the project ["Overcoming Barriers for the Wide-scale Adoption of Standardized Real-time Transit Information"](http://nitc.trec.pdx.edu/research/project/1062/Overcoming_Barriers_for_the_Wide-scale_Adoption_of_Standardized_Real-time_Transit_Information).  It also includes work funded under the [2015 Google Summer of Code](https://www.google-melange.com/archive/gsoc/2015/orgs/osgeo/projects/nipuna777.html).