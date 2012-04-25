/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.uzaygezen.core;

import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Non-empty interval with non-negative {@code long} inclusive start and
 * exclusive end.
 * 
 * TODO: Check for overflow in the long calculations.
 *
 * @author Daniel Aioanei
 */
public class LongRange {
  
  private final long start;
  private final long end;

  /**
   * Unsafe constructor.
   */
  LongRange(long start, long end) {
    assert 0 <= start & start < end : "start=" + start + " end=" + end;
    this.start = start;
    this.end = end;
  }

  /**
   * @return the inclusive start of the interval
   */
  public long getStart() {
    return start;
  }

  /**
   * @return the exclusive end of the interval
   */
  public long getEnd() {
    return end;
  }
  
  public static LongRange of(long start, long end) {
    Preconditions.checkArgument(
        0 <= start & start < end, "start must be nonnegative and less than end.");
    return new LongRange(start, end);
  }
  
  /**
   * Computes the overlap between this range and {@code other}.
   * 
   * @return the size of the overlapping area
   */
  public long overlap(LongRange other) {
    if (start >= other.end || end <= other.start) {
      return 0;
    } else {
      // At this point they definitely have something in common.
      if (start < other.start || end > other.end) {
        return Math.min(end, other.end) - Math.max(start, other.start);
      } else {
        return end - start;
      }
    }
  }

  /**
   * Computes the overlap between the orthotope {@code x} and {@code y}, which
   * must have exactly the same number of dimensions.
   * <p>
   * By convention the overlap between two orhotopes with zero dimensions is
   * {@code 1}.
   * </p>
   * 
   * @return the overlapping content size
   */
  public static long overlap(List<LongRange> x, List<LongRange> y) {
    int n = x.size();
    Preconditions.checkArgument(y.size() == n, "x and y must have the same size.");
    long overlap = 1;
    // Stop early if overlap.signum() becomes zero.
    for (int i = 0; i < n && overlap != 0; ++i) {
      LongRange xRange = x.get(i);
      LongRange yRange = y.get(i);
      overlap *= xRange.overlap(yRange);
    }
    return overlap;
  }
  
  /**
   * Convenience method which sums the overlap between one orthotope and a set
   * of target orthotopes. This is a plain summation, and the result is actually
   * higher than the intersection with the union of the target orthotope when
   * they are not disjoint.
   * 
   * @return the sum of the overlap of {@code x} with each element in {@code y}.
   */
  public static long overlapSum(List<LongRange> x, List<? extends List<LongRange>> y) {
    long sum = 0;
    for (List<LongRange> yElement : y) {
      sum += overlap(x, yElement);
    }
    return sum;
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(start, end);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LongRange)) {
      return false;
    }
    LongRange other = (LongRange) obj;
    return start == other.start && end == other.end;
  }

  public static boolean contains(List<LongRange> orthotope, List<Long> point) {
    final int n = point.size();
    Preconditions.checkArgument(orthotope.size() == point.size(), "dimensionality mismatch");
    for (int i = 0; i < n; ++i) {
      if (!orthotope.get(i).contains(point.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Visible for testing.
   */
  boolean contains(long point) {
    return start <= point && end > point;
  }
}
