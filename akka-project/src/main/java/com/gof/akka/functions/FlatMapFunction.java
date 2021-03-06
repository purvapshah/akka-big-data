package com.gof.akka.functions;

import com.gof.akka.messages.Message;

import java.util.List;

public interface FlatMapFunction extends AbstractFunction {
    public List<Message> process(String key, String value);

}
