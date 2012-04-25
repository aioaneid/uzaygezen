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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

/**
 * Compact Hilbert curve implementation that uses the minimum number of bits
 * needed to establish a bijection between the multidimensional space and
 * the compact Hilbert index, that is, {@code sum(i=0..n-1, m[i])}.
 * <p>
 * The main difference from the paper(s) is that we reverse the order of the
 * dimensions so that we keep the natural orientation of the curve.
 * </p>
 * This class is not thread safe.
 * 
 * @author Daniel Aioanei
 */
public class CompactHilbertCurve implements SpaceFillingCurve {
  
  private final MultiDimensionalSpec spec;

  /**
   * Cache of masks.
   */
  private final HilbertIndexMasks masks;
  
  /**
   * To save some typing, and also for efficiency, we store a copy of the
   * {@code bitsPerDimension} list here as a plain array.
   */
  private final int[] m;
  
  /**
   * To save some typing, we store here the number of dimensions.
   */
  private final int n;
  
  /**
   * Scratch bit vectors used in {@link #index}, {@link #indexInverse} and
   * {@link #accept}. This will work as long as these methods do not call each
   * other.
   */
  private final BitVector e;
  private final BitVector mu;
  private final BitVector w;
  private final BitVector t;
  private final BitVector[] rBuffer;

  public CompactHilbertCurve(MultiDimensionalSpec spec) {
    this.spec = Preconditions.checkNotNull(spec, "spec");
    masks = new HilbertIndexMasks(spec);
    m = Ints.toArray(spec.getBitsPerDimension());
    n = m.length;
    e = BitVectorFactories.OPTIMAL.apply(n);
    mu = BitVectorFactories.OPTIMAL.apply(n);
    w = BitVectorFactories.OPTIMAL.apply(n);
    t = BitVectorFactories.OPTIMAL.apply(n);
    rBuffer = allocateBitsForAllIterations();
  }

  /**
   * Convenience constructor.
   * 
   * @param m bits per dimension
   */
  public CompactHilbertCurve(int[] m) {
    this(new MultiDimensionalSpec(Ints.asList(m)));
  }
  
  @Override
  public MultiDimensionalSpec getSpec() {
    return spec;
  }

  /**
   * Computes the compact Hilbert index of the n-point {@code p}.
   */
  @Override
  public void index(BitVector[] p, int minLevel, BitVector index) {
    Preconditions.checkArgument(p.length == n, "Wrong number of elements.");
    Preconditions.checkArgument(0 <= minLevel & minLevel <= spec.maxBitsPerDimension());
    for (int i = 0; i < n; ++i) {
      Preconditions.checkArgument(p[i].length() <= m[i], "Value too large.");
    }
    index.clear();
    int d = 0;
    int exclusiveUpperBitIndexBound = spec.sumBitsPerDimension();
    e.clear();
    for (int i = spec.maxBitsPerDimension(); --i >= minLevel; ) {
      assert d < n;
      int dimensionCount = masks.getCardinality(i);
      masks.copyMaskTo(i, d, mu);
      copyOneBitFromEachDimension(i, p, w);
      BitVector r = rBuffer[i];
      assert r.size() == dimensionCount;
      computeCompactHilbertBits(d, mu, e, w, r);
      exclusiveUpperBitIndexBound -= dimensionCount;
      index.copySectionFrom(exclusiveUpperBitIndexBound, r);
      int oldD = d;
      d = updateD(d, w);
      updateE(oldD, w, e);
    }
    assert minLevel != 0 | exclusiveUpperBitIndexBound == 0;
  }

  /**
   * Computes the unique n-point {@code p} having {@code index} as its compact
   * Hilbert index in this multidimensional space.
   */
  @Override
  public void indexInverse(BitVector index, BitVector[] p) {
    Preconditions.checkArgument(n == p.length, "p does not have the right size.");
    for (int i = 0; i < n; ++i) {
      p[i].clear();
    }
    int d = 0;
    int k = spec.sumBitsPerDimension();
    BitVector[] wAndT = new BitVector[] {w, t};
    e.clear();
    t.clear();
    for (int i = spec.maxBitsPerDimension(); --i >= 0; ) {
      assert d < n;
      masks.copyMaskTo(i, d, mu);
      BitVector r = rBuffer[i];
      int start = k - r.size();
      r.copyFromSection(index, start);
      assert k == start + r.size();
      computeInverseBits(d, mu, e, r, wAndT);
      copyOneBitToEachDimensionWhereSet(t, i, p);
      int oldD = d;
      d = updateD(d, w);
      k = start;
      updateE(oldD, w, e);
    }
    assert k == 0;
  }

  @Override
  public void accept(ZoomingNavigator visitor) {
    BitVector[] p = new BitVector[n];
    assert n == p.length;
    for (int i = 0; i < n; ++i) {
      p[i] = BitVectorFactories.OPTIMAL.apply(m[i]);
    }
    // We use it to avoid re-calculating the index at each step.
    BitVector index = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
    e.clear();
    int d = 0;
    int k = spec.sumBitsPerDimension();
    t.clear();
    w.clear();
    BitVector[] wAndT = new BitVector[] {w, t};
    final int mMax = spec.maxBitsPerDimension();
    /*
     * It is probably possible to get rid of all these stacks, but to keep
     * things simple and efficient we use this extra amount of memory for now.
     */
    int[] dStack = new int[mMax];
    BitVector[] eStack = new BitVector[mMax];
    for (int i = 0; i < mMax; ++i) {
      eStack[i] = BitVectorFactories.OPTIMAL.apply(n);
    }
    if (!visitor.visit(mMax, index, p)) {
      return;
    }
    if (mMax != 0) {
      rBuffer[mMax - 1].clear();
    }
    // Temporary storage for output parameters i, k.
    int[] ik = new int[2];
    for (int i = mMax; --i >= 0; ) {
      masks.copyMaskTo(i, d, mu);
      computeInverseBits(d, mu, e, rBuffer[i], wAndT);
      copyOneBitToEachDimensionWhereSet(t, i, p);
      dStack[i] = d;
      eStack[i].copyFrom(e);
      int oldD = d;
      d = updateD(d, w);
      k -= masks.getCardinality(i);
      updateE(oldD, w, e);
      index.copySectionFrom(k, rBuffer[i]);
      boolean wantsChildren = visitor.visit(i, index, p);
      if (wantsChildren & i != 0) {
        rBuffer[i - 1].clear();
      } else {
        ik[0] = i;
        ik[1] = k;
        assert (i == 0) == (k == 0);
        goUpWhileNeeded(p, dStack, eStack, ik, index);
        i = ik[0];
        k = ik[1];
        d = dStack[i - 1];
        e.copyFrom(eStack[i - 1]);
        boolean incremented = rBuffer[i - 1].increment();
        if (!incremented) {
          assert i == mMax;
          break;
        }
      }
    }
  }

  private BitVector[] allocateBitsForAllIterations() {
    int mMax = spec.maxBitsPerDimension();
    assert mMax == masks.cardinalities().size();
    BitVector[] r = new BitVector[mMax];
    for (int i = 0; i < mMax; ++i) {
      r[i] = BitVectorFactories.OPTIMAL.apply(masks.getCardinality(i));
    }
    return r;
  }

  /**
   * As a side-effect it modifies {@link #mu} and {@link #t}.
   * 
   * @param p input
   * @param dStack input
   * @param eStack input
   * @param ik input-output
   * @param index input-output
   */
  private void goUpWhileNeeded(
      BitVector[] p, int[] dStack, BitVector[] eStack, int[] ik, BitVector index) {
    // Could just as well clear mu, e and r here. It doesn't matter.
    final int mMax = spec.maxBitsPerDimension();
    int i = ik[0];
    int k = ik[1];
    int dimensionCount;
    do {
      clearOneBitInEachDimension(i, p);
      dimensionCount = masks.getCardinality(i);
      k += dimensionCount;
      i++;
    } while (rBuffer[i - 1].cardinality() == dimensionCount && i != mMax);
    index.clear(ik[1], k);
    ik[0] = i;
    ik[1] = k;
    // Could just as well clear mu here. It doesn't matter.
  }

  /**
   * Computes the next bit in each coordinate.
   * 
   * @param d intra subhypercube direction
   * @param mu free bits pattern
   * @param e entry vertex of the current subhypercube
   * @param r index bits
   * @param wt output; gray code rank inverse and bit set with one bit for each coordinate
   */
  private static void computeInverseBits(
      int d, BitVector mu, BitVector e, BitVector r, BitVector[] wt) {
    BitVector w = wt[0];
    // Clone e into pi.
    w.copyFrom(e);
    w.rotate(d);
    w.andNot(mu);
    BitVector t = wt[1];
    t.grayCodeRankInverse(mu, w, r);
    /*
     * Remember w for updating d and e at the end of the iteration.
     * Reuse pi as w. Clone t.
     */
    w.copyFrom(t);
    t.grayCode();
    t.rotate(-d);
    t.xor(e);
  }
  
  /**
   * Called once per index calculation iteration to compute the next  {@code
   * mu.cardinality()} bits of the compact Hilbert index.
   * 
   * @param d current direction
   * @param mu free bits pattern
   * @param e entry vertex of the current subhypercube
   * @param w subhypercube
   * @param r output
   */
  private static void computeCompactHilbertBits(
      int d, BitVector mu, BitVector e, BitVector w, BitVector r) {
    // Reuse l as t.
    w.xor(e);
    w.rotate(d);
    // Reuse l, which is t, as w.
    w.grayCodeInverse();
    r.grayCodeRank(mu, w);
  }

  /**
   * Updates the intra-subhypercube direction for the next iteration.
   * 
   * @param d intra subhypercube direction
   * @param w subhypercube
   * @return the new intra-subhypercube direction
   */
  private static int updateD(int d, BitVector w) {
    // Add the intra-subhypercube direction.
    d += w.lowestDifferentBit() + 1;
    d %= w.size();
    return d;
  }

  /**
   * Updates the entry vertex of the subhypercube for the next iteration.
   * 
   * @param oldD the current iteration's intra-subhypercube direction
   * @param w subhypercube. This parameter is used as both input and scratch
   * space, so it must not be used after the call without full reinitialisation.
   * @param e input-output
   */
  private static void updateE(int oldD, BitVector w, BitVector e) {
    // Compute the entry vertex.
    w.smallerEvenAndGrayCode();
    w.rotate(-oldD);
    e.xor(w);
  }

  /**
   * Copies the {@code i}'th bit from each dimension, and optionally clears those
   * bits as well.
   * <p>Visible for testing.</p>
   * 
   * @param i the bit index to be copied
   * @param p source
   * @param bs output
   */
  static void copyOneBitFromEachDimension(int i, BitVector[] p, BitVector bs) {
    bs.clear();
    int n = p.length;
    for (int j = n; --j >= 0; ) {
      BitVector bv = p[n - j - 1];
      if (i < bv.size() && bv.get(i)) {
        bs.set(j);
      }
    }
  }

  /**
   * Clears bit {@code i} in each dimension with size at least {@code i + 1}.
   * 
   * @param i bit position
   * @param p output
   */
  static void clearOneBitInEachDimension(int i, BitVector[] p) {
    int n = p.length;
    for (int j = 0; j < n; ++j) {
      BitVector bv = p[j];
      if (i < bv.size()) {
        bv.clear(i);
      }
    }
  }

  /**
   * For each bit in {@code l} that is set, it also sets bit number {@code i} in
   * the result {@code p}, but looking at {@code p} in reverse order. Any other
   * bits in {@code p} are untouched.
   * <p>Visible for testing.</p>
   * 
   * @param src source
   * @param i bit index
   * @param p output
   */
  static void copyOneBitToEachDimensionWhereSet(BitVector src, int i, BitVector[] p) {
    int n = p.length;
    int srcSize = src.size();
    for (int j = srcSize == 0 ? -1 : src.nextSetBit(0); j != -1;
        j = j == srcSize - 1 ? -1 : src.nextSetBit(j + 1)) {
      p[n - j - 1].set(i);
    }
  }
  
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
