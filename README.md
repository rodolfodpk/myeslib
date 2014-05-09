[![Build Status](https://travis-ci.org/rodolfodpk/myeslib.svg?branch=master)](https://travis-ci.org/rodolfodpk/myeslib)

#### Context
The idea is to explore a design introduced in [Don’t publish Domain Events, return them!](http://www.jayway.com/2013/06/20/dont-publish-domain-events-return-them/) Basically, instead of having the  [CommandHandler](https://github.com/gregoryyoung/m-r/blob/master/SimpleCQRS/CommandHandlers.cs) with void method, the method will return a ```List<? extends Event>```. These events and the original command will then form a <a href="myeslib-core/src/main/java/org/myeslib/core/data/UnitOfWork.java">UnitOfWork</a>. This UnitOfWork is then persisted into a fairly effective (and simple) Event Store backed by a relational database. There is not any read model yet but I do plan to develop it just for the sake of the example. [Tasks based UIs](http://cqrs.wordpress.com/documents/task-based-ui) are out of the current scope. The communication between producer and consumer services is HTTP based but for now the endpoints are not really well polished REST services.

#### The sample core domain
Here is the <a href="inventory-aggregate-root/src/main/java/org/myeslib/example/SampleDomain.java">SampleDomain</a>. Except for the AggregateRoot class, all other classes (commands and events) are immutable. If you are wondering where are the final modifiers, take a look at [@Value annotation](http://projectlombok.org/features/Value.html) from  [Project Lombok](http://projectlombok.org/). And just in case you prefer to have InventoryItemAggregateRoot as an immutable class, you may use [Lenses for Java](https://github.com/remeniuk/java-lenses/blob/master/examples/src/main/java/PersonZipCodeExample.java) to apply events to it.

There are tests for both <a href="inventory-aggregate-root/src/test/java/org/myeslib/example/InventoryItemCommandHandlerTest.java">InventoryItemCommandHandlerTest</a> and <a href="inventory-aggregate-root/src/test/java/org/myeslib/example/InventoryItemAggregateRootTest.java">InventoryItemAggregateRootTest</a>. I'm using plain JUnit but it's worth to mention Event Sourcing helps a lot when writing BDD specifications.

#### The examples

There are two examples implemented:

* inventory-jdbi
* inventory-hazelcast

Both have in common the <a href="inventory-hazelcast/src/main/java/org/myeslib/example/hazelcast/routes/ReceiveCommandsAsJsonRoute.java">ReceiveCommandsAsJsonRoute</a>. This route receives commands as JSON, deserialize it an then routes the command instance to the next endpoint. 

In the Hazelcast implementation, <a href="inventory-hazelcast/src/main/java/org/myeslib/example/hazelcast/routes/HzConsumeCommandsRoute.java">HzConsumeCommandsRoute</a> will then consume the command from the endpoint above and use the <a href="inventory-hazelcast/src/main/java/org/myeslib/example/hazelcast/routes/HzInventoryItemCmdProcessor.java">HzInventoryItemCmdProcessor</a> to process the command and save the resulting events into the eventstore. After this, the HzConsumeCommandsRoute will enqueue the AggregateRoot's id into a queue being consumed by <a href="inventory-hazelcast/src/main/java/org/myeslib/example/hazelcast/routes/HzConsumeEventsRoute.java">HzConsumeEventsRoute</a> in order to reflect the events into the query model.

You can find more info on the <a href="https://github.com/rodolfodpk/myeslib/wiki/Home">wiki</a>
