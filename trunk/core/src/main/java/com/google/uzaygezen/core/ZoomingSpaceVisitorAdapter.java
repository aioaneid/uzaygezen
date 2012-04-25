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

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

/**
 * Adapts the user friendly {@link SpaceVisitor} to serve as a {@link
 * ZoomingNavigator}.
 * 
 * @author Daniel Aioanei
 */
public class ZoomingSpaceVisitorAdapter implements ZoomingNavigator {

  private final IndexCalculator curve;
  private final SpaceVisitor visitor;
  
  /**
   * To save some typing, and also for efficiency, we store a copy of the
   * {@code bitsPerDimension} list here as a plain array.
   */
  private final int[] m;
  
  /**
   * @param curve space filling curve
   * @param visitor adaptee
   */
  public ZoomingSpaceVisitorAdapter(IndexCalculator curve, SpaceVisitor visitor) {
    this.curve = curve;
    this.visitor = visitor;
    m = Ints.toArray(curve.getSpec().getBitsPerDimension());
  }
  
  @Override
  public boolean visit(int level, BitVector index, BitVector[] p) {
    checkArguments(level, p);
    Pow2LengthBitSetRange[] ranges = new Pow2LengthBitSetRange[p.length];
    int n = p.length;
    // TODO: Compute all lowOrderBitCount in the constructor.
    int lowOrderBitCount = 0;
    for (int i = 0; i < n; ++i) {
      int realLevel = Math.min(m[i], level);
      ranges[i] = new Pow2LengthBitSetRange(p[i], realLevel);
      lowOrderBitCount += realLevel;
    }
    assert computeIndex(p, level).equals(index);
    assert index.areAllLowestBitsClear(lowOrderBitCount);
    boolean needChildren = visitor.visit(
        new Pow2LengthBitSetRange(index, lowOrderBitCount),
        Collections.unmodifiableList(Arrays.asList(ranges)));
    return needChildren;
  }
  
  private BitVector computeIndex(BitVector[] p, int level) {
    BitVector index = BitVectorFactories.OPTIMAL.apply(curve.getSpec().sumBitsPerDimension());
    curve.index(p, level, index);
    return index;
  }

  private void checkArguments(int level, BitVector[] p) {
    Preconditions.checkArgument(
        0 <= level & level <= curve.getSpec().maxBitsPerDimension(), "Level out of range.");
    int len = p.length;
    Preconditions.checkArgument(len == m.length, "p.length must match the number of dimensions.");
    for (int i = 0; i < len; ++i) {
      BitVector point = p[i];
      int pointSize = point.size();
      Preconditions.checkArgument(pointSize == m[i]
          & ((pointSize < level && point.isEmpty()) || point.areAllLowestBitsClear(level)));
    }
  }
}
