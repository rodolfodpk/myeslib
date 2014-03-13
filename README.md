myeslib
=======

It was inspired by: [Don’t publish Domain Events, return them!](http://www.jayway.com/2013/06/20/dont-publish-domain-events-return-them/)

and off course, also by: [Simple CQRS example](https://github.com/gregoryyoung/m-r)

Getting Started
===============

```
cd myeslib
mvn clean install
```
Before running it, you can optionally customize database settings on export-db-env-h2.sh: 
```
#!/bin/sh
DB_DATASOURCE_CLASS_NAME=org.h2.jdbcx.JdbcDataSource
#DB_URL=jdbc:h2:mem:test;MODE=Oracle
DB_URL=jdbc:h2:file:~/myeslib-database;MODE=Oracle
DB_USER=scott
DB_PASSWORD=tiger
export DB_DATASOURCE_CLASS_NAME
export DB_URL
export DB_USER
export DB_PASSWORD
```
then export the db variables and call [Flyway](http://flywaydb.org/) to initialize the target database:
```
source ./export-db-env-oracle.sh
cd myeslib-database
mvn clean compile flyway:migrate -Dflyway.locations=db/h2
```
there is a script for Oracle too:
```
mvn clean compile flyway:migrate -Dflyway.locations=db/oracle
```
after this your database should be ready. Now:
```
cd ../myeslib-hazelcast-example
java -jar target/myeslib-hazelcast-example-0.0.1-SNAPSHOT.jar
```
this service will receive commands as JSON on http://localhost:8080/inventory-item-command.
There is another implementation (simpler since it uses Hazelcast just for cache) on myeslib-jdbi-example.
Finally, start this in other console in order to create and send commands to the above endpoint:
```
cd myeslib-cmd-producer
java -jar target/myeslib-cmd-producer-0.0.1-SNAPSHOT.jar
```
Notes
=====
Your IDE must support [Project Lombok](http://projectlombok.org/)

Your maven repository must contain [Oracle jdbc drivers](http://www.oracle.com/technetwork/database/features/jdbc/jdbc-drivers-12c-download-1958347.html) - or you can simply remove references to it in pom.xml if want H2 instead

Disclaimer
==========
There are 2 packages within module 3rd-party with (intact) code from :

[mm4j - Simple multi-methods for Java](http://gsd.di.uminho.pt/members/jop/mm4j)

[google-gson](https://code.google.com/p/google-gson) Since I did not found RuntimeTypeAdapter classes within gson-2.2.4.jar
Some references
===============
[Building an Event Store based on a relational database](http://cqrs.wordpress.com/documents/building-event-storage/)

[Optimistic Locking with Oracle - pdf](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&ved=0CCgQFjAA&url=http%3A%2F%2Fwww.orafaq.com%2Fpapers%2Flocking.pdf&ei=rusgU7fgI8aqkAfU0oHQCw&usg=AFQjCNHwIQtdeFyDPmKRd-LYChUtLf0XFw&sig2=aQD6hQbsKKP0yow7677ZtA&bvm=bv.62922401,d.eW0)

[Transaction Isolation Levels in Oracle](http://www.oracle.com/technetwork/issue-archive/2005/05-nov/o65asktom-082389.html)

