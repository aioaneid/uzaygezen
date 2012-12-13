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

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * @author Daniel Aioanei
 */
public class ZoomingSpaceVisitorAdapterTest {

  /**
   * Tests that the {@link ZoomingNavigator} calls are translated correctly into
   * {@link SpaceVisitor} calls for all valid indexes and all valid levels in a
   * multidimensional space with {@code 1}, {@code 0} and {@code 3} bits for
   * each dimension, respectively.
   */
  @Test
  public void visit() {
    // m0 must be less than or equal to m2 for this test to work.
    final int m0 = 1, m2 = 3;
    final MultiDimensionalSpec spec =
        new MultiDimensionalSpec(ImmutableList.of(m0, 0, m2));
    SpaceVisitor mock = EasyMock.createStrictMock(SpaceVisitor.class);
    BitVector[] p = new BitVector[] {null, BitVectorFactories.OPTIMAL.apply(0), null};
    BitVector[] q = new BitVector[] {BitVectorFactories.OPTIMAL.apply(m0),
        BitVectorFactories.OPTIMAL.apply(0), BitVectorFactories.OPTIMAL.apply(m2)};
    for (int index = 0; index < 1 << (m0 + m2); ++index) {
      BitVector indexBs = BitVectorFactories.OPTIMAL.apply(m0 + m2);
      indexBs.copyFrom(index);
      IndexCalculator fake = new FakeSpaceFillingCurve(spec, indexBs);
      ZoomingSpaceVisitorAdapter adaptor = new ZoomingSpaceVisitorAdapter(fake, mock);
      for (int i = 0; i < 1 << m0; ++i) {
        p[0] = BitVectorFactories.OPTIMAL.apply(m0);
        p[0].copyFrom(i);
        for (int j = 0; j < 1 << m2; ++j) {
          p[2] = BitVectorFactories.OPTIMAL.apply(m2);
          p[2].copyFrom(j);
          for (int level = 0; level <= spec.maxBitsPerDimension(); ++level) {
            for (int l = 0; l < p.length; ++l) {
              q[l].copyFrom(p[l]);
              q[l].clear(0, Math.min(l == 0 ? m0 : (l == 1 ? 0 : m2), level));
            }
            BitVector indexEndBs = indexBs.clone();
            int lowOrderBitCount = level <= m0 ? (2 * level) : m0 + level;
            indexEndBs.clear(0, lowOrderBitCount);
            Pow2LengthBitSetRange x = new Pow2LengthBitSetRange(
                TestUtils.createBitVector(i >>> level << level, m0), Math.min(m0, level));
            Pow2LengthBitSetRange y =
                new Pow2LengthBitSetRange(BitVectorFactories.OPTIMAL.apply(0), 0);
            Pow2LengthBitSetRange z = new Pow2LengthBitSetRange(
                TestUtils.createBitVector(j >>> level << level, m2), level);
            List<Pow2LengthBitSetRange> ranges =  ImmutableList.of(x, y, z);
            for (int b = 0; b < 2; ++b) {
              EasyMock.expect(mock.visit(EasyMock.eq(
                  new Pow2LengthBitSetRange(indexEndBs, lowOrderBitCount)), EasyMock.eq(ranges)))
                  .andReturn(b % 2 == 0);
              EasyMock.replay(mock);
              Assert.assertEquals(b % 2 == 0, adaptor.visit(level, indexEndBs, q));
              EasyMock.verify(mock);
              EasyMock.reset(mock);
            }
          }
        }
      }
    }
  }
  
  private static class FakeSpaceFillingCurve implements SpaceFillingCurve {

    private final MultiDimensionalSpec spec;
    private final BitVector fixedIndex;
    
    public FakeSpaceFillingCurve(MultiDimensionalSpec spec, BitVector fixedIndex) {
      this.spec = spec;
      this.fixedIndex = fixedIndex;
    }
    
    @Override
    public void accept(ZoomingNavigator visitor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public MultiDimensionalSpec getSpec() {
      return spec;
    }

    @Override
    public void index(BitVector[] p, int minLevel, BitVector index) {
      index.copyFrom(fixedIndex);
      int lowOrderBitCount = 0;
      for (int i = 0; i < spec.getBitsPerDimension().size(); ++i) {
        int realLevel = Math.min(spec.getBitsPerDimension().get(i), minLevel);
        lowOrderBitCount += realLevel;
      }
      index.clear(0, lowOrderBitCount);
    }

    @Override
    public void indexInverse(BitVector index, BitVector[] p) {
      throw new UnsupportedOperationException();
    }
  }
}
