package com.google.uzaygezen.core.ranges;

public interface Range<T, V> extends Measurable<V> {

    T getStart();
    T getEnd();
}
