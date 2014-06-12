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
import java.util.BitSet;
import java.util.Calendar;
import java.util.Comparator;

import com.google.uzaygezen.core.ranges.LongRange;

/**
 * @author Daniel Aioanei
 */
public class TestUtils {
  
  public static final LongRange ZERO_ONE = LongRange.of(0, 1);
  public static final LongRange ZERO_TWO = LongRange.of(0, 2);
  public static final LongRange ZERO_FOUR = LongRange.of(0, 4);
  public static final LongRange ZERO_TEN = LongRange.of(0, 10);
  public static final LongRange ONE_TEN = LongRange.of(1, 10);
  public static final LongRange TWO_FOUR = LongRange.of(2, 4);
  public static final LongRange TWO_SIX = LongRange.of(2, 6);
  public static final LongRange TWO_TEN = LongRange.of(2, 10);
  public static final LongRange THREE_FOUR = LongRange.of(3, 4);
  public static final LongRange FOUR_EIGHT = LongRange.of(4, 8);
  public static final LongRange THREE_SEVEN = LongRange.of(3, 7);
  public static final LongRange SIX_SEVEN = LongRange.of(6, 7);
  public static final LongRange SIX_TEN = LongRange.of(6, 10);
  public static final LongRange EIGHT_TEN = LongRange.of(8, 10);
  
  public static final long SEED = computeSeed();
  
  private static class ImmutableLongContent extends LongContent {
    
    public ImmutableLongContent(long v) {
      super(v);
    }
    
    @Override
    public void add(LongContent other) {
      throw new IllegalStateException("Cannot modify shared instance.");
    }
  }
  
  public static final LongContent ZERO_LONG_CONTENT = new ImmutableLongContent(0);
  public static final LongContent ONE_LONG_CONTENT = new ImmutableLongContent(1);
  
  private TestUtils() {}
  
  public static BitSet unsignedIntToLittleEndianBitSet(int i) {
    BitSet bs = new BitSet(32);
    for (int j = 0; j < 32; ++j) {
      if ((i & 1) == 1) {
        bs.set(j);
      }
      i >>>= 1;
    }
    return bs;
  }
  
  /**
   * Generates all possible multidimensional specifications with at most {@code
   * maxDimensions} dimensions, and the sum of all bits for all dimensions at
   * most {@code maxDimBitsSum}. For each such multidimensional space
   * specification it calls {@code callback}.
   * <p>
   * This method is used for exhaustive space search testing.
   * </p>
   * 
   * @param maxDimensions maximum number of dimensions
   * @param maxDimBitsSum maximum sum of the number of bits
   * @param callback will be called once for each different multidimensional
   * space specification
   */
  public static void generateSpec(int maxDimensions, int maxDimBitsSum, IntArrayCallback callback) {
    generateSpec(maxDimensions, maxDimBitsSum, false, callback);
  }

  /**
   * Like {@link #generateSpec(int, int, IntArrayCallback)}, but the sum must be exact.
   */
  public static void generateSpecWithExactSum(
      int maxDimensions, int dimBitsSum, IntArrayCallback callback) {
    generateSpec(maxDimensions, dimBitsSum, true, callback);
  }
  
  private static void generateSpec(
      int maxDimensions, int maxDimBitsSum, boolean exactSum, IntArrayCallback callback) {
    // mSum = sum of all dimensions.
    // n = number of dimensions
    for (int n = 0; n <= maxDimensions; ++n) {
      int[] m = new int[n];
      Arrays.fill(m, -1);
      // Generate all possible n dimensions with sum == mSum.
      int sum = 0;
      for (int k = 0; k >= 0;) {
        if (k == n) {
          if (sum <= maxDimBitsSum & (!exactSum | sum == maxDimBitsSum)) {
              callback.call(m);
          }
          --k;
          if (k >= 0) {
            sum -= m[k];
          }
        } else {
          ++m[k];
          if (sum + m[k] <= maxDimBitsSum) {
            sum += m[k];
            ++k;
          } else {
            m[k] = -1;
            --k;
            if (k >= 0) {
              sum -= m[k];
            }
          }
        }
      }
    }
  }
  
  /**
   * Callback for {@link #generateSpec(int, int, IntArrayCallback)} and
   * {@link #generateSpecWithExactSum(int, int, IntArrayCallback)}.
   */
  public interface IntArrayCallback {
    /**
     * Called for each multidimensional space specification.
     * 
     * @param m the bit count for each dimension
     */
    void call(int[] m);
  }

  /**
   * Lexicographical comparator of {@code int[]}.
   */
  public enum IntArrayComparator implements Comparator<int[]> {
    
    INSTANCE;

    @Override
    public int compare(int[] a, int[] b) {
      int minLen = Math.min(a.length, b.length);
      for (int i = 0; i < minLen; ++i) {
        int cmp = Integer.compare(a[i], b[i]);
        if (cmp != 0) {
          return cmp;
        }
      }
      return Integer.compare(a.length, b.length);
    }
  }
  
  private static long computeSeed() {
    String s = System.getProperty("test.compactHilbertCurve.seed");
    return s == null ? Calendar.getInstance().get(Calendar.DAY_OF_MONTH) : Long.parseLong(s);
  }

  /**
   * To be used by those tests methods which don't care about which type of bit
   * vector is instantiated.
   */
  public static BitVector createBitVector(long initialValue, int bitCount) {
    BitVector bitVector = BitVectorFactories.OPTIMAL.apply(bitCount);
    if (initialValue != 0) {
      bitVector.copyFrom(initialValue);
    }
    return bitVector;
  }
}
