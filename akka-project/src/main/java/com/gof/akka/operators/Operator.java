package com.gof.akka.operators;

import com.gof.akka.functions.AbstractFunction;

public abstract class Operator {
    public String name;
    public final int batchSize;

    public Operator(String name, int batchSize) {
        this.name = name;
        this.batchSize = batchSize;
    }

    public abstract Operator clone();

}

