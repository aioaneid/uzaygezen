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

import com.google.common.base.Preconditions;

import java.util.BitSet;

/**
 * BitSet utilities. Unless otherwise specified, when seen as numbers the
 * bit sets here are considered to be in little-endian format.
 * 
 * @author Daniel Aioanei
 */
public class BitSetMath {
  
  private BitSetMath() {}
  
  /**
   * Increments the given bit set in-place, looking at it as a little-endian
   * value with no bit sign (always non-negative).
   * 
   * @return trailing set bit count of the result. Same as trailing clear bit
   * count of the input.
   */
  public static int increment(BitSet i) {
    int tsb = i.nextClearBit(0);
    i.set(tsb);
    i.clear(0, tsb);
    return tsb;
  }
  
  /**
   * Rotates the lowest {@code n} bits by {@code count} positions in-place. The
   * bits of index higher than or equal to {@code n} are not affected.
   * 
   * @param bs input-output
   * @param n the number of lowest bits to rotate
   * @param count right rotation if positive; left rotation if negative;
   * no rotation at all if zero.
   */
  /*
   * In Java 7 consider extracting a long[] and doing the rotation there.
   */
  public static void rotate(BitSet bs, int n, int count) {
    int gcd = MathUtils.gcd(n, count);
    if (gcd != 0) {
      // Optimisation: no need to rotate when n divides into count.
      if (gcd == n) {
        return;
      }
      int m = n / gcd;
      for (int i = 0; i < gcd; ++i) {
        boolean previous = bs.get(i);
        int jTimesCount = 0;
        for (int j = 1; j <= m; ++j) {
          jTimesCount += count;
          int index = i - jTimesCount % n;
          if (index < 0) {
            index += n;
          }
          boolean current = bs.get(index);
          if (previous ^ current) {
            bs.set(index, previous);
            previous = current;
          }
          assert (j < m) ^ (jTimesCount % n == 0);
        }
      }
    }
  }
  
  /**
   * Unfortunately BitSet.get cannot reuse an existing instance so we have this
   * method for exactly that purpose.
   * 
   * @param chi original bit set to extract bits from
   * @param from inclusive start of range to extract
   * @param to exclusive end of range to extract
   * @param r output
   */
  /*
   * Via some preliminary testing with a very small number of bits I found
   * this implementation to be slower than {@link BitSet#get(int, int)}. However
   * I expect in real life this implementation to behave slightly better because
   * of the complete lack of garbage. 
   */
  public static void extractBitRange(BitSet chi, int from, int to, BitSet r) {
    r.clear();
    for (int j = chi.nextSetBit(from); j < to
        & j != -1; j = chi.nextSetBit(j + 1)) {
      r.set(j - from);
    }
  }
  
  /**
   * Computes {@code i}'s gray code in-place.
   * 
   * @param i input-output
   */
  public static void grayCode(BitSet i) {
    /*
     * GC has the property that all highest bits remain unchanged, up to and
     * including the highest set bit. Thus we don't care about the intended
     * "size" of the input bit set.
     */
    boolean oneBeyond = true;
    // The first bit stays the same.
    // For zero the loop starts from -2 thence it never executes.
    for (int j = i.length() - 2; j >= 0; --j) {
      boolean current = i.get(j);
      if (oneBeyond) {
        // Could as well have used flip.
        i.set(j, !current);
      }
      oneBeyond = current;
    }
  }
  
  /**
   * Given the gray code of a number, it computes that number in-place.
   * 
   * @param i input-output
   */
  public static void grayCodeInverse(BitSet i) {
    /*
     * For zero the loop executes no iterations at all.
     * We use oneBeyond to remember the previously calculated bit as an
     * optimisation.
     */
    boolean oneBeyond = true;
    for (int j = i.length() - 2; j >= 0; --j) {
      if (oneBeyond) {
        // Unfortunately there is no getAndSet or getAndFlip.
        oneBeyond = !i.get(j);
        // Could as well have used flip.
        i.set(j, oneBeyond);
      } else {
        oneBeyond = i.get(j);
      }
    }
  }

  /**
   * Makes the bit set {@code to} equal to {@code from}.
   * 
   * @param from source
   * @param to destination
   */
  public static void copy(BitSet from, BitSet to) {
    to.clear();
    to.or(from);
  }
  
  /**
   * Produces a {@code long} from a little endian bit set. 
   * 
   * @param bs little endian base 2 number representation
   * @return the same number as {@code bs}, but represented as {@code long}
   */
  public static long littleEndianBitSetToNonNegativeLong(BitSet bs) {
    Preconditions.checkArgument(bs.length() < 64, "too many bits");
    long value = 0;
    for (int i = bs.nextSetBit(0); i != -1; i = bs.nextSetBit(i + 1)) {
      value |= 1L << i;
    }
    assert value >= 0;
    assert Long.bitCount(value) == bs.cardinality() : "Wrong cardinality for " + bs;
    return value;
  }
  
  /**
   * Computes the little endian representation of a non-negative {@literal
   * long}. This is the inverse of {@link #littleEndianBitSetToNonNegativeLong}.
   * 
   * @param value input
   * @param bs output
   */
  public static void nonNegativeLongToLittleEndianBitSet(long value, BitSet bs) {
    Preconditions.checkArgument(value >= 0, "Negative numbers are not allowed.");
    bs.clear();
    int bitLength = 64 - Long.numberOfLeadingZeros(value);
    assert bitLength < 64 : "How could negative number " + value + " get here?";
    if (bitLength != 0) {
      int lowestSetBit = Long.numberOfTrailingZeros(value);
      assert lowestSetBit != 64;
      for (int i = lowestSetBit; i < bitLength; ++i) {
        if ((value & (1L << i)) != 0) {
          bs.set(i);
        }
      }
    }
  }
}
