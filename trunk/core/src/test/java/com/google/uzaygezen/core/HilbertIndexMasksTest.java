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
import java.util.List;

import junit.framework.TestCase;

import com.google.common.primitives.Ints;

/**
 * @author Daniel Aioanei
 */
public class HilbertIndexMasksTest extends TestCase {
  
  public void testExtractMaskOneDimensionOneBit() {
    HilbertIndexMasks masks = new HilbertIndexMasks(new MultiDimensionalSpec(Arrays.asList(1)));
    BitVector mu = BitVectorFactories.OPTIMAL.apply(1);
    masks.copyMaskTo(0, 0, mu);
    BitVector expected = TestUtils.createBitVector(1, 1);
    assertEquals(expected, mu);
  }

  public void testExtractMaskTwoDimensions() {
    HilbertIndexMasks masks = new HilbertIndexMasks(new MultiDimensionalSpec(Arrays.asList(5, 4)));
    BitVector mu = BitVectorFactories.OPTIMAL.apply(2);
    masks.copyMaskTo(3, 0, mu);
    BitVector expected = TestUtils.createBitVector(3, 2);
    assertEquals(expected, mu);
  }
  
  public void testExtractMask() {
    List<Integer> bitsPerDimension = Arrays.asList(5, 4, 6);
    MultiDimensionalSpec spec = new MultiDimensionalSpec(bitsPerDimension);
    HilbertIndexMasks masks = new HilbertIndexMasks(spec);
    for (int i = 0; i < spec.maxBitsPerDimension(); ++i) {
      for (int d = 0; d < bitsPerDimension.size(); ++d) {
        BitVector expected = extractMask(bitsPerDimension, i, d);
        BitVector actual = BitVectorFactories.OPTIMAL.apply(bitsPerDimension.size());
        masks.copyMaskTo(i, d, actual);
        assertEquals(expected, actual);
      }
    }
  }
  
  private static BitVector extractMask(List<Integer> bitsPerDimension, int i, int d) {
    int[] m = Ints.toArray(bitsPerDimension);
    int n = m.length;
    BitVector mu = BitVectorFactories.OPTIMAL.apply(m.length);
    for (int j = 0; j < n; ++j) {
      if (m[((n << 1) - j - d - 1) % n] > i) {
        mu.set(j);
      }
    }
    return mu;
  }
}
