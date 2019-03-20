package com.gof.akka.operators;

import com.gof.akka.messages.Message;

import java.util.List;

public interface MapFunction extends AbstractFunction {
    public Message process(String key, String value);
}
