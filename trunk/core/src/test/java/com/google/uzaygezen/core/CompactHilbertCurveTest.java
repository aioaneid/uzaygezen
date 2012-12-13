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
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.uzaygezen.core.TestUtils.IntArrayCallback;

/**
 * @author Daniel Aioanei
 */
public class CompactHilbertCurveTest {

  private static final boolean CHECK_TEST_SANITY = true;
  private static final boolean DEFAULT_CHECK_INVERSE = true;
  
  private static final boolean kAnyBoolean = TestUtils.SEED % 2 == 0;

  @Test
  public void copyBitsToAndFromCancelEachOther() {
    for (int n = 0; n < 10; ++n) {
      BitVector[] p = new BitVector[n];
      for (int i = 0; i < n; ++i) {
        p[i] = BitVectorFactories.OPTIMAL.apply(n);
      }
      for (int i = 0; i < 1 << n; ++i) {
        BitVector l = TestUtils.createBitVector(i, n);
        for (int k = 0; k < n; ++k) {
          CompactHilbertCurve.copyOneBitToEachDimensionWhereSet(l, k, p);
          BitVector l2 = BitVectorFactories.OPTIMAL.apply(n);
          CompactHilbertCurve.copyOneBitFromEachDimension(k, p, l2);
          Assert.assertEquals(l, l2);
          for (BitVector bs : p) {
            bs.clear();
          }
        }
      }
    }
  }

  @Test
  public void clearOneBitInEachDimension() {
    BitVector[] p = {BitVectorFactories.OPTIMAL.apply(3), BitVectorFactories.OPTIMAL.apply(1)};
    p[0].copyFrom(7);
    p[1].copyFrom(1);
    CompactHilbertCurve.clearOneBitInEachDimension(0, p);
    Assert.assertEquals(6, p[0].toExactLong());
    Assert.assertEquals(0, p[1].toExactLong());
    CompactHilbertCurve.clearOneBitInEachDimension(1, p);
    Assert.assertEquals(4, p[0].toExactLong());
    Assert.assertEquals(0, p[1].toExactLong());
    CompactHilbertCurve.clearOneBitInEachDimension(2, p);
    Assert.assertEquals(0, p[0].toExactLong());
    Assert.assertEquals(0, p[1].toExactLong());
  }
  
  @Test
  public void compactHilbertIndexOrderZeroOnHyperCubeIsZero() {
    for (int n = 0; n < 10; ++n) {
      int[] dims = new int[n];
      Arrays.fill(dims, 0);
      CompactHilbertCurve chc = new CompactHilbertCurve(dims);
      BitVector[] p = new BitVector[n];
      Arrays.fill(p, TestUtils.createBitVector(0, 0));
      BitVector chi = compactHilbertIndex(chc, p);
      Assert.assertTrue(chi.isEmpty());
    }
  }

  @Test
  public void compactHilbertIndexOnOneDimensionIsIdentityFunction() {
    for (int order = 0; order < 11; ++order) {
      for (int i = 0; i < 1 << order; ++i) {
        BitVector bitSetCounter = TestUtils.createBitVector(i, order);
        CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {order});
        BitVector chi = compactHilbertIndex(chc, new BitVector[] {bitSetCounter});
        Assert.assertEquals(bitSetCounter, chi);
      }
    }
  }

  @Test
  public void compactHilbertIndexFirstOrderOnSquare() {
    CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {1, 1});
    BitVector chi = compactHilbertIndex(chc, new BitVector[] {
        TestUtils.createBitVector(0, 1), TestUtils.createBitVector(0, 1)});
    Assert.assertEquals(TestUtils.createBitVector(0, 2), chi);
    chi = compactHilbertIndex(chc, new BitVector[] {
        TestUtils.createBitVector(0, 1), TestUtils.createBitVector(1, 1)});
    Assert.assertEquals(TestUtils.createBitVector(1, 2), chi);
    BitVector two = TestUtils.createBitVector(2, 2);
    chi = compactHilbertIndex(chc, new BitVector[] {
        TestUtils.createBitVector(1, 1), TestUtils.createBitVector(1, 1)});
    Assert.assertEquals(two, chi);
    chi = compactHilbertIndex(chc, new BitVector[] {
        TestUtils.createBitVector(1, 1), TestUtils.createBitVector(0, 1)});
    Assert.assertEquals(TestUtils.createBitVector(3, 2), chi);
  }

  @Test
  public void compactHilbertIndexFirstOrderOnX1Y0() {
    CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {1, 0});
    BitVector chi = compactHilbertIndex(chc, new BitVector[] {
        TestUtils.createBitVector(0, 1), TestUtils.createBitVector(0, 0)});
    Assert.assertEquals(TestUtils.createBitVector(0, 1), chi);
    chi = compactHilbertIndex(chc, new BitVector[] {
        TestUtils.createBitVector(1, 1), TestUtils.createBitVector(0, 0)});
    Assert.assertEquals(TestUtils.createBitVector(1, 1), chi);
  }

  @Test
  public void compactHilbertIndexFirstOrderOnX0Y1() {
    CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {0, 1});
    BitVector chi = compactHilbertIndex(chc, new BitVector[] {
        TestUtils.createBitVector(0, 0), TestUtils.createBitVector(0, 1)});
    Assert.assertEquals(TestUtils.createBitVector(0, 1), chi);
    chi = compactHilbertIndex(chc, new BitVector[] {
        TestUtils.createBitVector(0, 0), TestUtils.createBitVector(1, 1)});
    Assert.assertEquals(TestUtils.createBitVector(1, 1), chi);
  }

  @Test
  public void consecutiveFirstOrderHilbertIndexIsGrayCodeInverse() {
    for (int n = 0; n < 10; ++n) {
      int[] dims = new int[n];
      Arrays.fill(dims, 1);
      CompactHilbertCurve chc = new CompactHilbertCurve(dims);
      BitVector[] p = new BitVector[n];
      for (int i = 0; i < 1 << n; ++i) {
        BitVector bitSetCounter = TestUtils.createBitVector(i, n);
        for (int j = 0; j < n; ++j) {
          p[n - j - 1] = (i & (1 << j)) == 0 ? TestUtils.createBitVector(0, dims[j])
              : TestUtils.createBitVector(1, dims[j]);
        }
        BitVector expected = bitSetCounter.clone();
        expected.grayCodeInverse();
        BitVector chi = compactHilbertIndex(chc, p);
        Assert.assertEquals(expected, chi);
      }
    }
  }

  /**
   * Two consecutive n-points on the Hilbert curve of any fixed order and
   * dimensionality differ by 1 in TestUtils.oneAsBitVector dimension and are
   * identical in the other dimensions.
   */
  @Test
  public void consecutiveHilbertIndexesDifferInOneDimensionByOne() {
    /*
     * Execution time increases exponentially with the total number of bits
     * since we generate all possibilities.
     */
    final int bits = 8;
    for (int n = 1; n <= bits; ++n) {
      for (int order = 0; order <= bits / n; ++order) {
        int[] dims = new int[n];
        Arrays.fill(dims, order);
        CompactHilbertCurve chc = new CompactHilbertCurve(dims);
        BitVector[] p = new BitVector[n];
        SortedMap<BitVector, Integer> hilbertIndexToPoint = new TreeMap<BitVector, Integer>();
        for (int i = 0; i < 1 << (order * n); ++i) {
          for (int j = 0; j < n; ++j) {
            int component = 0;
            for (int k = order - 1; k >= 0; --k) {
              component <<= 1;
              component |= (i >>> (j * order + k)) & 1;
            }
            p[n - j - 1] = TestUtils.createBitVector(component, order);
          }
          BitVector chi = compactHilbertIndex(chc, p);
          Integer old = hilbertIndexToPoint.put(chi, i);
          assert old == null;
        }
        Assert.assertEquals(1 << (order * n), hilbertIndexToPoint.size());
        Integer previous = null;
        for (Integer i : hilbertIndexToPoint.values()) {
          if (previous == null) {
            Assert.assertEquals(0, i.intValue());
          } else {
            int x = i.intValue();
            int y = previous.intValue();
            checkNeighboursDifferInExactlyOneDimension(order, x, y);
          }
          previous = i;
        }
        if (order != 0) {
          checkNeighboursDifferInExactlyOneDimension(order, 0, previous
              .intValue());
        } else {
          Assert.assertEquals(0, previous.intValue());
        }
      }
    }
  }

  @Test
  public void compactHilbertIndexPreservesHilbertIndexOrdering() {
    Random rnd = new Random(TestUtils.SEED);
    int n = 5;
    int[] m = new int[n];
    int mMax = 0;
    int mUpperBound = 1 + rnd.nextInt(31);
    for (int i = 0; i < n; ++i) {
      m[i] = rnd.nextInt(mUpperBound);
      if (mMax < m[i]) {
        mMax = m[i];
      }
    }
    for (int trial = 0; trial < 256; ++trial) {
      BitVector[] p = new BitVector[n];
      BitVector[] q = new BitVector[n];
      for (int i = 0; i < n; ++i) {
        p[i] = TestUtils.createBitVector(rnd.nextInt(1 << m[i]), m[i]);
        q[i] = TestUtils.createBitVector(rnd.nextInt(1 << m[i]), m[i]);
      }
      // Compact indexes.
      CompactHilbertCurve chc = new CompactHilbertCurve(m);
      BitVector pChi = compactHilbertIndex(chc, p);
      BitVector qChi = compactHilbertIndex(chc, q);
      int[] sizedUpM = new int[n];
      Arrays.fill(sizedUpM, mMax);
      int actual = Integer.signum(pChi.compareTo(qChi));
      // Non-compact indexes.
      CompactHilbertCurve h = new CompactHilbertCurve(m);
      BitVector pHi = compactHilbertIndex(h, p);
      BitVector qHi = compactHilbertIndex(h, q);
      int expected = Integer.signum(pHi.compareTo(qHi));
      Assert.assertEquals(expected, actual);
    }
  }

  private static void checkNeighboursDifferInExactlyOneDimension(int bitsPerDim, int x, int y) {
    int m = Math.max(x, y);
    int l = Math.min(x, y);
    int diff = m - l;
    int diffBitCount = Integer.bitCount(m - l);
    Assert.assertTrue(1 == diffBitCount || bitsPerDim == diffBitCount);
    if (diffBitCount == bitsPerDim) {
      // Must be a contiguous section bitsPerDim aligned.
      int tcb = Integer.numberOfTrailingZeros(diff);
      Assert.assertEquals(0, tcb % bitsPerDim);
      int hcb = Integer.numberOfLeadingZeros(diff);
      Assert.assertEquals(32, tcb + hcb + bitsPerDim);
    }
  }
  
  private static BitVector compactHilbertIndex(CompactHilbertCurve chc, BitVector[] p) {
    return compactHilbertIndex(chc, p, DEFAULT_CHECK_INVERSE);
  }
  
  private static BitVector compactHilbertIndex(
      CompactHilbertCurve chc, BitVector[] p, boolean checkInverse) {
    if (CHECK_TEST_SANITY) {
      final int n = chc.getSpec().getBitsPerDimension().size();
      for (int i = 0; i < n; ++i) {
        Assert.assertEquals(chc.getSpec().getBitsPerDimension().get(i).intValue(), p[i].size());
      }
    }
    BitVector chi = BitVectorFactories.OPTIMAL.apply(chc.getSpec().sumBitsPerDimension());
    chc.index(p, 0, chi);
    if (checkInverse) {
      BitVector[] q = new BitVector[p.length];
      for (int i = 0; i < q.length; ++i) {
        q[i] = BitVectorFactories.OPTIMAL.apply(chc.getSpec().getBitsPerDimension().get(i));
      }
      chc.indexInverse(chi, q);
      Assert.assertEquals(Arrays.asList(p), Arrays.asList(q));
    }
    return chi;
  }

  @Test
  public void compactHilbertIndexInverseOrderZeroOnHyperCubeIsZero() {
    for (int n = 0; n < 10; ++n) {
      int[] dims = new int[n];
      Arrays.fill(dims, 0);
      CompactHilbertCurve chc = new CompactHilbertCurve(dims);
      BitVector[] p = new BitVector[n];
      Arrays.fill(p, TestUtils.createBitVector(0, 0));
      chc.indexInverse(TestUtils.createBitVector(0, 0), p);
      for (BitVector coordinate : p) {
        Assert.assertTrue(coordinate.isEmpty());
      }
    }
  }

  @Test
  public void compactHilbertIndexInverseOnOneDimensionIsIdentityFunction() {
    for (int order = 0; order < 11; ++order) {
      for (int i = 0; i < 1 << order; ++i) {
        BitVector bitSetCounter = TestUtils.createBitVector(i, order);
        CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {order});
        BitVector[] p = new BitVector[1];
        p[0] = BitVectorFactories.OPTIMAL.apply(order);
        chc.indexInverse(bitSetCounter, p);
        Assert.assertEquals(bitSetCounter, p[0]);
      }
    }
  }

  @Test
  public void compactHilbertIndexInverseFirstOrderOnSquare() {
    CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {1, 1});
    BitVector[] p = new BitVector[2];
    p[0] = BitVectorFactories.OPTIMAL.apply(1);
    p[1] = BitVectorFactories.OPTIMAL.apply(1);
    chc.indexInverse(TestUtils.createBitVector(0, 2), p);
    Assert.assertTrue(p[0].isEmpty());
    Assert.assertTrue(p[1].isEmpty());
    chc.indexInverse(TestUtils.createBitVector(1, 2), p);
    Assert.assertTrue(p[0].isEmpty());
    Assert.assertEquals(TestUtils.createBitVector(1, 1), p[1]);
    BitVector two = TestUtils.createBitVector(2, 2);
    chc.indexInverse(two, p);
    Assert.assertEquals(TestUtils.createBitVector(1, 1), p[0]);
    Assert.assertEquals(TestUtils.createBitVector(1, 1), p[1]);
    BitVector three = TestUtils.createBitVector(3, 2);
    chc.indexInverse(three, p);
    Assert.assertEquals(TestUtils.createBitVector(1, 1), p[0]);
    Assert.assertEquals(TestUtils.createBitVector(0, 1), p[1]);
  }

  @Test
  public void compactHilbertIndexInverseFirstOrderOnX1Y0() {
    CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {1, 0});
    BitVector[] p = new BitVector[2];
    p[0] = BitVectorFactories.OPTIMAL.apply(1);
    p[1] = BitVectorFactories.OPTIMAL.apply(0);
    chc.indexInverse(TestUtils.createBitVector(0, 1), p);
    Assert.assertTrue(p[0].isEmpty());
    Assert.assertTrue(p[1].isEmpty());
    chc.indexInverse(TestUtils.createBitVector(1, 1), p);
    Assert.assertEquals(TestUtils.createBitVector(1, 1), p[0]);
    Assert.assertTrue(p[1].isEmpty());
  }

  @Test
  public void compactHilbertIndexInverseFirstOrderOnX0Y1() {
    CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {0, 1});
    BitVector[] p = new BitVector[2];
    p[0] = BitVectorFactories.OPTIMAL.apply(0);
    p[1] = BitVectorFactories.OPTIMAL.apply(1);
    chc.indexInverse(TestUtils.createBitVector(0, 1), p);
    Assert.assertTrue(p[0].isEmpty());
    Assert.assertTrue(p[1].isEmpty());
    chc.indexInverse(TestUtils.createBitVector(1, 1), p);
    Assert.assertTrue(p[0].isEmpty());
    Assert.assertEquals(TestUtils.createBitVector(1, 1), p[1]);
  }

  @Test
  public void compactHilbertIndexesInverseForEqualSideLenghts() {
    final int bits = 8;
    for (int n = 1; n <= bits; ++n) {
      for (int order = 0; order <= bits / n; ++order) {
        int[] dims = new int[n];
        Arrays.fill(dims, order);
        CompactHilbertCurve chc = new CompactHilbertCurve(dims);
        BitVector[] p = new BitVector[n];
        for (int i = 0; i < 1 << (order * n); ++i) {
          for (int j = 0; j < n; ++j) {
            int component = 0;
            for (int k = order - 1; k >= 0; --k) {
              component <<= 1;
              component |= (i >>> (j * order + k)) & 1;
            }
            p[n - j - 1] = TestUtils.createBitVector(component, order);
          }
          compactHilbertIndex(chc, p, true);
        }
      }
    }
  }
  
  @Test
  public void compactHilbertIndexInverse() {
    TestUtils.generateSpec(4, 6, new IntArrayCallback() {
      @Override
      public void call(int[] m) {
        matchHilbertIndexWithInverse(m);
      }
    });
  }
  
  @Test
  public void twoDimensionsOfOrder3AndOrder1Respectively() {
    CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {3, 1});
    ZoomingNavigator mock = EasyMock.createStrictMock(ZoomingNavigator.class);
    EasyMock.expect(mock.visit(EasyMock.eq(3), EasyMock.eq(TestUtils.createBitVector(0, 4)),
        EasyMock.aryEq(new BitVector[] {
            TestUtils.createBitVector(0, 3), TestUtils.createBitVector(0, 1)}))).andReturn(true);
    EasyMock.expect(mock.visit(EasyMock.eq(2), EasyMock.eq(TestUtils.createBitVector(0, 4)),
        EasyMock.aryEq(new BitVector[] {
            TestUtils.createBitVector(0, 3), TestUtils.createBitVector(0, 1)}))).andReturn(true);
    EasyMock.expect(mock.visit(EasyMock.eq(1), EasyMock.eq(TestUtils.createBitVector(0, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(0, 3), TestUtils.createBitVector(0, 1)}))).andReturn(false);
    EasyMock.expect(mock.visit(EasyMock.eq(1), EasyMock.eq(TestUtils.createBitVector(4, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(2, 3), TestUtils.createBitVector(0, 1)}))).andReturn(true);
    EasyMock.expect(mock.visit(EasyMock.eq(0), EasyMock.eq(TestUtils.createBitVector(4, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(2, 3), TestUtils.createBitVector(0, 1)}))).andReturn(kAnyBoolean);
    EasyMock.expect(mock.visit(EasyMock.eq(0), EasyMock.eq(TestUtils.createBitVector(5, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(3, 3), TestUtils.createBitVector(0, 1)}))).andReturn(kAnyBoolean);
    EasyMock.expect(mock.visit(EasyMock.eq(0), EasyMock.eq(TestUtils.createBitVector(6, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(3, 3), TestUtils.createBitVector(1, 1)}))).andReturn(kAnyBoolean);
    EasyMock.expect(mock.visit(EasyMock.eq(0), EasyMock.eq(TestUtils.createBitVector(7, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(2, 3), TestUtils.createBitVector(1, 1)}))).andReturn(kAnyBoolean);
    EasyMock.expect(mock.visit(EasyMock.eq(2), EasyMock.eq(TestUtils.createBitVector(8, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(4, 3), TestUtils.createBitVector(0, 1)}))).andReturn(true);
    EasyMock.expect(mock.visit(EasyMock.eq(1), EasyMock.eq(TestUtils.createBitVector(8, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(4, 3), TestUtils.createBitVector(0, 1)}))).andReturn(false);
    EasyMock.expect(mock.visit(EasyMock.eq(1), EasyMock.eq(TestUtils.createBitVector(12, 4)),
        EasyMock.aryEq(new BitVector[] {
        TestUtils.createBitVector(6, 3), TestUtils.createBitVector(0, 1)}))).andReturn(false);
    EasyMock.replay(mock);
    chc.accept(mock);
    EasyMock.verify(mock);
  }
  
  @Test
  public void allSpaceInquiredWhenChildrenAreAlwaysRequested() {
    TestUtils.generateSpec(4, 7, new IntArrayCallback() {
      @Override
      public void call(int[] m) {
        gatherAllIndexEnds(m);
      }
    });
  }

  private static void gatherAllIndexEnds(int[] m) {
    CompactHilbertCurve chc = new CompactHilbertCurve(m);
    HilbertOrderCheckingDriver visitor = new HilbertOrderCheckingDriver(chc);
    chc.accept(visitor);
    Assert.assertNull(visitor.getNextExpectedHilbertIndex());
  }
  
  private void matchHilbertIndexWithInverse(int[] m) {
    CompactHilbertCurve chc = new CompactHilbertCurve(m);
    final int n = m.length;
    final int mSum = chc.getSpec().sumBitsPerDimension();
    for (int i = 0; i < 1 << mSum; ++i) {
      BitVector bitStringCounter = TestUtils.createBitVector(i, mSum);
      BitVector[] p = new BitVector[n];
      int exclusiveUpperBitIndexBound = mSum;
      for (int j = 0; j < n; ++j) {
        p[j] = BitVectorFactories.OPTIMAL.apply(m[j]);
        p[j].copyFromSection(bitStringCounter, exclusiveUpperBitIndexBound - m[j]);
        exclusiveUpperBitIndexBound -= m[j];
      }
      assert exclusiveUpperBitIndexBound == 0;
      compactHilbertIndex(chc, p, true);
    }
  }
  
  /**
   * Mock visitor that excepts all points at the deepest level to be seen in
   * consecutive order. While it would be doable with EasyMock as well, it's
   * faster and easier like this. We do care about speed since we want to run
   * the tests for as many and large dimensions as possible, and exhaustive
   * space search is not exactly cheap.
   */
  private static class HilbertOrderCheckingDriver implements ZoomingNavigator {

    private final IndexCalculator chc;
    
    private BitVector nextExpectedHilbertIndex;
    
    public HilbertOrderCheckingDriver(IndexCalculator chc) {
      this.chc = chc;
      this.nextExpectedHilbertIndex =
          BitVectorFactories.OPTIMAL.apply(chc.getSpec().sumBitsPerDimension());
    }
    
    @Override
    public boolean visit(int level, BitVector index, BitVector[] p) {
      if (level == 0) {
        BitVector chi = BitVectorFactories.OPTIMAL.apply(chc.getSpec().sumBitsPerDimension());
        chc.index(p, 0, chi);
        Assert.assertEquals(nextExpectedHilbertIndex, chi);
        Assert.assertEquals(chi, index);
        if (!nextExpectedHilbertIndex.increment()) {
          nextExpectedHilbertIndex = null;
        }
      }
      return true;
    }
    
    public BitVector getNextExpectedHilbertIndex() {
      return nextExpectedHilbertIndex;
    }
  }
}
