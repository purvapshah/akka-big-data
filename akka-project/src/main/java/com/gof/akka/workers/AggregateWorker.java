package com.gof.akka.workers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.gof.akka.messages.BatchMessage;
import com.gof.akka.functions.AggregateFunction;
import com.gof.akka.messages.Message;


public class AggregateWorker extends Worker {
    private final int windowSize;
    private final int windowSlide;
    private final AggregateFunction fun;
    private final Map<String, List<String>> windows = new HashMap<>();

    public AggregateWorker(String color, int stagePos, final List<ActorRef> downstream, final int batchSize,
                           final int windowSize, final int windowSlide, final AggregateFunction fun) {
        super(color, stagePos, downstream, batchSize);
        this.windowSize = windowSize;
        this.windowSlide = windowSlide;
        this.fun = fun;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder() //
                .match(Message.class, this::onMessage) //
                .match(BatchMessage.class, this::onBatchMessage) //
                .build();
    }

    @Override
    protected final void onMessage(Message message) {
        System.out.println(color + self().path().name() + "(" + stagePos + ") received: " + message);
        // Get key and value of the message
        final String key = message.getKey();
        final String value = message.getVal();

        // List of current values of the window
        List<String> winValues = windows.get(key);
        if (winValues == null) {
            winValues = new ArrayList<>();
            windows.put(key, winValues);
        }
        winValues.add(value);

        // If the size is reached
        if (winValues.size() == windowSize) {
            final Message result = fun.process(key, winValues);
            final int receiver = Math.abs(result.getKey().hashCode()) % downstream.size();
            downstream.get(receiver).tell(result, self());

            // Slide window
            windows.put(key, winValues.subList(windowSlide, winValues.size()));
        }
    }


    @Override
    protected void onBatchMessage(BatchMessage batchMessage) {
        System.out.println(color + self().path().name() + "(" + stagePos + ") received batch: " + batchMessage);
        for(Message message : batchMessage.getMessages()) {
            // Get key and value of the message
            final String key = message.getKey();
            final String value = message.getVal();

            // List of current values of the window
            List<String> winValues = windows.get(key);
            if (winValues == null) {
                winValues = new ArrayList<>();
                windows.put(key, winValues);
            }
            winValues.add(value);

            // Slide window
            windows.put(key, winValues.subList(windowSlide, winValues.size()));

            // If the size is reached
            if (winValues.size() == windowSize) {
                final Message result = fun.process(key, winValues);
                batchQueue.add(result);

                if(batchQueue.size() == batchSize) {
                    // Use the key of the first message to determine the right partition
                    final int receiver = Math.abs(batchQueue.get(0).getKey().hashCode()) % downstream.size();
                    downstream.get(receiver).tell(new BatchMessage(batchQueue), self());

                    // Empty queue
                    batchQueue.clear();
                }
                final int receiver = Math.abs(result.getKey().hashCode()) % downstream.size();
                downstream.get(receiver).tell(result, self());

            }
        }
    }

    static public final Props props(String color, int stagePos, List<ActorRef> downstream, int batchSize,
                                    final AggregateFunction fun, int size, int slide) {
        return Props.create(AggregateWorker.class, color, stagePos, downstream, batchSize, fun, size, slide);
    }
}
