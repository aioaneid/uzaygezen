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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Preconditions;

/**
 * {@link BitVector} range with a power of 2 length, where the inclusive start has
 * all the {@code level} lowest bits set to zero, and the lowest {@code level}
 * bits of the inclusive end are all set to one. A single point is represented
 * as a bit set with {@code level=0}.
 * <p>
 * Caveat: As soon as the underlying bit set is modified, an instance and all
 * its clones should be considered invalid. We have to make this compromise for
 * performance reasons.
 * </p>
 * 
 * @author Daniel Aioanei
 */
public class Pow2LengthBitSetRange {

  private final BitVector start;
  private final int level;

  private static final int ANY_INT = -1;
  
  /**
   * @param start must have all the lowest {@code level} bits cleared
   * @param level non-negative
   */
  public Pow2LengthBitSetRange(BitVector start, int level) {
    this(start, level, ANY_INT);
    Preconditions.checkArgument(start.areAllLowestBitsClear(level),
        "Lowest level bits must be zero.");
    Preconditions.checkArgument(0 <= level & level <= start.size(), "level must be non-negative");
  }
  
  /**
   * Unsafe constructor. Keep it private.
   */
  private Pow2LengthBitSetRange(BitVector start, int level, int anyInt) {
    assert anyInt == ANY_INT;
    this.start = start;
    this.level = level;
  }

  public BitVector getStart() {
    return start;
  }

  public int getLevel() {
    return level;
  }
  
  public long length() {
    Preconditions.checkState(level < 64, "This method should only be called for level < 64.");
    return 1L << level;
  }

  public LongRange toLongRange() {
    long inclusiveStart = start.toExactLong();
    long delta = 1L << level;
    long exclusiveEnd = inclusiveStart + delta;
    // Use the unsafe constructor since the values are known to be fine.
    return new LongRange(inclusiveStart, exclusiveEnd);
  }
  
  /**
   * Computes the content of an orthotope. Semantically equivalent, but
   * hopefully faster, than {@code BigIntRange.overlap(
   * toBigIntOrthotope(orthotope), toBigIntOrthotope(orthotope))}.
   * <p>
   * By convention the content of an orthotope with zero dimensions is {@code
   * 1}.
   * </p>
   * 
   * @param pow2LengthOrthotope the orthotope whose content will be computed
   * @return the content of {@code orthotope}
   */
  public static long content(List<Pow2LengthBitSetRange> pow2LengthOrthotope) {
    int levelSum = 0;
    for (Pow2LengthBitSetRange range : pow2LengthOrthotope) {
      levelSum += range.level;
    }
    Preconditions.checkArgument(levelSum < 64, "content is too large");
    return 1L << levelSum;
  }
  
  public static List<LongRange> toLongOrthotope(
      List<Pow2LengthBitSetRange> pow2LengthOrthotope) {
    int len = pow2LengthOrthotope.size();
    LongRange[] array = new LongRange[len];
    for (int i = 0; i < len; ++i) {
      array[i] = pow2LengthOrthotope.get(i).toLongRange();
    }
    return Collections.unmodifiableList(Arrays.asList(array));
  }
  
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public int hashCode() {
    return start.hashCode() + 31 * level;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Pow2LengthBitSetRange)) {
      return false;
    }
    Pow2LengthBitSetRange other = (Pow2LengthBitSetRange) obj;
    return level == other.level && start.equals(other.start);
  }
  
  @Override
  public Pow2LengthBitSetRange clone() {
    return new Pow2LengthBitSetRange(start.clone(), level, ANY_INT);
  }

  public boolean encloses(Pow2LengthBitSetRange other) {
    final boolean result;
    if (level > other.level) {
      start.xor(other.start);
      result = level == start.size() || start.nextSetBit(level) == -1;
      start.xor(other.start);
    } else if (level == other.level) {
      // should be faster than xor-ing twice and computing nextSetBit
      result = start.equals(other.start);
    } else {
      result = false;
    }
    return result;
  }
}
