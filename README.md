[![Build Status](https://travis-ci.org/rodolfodpk/myeslib.svg?branch=master)](https://travis-ci.org/rodolfodpk/myeslib)

#### Context
The idea is to explore a flavour of CQRS / ES introduced in [Don’t publish Domain Events, return them!](http://www.jayway.com/2013/06/20/dont-publish-domain-events-return-them/) Basically, instead of having the  [CommandHandler](https://github.com/gregoryyoung/m-r/blob/master/SimpleCQRS/CommandHandlers.cs) with void method, the method will return a ```List<? extends Event>``` instead. These events and the original command will then form a <a href="myeslib-core/src/main/java/org/myeslib/core/data/UnitOfWork.java">UnitOfWork</a>. This UnitOfWork is then persisted into a fairly effective (and simple) Event Store backed by a relational database. There is not any read model yet but I do plan to develop it just for the sake of the example. [Tasks based UIs](http://cqrs.wordpress.com/documents/task-based-ui) are out of the current scope. The communication between producer and consumer services is HTTP based but for now the endpoints are not really well polished REST services.

#### What really matters
Here is the <a href="inventory-aggregate-root/src/main/java/org/myeslib/example/SampleDomain.java">SampleDomain</a>. Except for the AggregateRoot class, all other classes (commands and events) are immutable. If you are wondering where are the final modifiers, take a look at [@Value annotation](http://projectlombok.org/features/Value.html) from  [Project Lombok](http://projectlombok.org/). And just in case you prefer to have InventoryItemAggregateRoot as an immutable class, you may use [Lenses for Java](https://github.com/remeniuk/java-lenses/blob/master/examples/src/main/java/PersonZipCodeExample.java) to apply events to it.

There are tests for both <a href="inventory-aggregate-root/src/test/java/org/myeslib/example/InventoryItemCommandHandlerTest.java">InventoryItemCommandHandlerTest</a> and <a href="inventory-aggregate-root/src/test/java/org/myeslib/example/InventoryItemAggregateRootTest.java">InventoryItemAggregateRootTest</a>. I'm using plain JUnit but it's worth to mention Event Sourcing helps a lot when writing BDD specifications.

Both examples (**inventory-jdbi** and **inventory-hazelcast**) have the <a href="inventory-hazelcast/src/main/java/org/myeslib/example/hazelcast/routes/ReceiveCommandsAsJsonRoute.java">ReceiveCommandsAsJsonRoute</a>. This route receives commands as JSON, deserialize it an then routes the command instance to the next endpoint. <a href="inventory-hazelcast/src/main/java/org/myeslib/example/hazelcast/routes/HzConsumeCommandsRoute.java">xxConsumeCommandsRoute</a> will then consume the command from the endpoint and use the <a href="inventory-hazelcast/src/main/java/org/myeslib/example/hazelcast/routes/HzInventoryItemCmdProcessor.java">xxInventoryItemCmdProcessor</a> to process the command and save the resulting events into the eventstore. After this, the HzConsumeCommandsRoute will enqueue the AggregateRoot's id into a queue being consumed by <a href="inventory-hazelcast/src/main/java/org/myeslib/example/hazelcast/routes/HzConsumeEventsRoute.java">xxConsumeEventsRoute</a> in order to reflect the events into the query model.


#### Running the Inventory example 
First of all, build it:
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
then export the db variables and call [Flyway](http://flywaydb.org/) to initialize the target database ([H2 database](http://www.h2database.com)):
```
source ./export-db-env-h2.sh
cd inventory-database
mvn clean compile flyway:migrate -Dflyway.locations=db/h2
```
just in case, there is a script for Oracle too:
```
mvn clean compile flyway:migrate -Dflyway.locations=db/oracle
```
after this your database should be ready. Now:
```
cd ../inventory-jdbi
java -jar target/inventory-jdbi-0.0.1-SNAPSHOT.jar 10 100 10 100
```
The parameters are, respectively: 
* jettyMinThreads 
* jettyMaxThreads 
* dbPoolMinConnections 
* dbPoolMaxConnections


This service will receive commands as JSON on http://localhost:8080/inventory-item-command. It uses Hazelcast just as a cache. 

There is another implementation: **inventory-hazelcast**. It is more tied to Hazelcast since beside caching for snapshots, it uses a distributed map backed by a MapStore implementation to store <a href="myeslib-core/src/main/java/org/myeslib/core/data/AggregateRootHistory.java">AggregateRootHistory</a> instances. This map is configured as write-through. It also uses a Hazelcast queue to store <a href="myeslib-core/src/main/java/org/myeslib/core/data/UnitOfWork.java">UnitOfWork</a> instances. This Hazelcast implementation has an additional parameter: 

* eventsQueueConsumers (default =50)

Finally, in order to create and send commands to the above endpoint, start this in other console:
```
cd inventory-cmd-producer
java -jar target/inventory-cmd-producer-0.0.1-SNAPSHOT.jar 100 60000 30000
```
The parameters are: 
* datasetSize (how many aggregateRoot instances)
* delayBetweenDatasets (in milliseconds) 
* initialDelay
 
There are 3 datasets. Each dataset will send just one type of command: 
* CreateCommand 
* IncreaseCommand 
* DecreaseCommand

Datasets will be sent in the correct order (create, increase and decrease). 

#### Notes
* Your IDE must support [Project Lombok](http://projectlombok.org/)
* If you want to use Oracle instead of [H2 database](http://www.h2database.com), you will need to remove the comments on ojdbc7 dependencies within poms.xml In this case, please remember your maven repository should contain [Oracle jdbc drivers](http://www.oracle.com/technetwork/database/features/jdbc/jdbc-drivers-12c-download-1958347.html).

#### Some references
* [Building an Event Store based on a relational database](http://cqrs.wordpress.com/documents/building-event-storage/)
* [Optimistic Locking with Oracle - pdf](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&ved=0CCgQFjAA&url=http%3A%2F%2Fwww.orafaq.com%2Fpapers%2Flocking.pdf&ei=rusgU7fgI8aqkAfU0oHQCw&usg=AFQjCNHwIQtdeFyDPmKRd-LYChUtLf0XFw&sig2=aQD6hQbsKKP0yow7677ZtA&bvm=bv.62922401,d.eW0)
* [Transaction Isolation Levels in Oracle](http://www.oracle.com/technetwork/issue-archive/2005/05-nov/o65asktom-082389.html)

#### Disclaimer
There are 2 packages within module 3rd-party with (intact) code from :

* [mm4j - Simple multi-methods for Java](http://gsd.di.uminho.pt/members/jop/mm4j)
* [google-gson](https://code.google.com/p/google-gson) Since I did not found RuntimeTypeAdapter classes within gson-2.2.4.jar
