package com.google.uzaygezen.core.ranges;

import java.util.List;

import com.google.uzaygezen.core.LongContent;
import com.google.uzaygezen.core.Pow2LengthBitSetRange;

public enum LongRangeHome implements RangeHome<Long, LongContent, LongRange> {

  INSTANCE;
  
  @Override
  public LongRange of(Long start, Long end) {
    return LongRange.of(start, end);
  }

  @Override
  public LongRange toRange(Pow2LengthBitSetRange bitSetRange) {
    long inclusiveStart = bitSetRange.getStart().toExactLong();
    long delta = 1L << bitSetRange.getLevel();
    long exclusiveEnd = inclusiveStart + delta;
    return LongRange.of(inclusiveStart, exclusiveEnd);
  }

  @Override
  public LongContent overlap(List<LongRange> x, List<LongRange> y) {
    long overlap = LongRange.overlap(x, y);
    return new LongContent(overlap);
  }
}
