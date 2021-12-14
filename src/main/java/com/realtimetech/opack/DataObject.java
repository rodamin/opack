package com.realtimetech.opack;

import com.realtimetech.opack.annotation.Transform;

import java.util.Random;

@Transform(transformer = DebugPrintTransformer.class)
public class DataObject {
    private static Random RANDOM = new Random();

    private int intValue;
    private Integer integerValue;

    public DataObject() {
        this.intValue = RANDOM.nextInt();
        this.integerValue = RANDOM.nextInt();
    }

    public int getIntValue() {
        return intValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }
}