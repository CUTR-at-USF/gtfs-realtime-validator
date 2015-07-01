# GTFS-Realtime Validator
Software that validates General Transit Feed Specification (GTFS)-realtime feeds

## Building and Running the project 
*Please not that this project is still under development and will not perform the intended task of 
monitoring a GTFS-Realtime feed and login the errors.*

*The instructions below are only to get the project up 
and running in the current state.*

### Prerequisites 

The GTFS-Realtime Validator is built using Java technologies. Maven is as the build management tool for this project.
An instance of the Jetty embedded server is used to run the core application.

Following are the requirements to get the project up and running. 

* Access to a terminal 
* JDK installed on the system 
* Maven installed on the system 
* (optional) git installed on the system to clone the repository

### 1. Download the code 

The source files would be needed in order to build the `jar` file. You can obtain them by downloading the files directly
or by cloning the git repository (recommended). 

#### 1.a Download zipped version of the repository

Download the current snapshot of the project to your local machine using the "Download Zip" link in the project home page. 
(https://github.com/CUTR-at-USF/gtfs-realtime-validator)


#### 1.b Clone this repository to your local machine.

With git installed on the system clone the repository to your local machine. 

`git clone https://github.com/CUTR-at-USF/gtfs-realtime-validator.git`

### 2. Build the project 

Using maven the project should be built. This process would create a executable `jar`.

With maven installed on the system package the project to build the executable. 

`mvn package`

### 3. Run the application

The second step would generate an executable file at the `target/` directory with all the dependencies needed to run 
the application. 

Execute the file created in order to start up the application 

`java -jar target/gtfs-rt-validator-1.0-SNAPSHOT.jar`

A message similar to `INFO: Started @XXXms` upon successful execution. 

### 4. View the applicaiton 

Once the application has been started the web interface can be accessed at `localhost:8080`

From there, following the instructions should allow for starting the monitoirng of the GTFS-Realtime Feeds.
