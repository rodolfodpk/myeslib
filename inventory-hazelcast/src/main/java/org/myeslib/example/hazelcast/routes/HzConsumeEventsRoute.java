package org.myeslib.example.hazelcast.routes;

import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;

import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;

@Slf4j
@AllArgsConstructor
public class HzConsumeEventsRoute extends RouteBuilder {

    static final String ID = "ID";
    static final int MAX_EVENTS_PER_POOLING = 100;

	final int eventsQueueConsumers;
	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
    final IMap<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap;
    final IQueue<UUID> eventsQueue;

    final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	@Override
	public void configure() throws Exception {

      from("timer://eventsQueuePolling?fixedRate=true&period=1s")
         .routeId("timer:eventsQueuePolling")
         .process(new EventsQueuePoolerProcessor())
         .split(body())
         .parallelProcessing()
         .streaming()
        //.log("received ${body}")
        .process(new Processor() {
            @Override
            public void process(Exchange e) throws Exception {
                UUID id = e.getIn().getBody(UUID.class);
                Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id);
                e.getOut().setHeader(ID, id);
                e.getOut().setBody(snapshot);
            }
        })
        //.log("produced ${body}")
		.to("direct:reflect-last-snapshot", "direct:reflect-query-model")
 		;
      
      from("direct:reflect-last-snapshot")
         .routeId("direct:reflect-last-snapshot")
         .process(new Processor() {
            @SuppressWarnings("unchecked")
            @Override
             public void process(Exchange e) throws Exception {
                 lastSnapshotMap.set(header(ID).evaluate(e, UUID.class), body().evaluate(e, Snapshot.class));
             }
         })
 	    ;

      from("direct:reflect-query-model")
      	.routeId("direct:reflect-query-model")
        .process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
			 // do nothing 
			}
		});
      
	}

    class EventsQueuePoolerProcessor implements Processor {
        @Override
        public void process(Exchange e) throws Exception {
            log.debug("will start polling eventsQueue...");
            Future<List<UUID>> pooledEvents = singleThreadExecutor.submit(new EventsQueuePooler());
            singleThreadExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
            e.getOut().setBody(pooledEvents.get(), List.class);
            if (pooledEvents.get().size()>0){
                log.info("found {} events", e.getOut().getBody(List.class).size());
            }
        }
    }

    class EventsQueuePooler implements Callable<List<UUID>> {
        @Override
        public List<UUID> call() throws Exception {
            final List<UUID> pooledEvents = new Vector<>();
            for (int i = 0; i < MAX_EVENTS_PER_POOLING; i++) {
                try {
                    //log.info("polling step {} ", i);
                    UUID uuid = eventsQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (uuid !=null ) {
                        log.debug("found {}", uuid.toString());
                        pooledEvents.add(uuid);
                    } else {
                        log.debug("did not found any event");
                    }
                } catch ( InterruptedException e){
                    log.debug("interrupted...");
                }
            }
            return pooledEvents;
        }
    }
}

