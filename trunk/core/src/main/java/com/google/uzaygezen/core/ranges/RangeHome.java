package com.google.uzaygezen.core.ranges;

import java.util.List;

import com.google.uzaygezen.core.Pow2LengthBitSetRange;

public interface RangeHome<T, V, R> {

    R of(T start, T end);
    
    R toRange(Pow2LengthBitSetRange bitSetRange);
    
    V overlap(List<R> x, List<R> y);
}
