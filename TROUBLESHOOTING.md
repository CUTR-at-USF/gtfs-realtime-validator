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