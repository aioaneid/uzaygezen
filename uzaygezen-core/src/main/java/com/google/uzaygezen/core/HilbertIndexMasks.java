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

import java.util.Collections;
import java.util.List;

import com.google.common.primitives.Ints;

/**
 * Precomputes the masks associated to the iterations in a Hilbert index.
 * 
 * @author Daniel Aioanei
 */
public class HilbertIndexMasks {

  private final BitVector[] masks;
  private final int[] cardinalities;
  private final int n;
  
  public HilbertIndexMasks(MultiDimensionalSpec spec) {
    List<Integer> bitsPerDimension = spec.getBitsPerDimension();
    n = bitsPerDimension.size();
    int mMax = spec.maxBitsPerDimension();
    masks = new BitVector[mMax];
    cardinalities = new int[mMax];
    for (int i = 0; i < mMax; ++i) {
      int card = computeCardinality(bitsPerDimension, i);
      cardinalities[i] = card;
      BitVector mask = BitVectorFactories.OPTIMAL.apply(n);
      extractMask(bitsPerDimension, i, mask);
      masks[i] = mask;
    }
  }

  /**
   * Computes how many dimensions have more than {@code i} bits.
   * @param i must be less than {@code mMax}
   */
  public int getCardinality(int i) {
    return cardinalities[i];
  }
  
  public List<Integer> cardinalities() {
    return Collections.unmodifiableList(Ints.asList(cardinalities));
  }
  
  /**
   * Computes the mask pattern that identifies at iteration {@code i} which
   * dimensions have enough bits to be meaningful. The result will have a
   * capacity of {@code n} bits, where each meaningful dimension will have a set
   * bit in its corresponding position, with a shift of {@code d}, but
   * considering the bits in reverse order. We reverse the order so as to obtain
   * a standard Hilbert curve orientation.
   * 
   * @param i iteration number. Must be less than mMax.
   * @param d intra subhypercube direction. Must be less than the number of
   * dimensions.
   * @param mu output
   */
  public void copyMaskTo(int i, int d, BitVector mu) {
    mu.copyFrom(masks[i]);
    mu.rotate(d);
  }
  
  private static int computeCardinality(List<Integer> bitsPerDimension, int i) {
    int cardinality = 0;
    for (int j = 0; j < bitsPerDimension.size(); ++j) {
      if (bitsPerDimension.get(j) > i) {
        cardinality++;
      }
    }
    return cardinality;
  }

  private static void extractMask(List<Integer> bitsPerDimension, int i, BitVector mu) {
    for (int j = 0; j < bitsPerDimension.size(); ++j) {
      if (bitsPerDimension.get(bitsPerDimension.size() - j - 1) > i) {
        mu.set(j);
      }
    }
  }
}
