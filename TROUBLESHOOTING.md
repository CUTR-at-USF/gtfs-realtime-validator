# Troubleshooting

Things not going well?  See if your problem is one that we've encountered before.

### Java 9 compatibility - `NoClassDefFoundError: javax/xml/bind/JAXBException` when running project

*Symptom* - I try to run the application using Java 9, but I get an error message (e.g., `NoClassDefFoundError`) about missing modules

*Solution* - Java 9 changes to a modular architecture, which means that you need to tell Java to load certain modules required by the project.  

Try including the `java.xml.bind` module with the `--add-modules` parameter:

`java -Djsee.enableSNIExtension=false --add-modules java.xml.bind -jar gtfs-realtime-validator-webapp/target/gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar`

### SSL handshake still fails after following [prerequisites](https://github.com/CUTR-at-USF/gtfs-realtime-validator#prerequisites-1)

*Symptom* - I use `java -Djsee.enableSNIExtension=false ...` as instructed to run the app when retrieving GTFS or GTFS Realtime feeds from HTTPS URLs over SSL, but it fails with an error like `javax.net.ssl.SSLHandshakeException: java.security.cert.CertificateException: No subject alternative DNS name matching www.donneesquebec.ca found.` 

*Solution* - The underlying problem is probably with the server certificate configuration where the GTFS or GTFS Realtime data is hosted.  You can try to change the parameter to `-Djsse.enableSNIExtension=true` instead, which has helped [in the past](https://github.com/CUTR-at-USF/gtfs-realtime-validator/pull/310) for Linux deployments.

### `java.lang.OutOfMemoryError: Java heap space` when running project

*Symptom* - I try to run the application on a dataset and I get an error that looks like:

```
[main] INFO edu.usf.cutr.gtfsrtvalidator.lib.batch.BatchProcessor - Starting batch processor...
[main] INFO edu.usf.cutr.gtfsrtvalidator.lib.batch.BatchProcessor - Reading GTFS data from /tmp/validation_165096_gtfs_rt/file.zip...
[main] INFO edu.usf.cutr.gtfsrtvalidator.lib.batch.BatchProcessor - file.zip read in 16.145 seconds
[main] INFO edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata - Building GtfsMetadata for /tmp/validation_165096_gtfs_rt/file.zip...
[main] INFO edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata - Processing trips and building trip shapes for /tmp/validation_165096_gtfs_rt/file.zip...
Exception in thread \"main\" java.lang.OutOfMemoryError: Java heap space
    at org.locationtech.spatial4j.shape.jts.JtsShapeFactory$CoordinatesAccumulator.pointXYZ(JtsShapeFactory.java:316)
    at org.locationtech.spatial4j.shape.jts.JtsShapeFactory$CoordinatesAccumulator.pointXY(JtsShapeFactory.java:310)
	at org.locationtech.spatial4j.shape.jts.JtsShapeFactory$JtsLineStringBuilder.pointXY(JtsShapeFactory.java:228)
	at edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata.<init>(GtfsMetadata.java:184)
	at edu.usf.cutr.gtfsrtvalidator.lib.batch.BatchProcessor.processFeeds(BatchProcessor.java:145)
	at edu.usf.cutr.gtfsrtvalidator.lib.Main.main(Main.java:62)"
```

*Solution* - Typically you can fix a `java.lang.OutOfMemoryError: Java heap space` by increasing the heap size using the following command-line parameters when running the batch validator:

```
java ... -Xmx512m -XX:MaxMetaspaceSize=512m
```

See [this StackOverflow post](https://stackoverflow.com/a/38336005/937715) for more details.

If you can't allocate more memory to the batch validator, note that you can typically avoid OOM errors on larger feeds by ignoring the rules that process shapes.txt file by adding the command line parameter:

`-ignoreShapes yes`

Note that setting this to true will prevent the validator from checking rules like E029 that require spatial data.

See the [batch processor command-line parameters](https://github.com/MobilityData/gtfs-realtime-validator/blob/master/gtfs-realtime-validator-lib/README.md#command-line-config-parameters) for details.
