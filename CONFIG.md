## Configuration options
 
#### Port number
 
 In server mode, port `8080` is used by default.  If you'd like to change the port number (e.g., port `80`), you can use the command line parameter `-port 80`:
 
 `java -jar target/gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar -port 80`
 
#### Database
 
 We use [Hibernate](http://hibernate.org/) to manage data persistence to a database.  To allow you to get the tool up and running quickly, we use the embedded [HSQLDB](http://hsqldb.org/) by default.  This is not recommended for a production deployment.
 
 Hibernate configuration can be changed in [`src/main/resources/hibernate.cfg.xml`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/src/main/resources/hibernate.cfg.xml) to store data in any relational database.  You might want to check out the following resources for getting started:
 
 * [MySQL](https://docs.jboss.org/hibernate/orm/3.3/reference/en-US/html/session-configuration.html#configuration-xmlconfig)
 * [PostgreSQL](http://stackoverflow.com/a/16572156/937715)
 * [Microsoft SQL Server](http://stackoverflow.com/a/3588652/937715)
 * [Oracle](https://docs.oracle.com/cd/E11035_01/workshop102/ormworkbench/hibernate-tutorial/tutHibernate9.html)
 
 A list of all the dialect properties for specific database versions is shown [here](http://www.tutorialspoint.com/hibernate/hibernate_configuration.htm).
 
#### Logging

If you'd like to change the logging level, for example to see all debug statements, in `src/main/resources/simplelogger.properties` change the following line to say `DEBUG`:
 
 ~~~
 org.slf4j.simpleLogger.defaultLogLevel=DEBUG
 ~~~

`DEBUG` level will show the output for all rule validation in the log.

`WARN` will show a smaller number of informational messages.

#### Batch processing
 
We support a command-line batch processing mode for archived GTFS Realtime files.  See the [**gtfs-realtime-validator-lib** README](gtfs-realtime-validator-lib/README.md) page for details, including command-line configuration options for the batch processing mode.