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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Preconditions;

/**
 * Adapts {@link java.util.BitSet} to the {@link BitVector} abstraction.
 * 
 * @author Mehmet Akin
 * @author Daniel Aioanei
 */
public final class BitSetBackedBitVector implements BitVector, Cloneable {

  private final BitSet bitset;
  private final int size;
  
  public BitSetBackedBitVector(int nbits) {
    this(nbits, new BitSet(nbits));
  }

  /**
   * Unsafe constructor. Keep it private.
   */
  private BitSetBackedBitVector(int size, BitSet bitset) {
    this.size = size;
    this.bitset = bitset;
  }
  
  private void checkSize(BitVector other) {
    if (other.size() != this.size()) {
      throw new IllegalArgumentException("Sizes are not equal. " + 
          "this:" + size + " other:" + other.size());
    }
  }

  private void checkIndex(int bitIndex) {
    if (bitIndex < 0 | bitIndex >= size) {
      throw new IndexOutOfBoundsException("BitIndex should be smaller than size : " + bitIndex);
    }
  }
  
  @Override
  public void and(BitVector o) {
    checkSize(o);
    bitset.and(toPotentiallySharedBitSet(o));
  }

  @Override
  public void andNot(BitVector o) {
    checkSize(o);
    bitset.andNot(toPotentiallySharedBitSet(o));
  }

  @Override
  public int cardinality() {
    return bitset.cardinality();
  }

  @Override
  public void clear() {
    bitset.clear();
  }

  @Override
  public void clear(int bitIndex) {
    checkIndex(bitIndex);
    bitset.clear(bitIndex);
  }

  @Override
  public void clear(int fromIndex, int toIndex) {
    bitset.clear(fromIndex, toIndex);
  }

  @Override
  public void copyFrom(BitVector from) {
    if (from.size() <= 64) {
      copyFrom(from.toExactLong());
    } else {
      bitset.clear();
      bitset.or(toPotentiallySharedBitSet(from));
    }
  }

  @Override
  public void flip(int bitIndex) {
    checkIndex(bitIndex);
    bitset.flip(bitIndex);
  }

  @Override
  public void flip(int fromIndex, int toIndex) {
    bitset.flip(fromIndex, toIndex);
  }

  @Override
  public boolean get(int bitIndex) {
    checkIndex(bitIndex);
    return bitset.get(bitIndex);
  }

  @Override
  public void grayCode() {
    BitSetMath.grayCode(bitset);
  }

  @Override
  public void grayCodeInverse() {
    BitSetMath.grayCodeInverse(bitset);
  }

  @Override
  public boolean increment() {
    int tsb = bitset.nextClearBit(0);
    if (tsb == size) {
      return false;
    }
    bitset.set(tsb);
    bitset.clear(0, tsb);
    return true;
  }

  @Override
  public boolean intersects(BitVector o) {
    checkSize(o);
    return bitset.intersects(toPotentiallySharedBitSet(o));
  }

  @Override
  public int length() {
    return bitset.length();
  }
  
  @Override
  public int size() {
    return size;
  }

  @Override
  public int nextClearBit(int fromIndex) {
    Preconditions.checkArgument(fromIndex >= 0);
    int ncb = bitset.nextClearBit(fromIndex);
    if (ncb >= size) {
      ncb = -1;
    }
    assert -1 <= ncb & ncb < size;
    return ncb;
  }

  @Override
  public int nextSetBit(int fromIndex) {
    return bitset.nextSetBit(fromIndex);
  }

  @Override
  public void or(BitVector o) {
    checkSize(o);
    bitset.or(toPotentiallySharedBitSet(o));
  }

  @Override
  public void rotate(int count) {
    BitSetMath.rotate(bitset, size, count);
  }

  @Override
  public void set(int bitIndex) {
    checkIndex(bitIndex);
    bitset.set(bitIndex);
  }
  
  @Override
  public void set(int bitIndex, boolean value) {
    bitset.set(bitIndex, value);
  }

  @Override
  public void set(int fromIndex, int toIndex) {
    bitset.set(fromIndex, toIndex);
  }

  @Override
  public void set(int fromIndex, int toIndex, boolean value) {
    bitset.set(fromIndex, toIndex, value);
  }

  @Override
  public void xor(BitVector o) {
    checkSize(o);
    bitset.xor(toPotentiallySharedBitSet(o));
  }

  public BitSet getBitset() {
    return bitset;
  }

  @Override
  public boolean isEmpty() {
    return bitset.isEmpty();
  }
  
  @Override
  public BitSetBackedBitVector clone() {
    return new BitSetBackedBitVector(size, (BitSet) bitset.clone());
  }
  
  @Override
  public int hashCode(){
    return size + 31 * bitset.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BitSetBackedBitVector) {
      BitSetBackedBitVector other = (BitSetBackedBitVector) obj;
      return size == other.size && bitset.equals(other.bitset);
    }
    if (obj instanceof BitVector) {
      BitVector other = (BitVector) obj;
      // optimisation
      if (size <= 64) {
        return size == other.size() && toExactLong() == other.toExactLong();
      } else {
        return size == other.size() && bitset.equals(other.toBitSet());
      }
    } else {
      return false;
    }
  }

  @Override
  public BitSet toBitSet() {
    return (BitSet) bitset.clone();
  }
  
  @Override
  public String toString() {
    return "size: " + size + " bitset: " + bitset;
  }

  @Override
  public long toLong() {
    long value = 0;
    for (int i = bitset.nextSetBit(0); i != -1 & i < 64; i = bitset.nextSetBit(i + 1)) {
      assert i < size;
      value |= 1L << i;
    }
    return value;
  }

  public void copyFrom(long value) {
    int bitLength = 64 - Long.numberOfLeadingZeros(value);
    Preconditions.checkArgument(bitLength <= size, "value doesn't fit");
    bitset.clear();
    int lowestSetBit = Long.numberOfTrailingZeros(value);
    for (int i = lowestSetBit; i < bitLength; ++i) {
      if ((value & 1L << i) != 0) {
        bitset.set(i);
      }
    }
  }

  @Override
  public int compareTo(BitVector o) {
    checkSize(o);
    return BitSetComparator.INSTANCE.compare(bitset, toPotentiallySharedBitSet(o));
  }

  @Override
  public void copyFrom(BitSet from) {
    Preconditions.checkArgument(from.length() <= size, "bit set is too large");
    bitset.clear();
    bitset.or(from);
  }

  @Override
  public void copyFromSection(BitVector src, int fromIndex) {
    Preconditions.checkArgument(fromIndex >= 0, "fromIndex must be non-negative");
    int srcSize = src.size();
    int toIndex = fromIndex + size;
    Preconditions.checkArgument(toIndex <= srcSize, "not enough bits in src");
    bitset.clear();
    for (int i = src.nextSetBit(fromIndex); i < toIndex && i != -1; i = src.nextSetBit(i + 1)) {
      bitset.set(i - fromIndex);
    }
  }

  @Override
  public long toExactLong() {
    long value = 0;
    for (int i = bitset.nextSetBit(0); i != -1; i = bitset.nextSetBit(i + 1)) {
      assert i < size;
      Preconditions.checkState(i < 64, "does not fit in long");
      value |= 1L << i;
    }
    return value;
  }
  
  private static BitSet toPotentiallySharedBitSet(BitVector bv) {
    if (bv instanceof BitSetBackedBitVector) {
      return ((BitSetBackedBitVector) bv).bitset;
    } else {
      return bv.toBitSet();
    }
  }

  @Override
  public void smallerEvenAndGrayCode() {
    if (bitset.get(0)) {
      bitset.clear(0);
      grayCode();
    } else {
      // Could use zero as well as the starting point.
      int firstSetIndex = bitset.nextSetBit(1);
      if (firstSetIndex != -1) {
        // Subtract 2 from this positive even number.
        bitset.clear(firstSetIndex);
        bitset.set(1, firstSetIndex, true);
        assert !bitset.get(0);
        grayCode();
      }
    }
  }

  @Override
  public void grayCodeRank(BitVector mu, BitVector w) {
    Preconditions.checkArgument(size() == mu.cardinality(), "r has the wrong size");
    Preconditions.checkArgument(mu.size() == w.size(), "mu/w size mismatch");
    clear();
    int pos = 0;
    for (int j = mu.size() == 0 ? -1 : mu.nextSetBit(0); j != -1;
        j = j == mu.size() - 1 ? -1 : mu.nextSetBit(j + 1)) {
      if (w.get(j)) {
        bitset.set(pos);
      }
      ++pos;
    }
  }

  @Override
  public int lowestDifferentBit() {
    final int value;
    if (bitset.isEmpty()) {
      value = 0;
    } else {
      if (bitset.get(0)) {
        int tsb = bitset.nextClearBit(0);
        assert tsb <= size : "bitset=" + bitset;
        value = tsb == size ? 0 : tsb;
      } else {
        int tcb = bitset.nextSetBit(0);
        assert 0 < tcb & tcb < size;
        value = tcb;
      }
    }
    assert value == 0 || (0 < value & value < size);
    return value;
  }
  
  @Override
  public void grayCodeRankInverse(BitVector mu, BitVector known, BitVector r) {
    Preconditions.checkArgument(r.size() == mu.cardinality(), "r.size()/mu.cardinality() mismatch");
    int muSize = mu.size();
    Preconditions.checkArgument(size == muSize, "i/mu size mismatch");
    // Will fail if the sizes are different.
    Preconditions.checkArgument(!known.intersects(mu), "known and mu must not intersect");
    bitset.clear();
    int pos = 0;
    int highestFreeBitIndex = -1;
    for (int k = muSize == 0 ? -1 : mu.nextSetBit(0); k != -1;
        k = k == muSize - 1 ? -1 : mu.nextSetBit(k + 1)) {
      highestFreeBitIndex = k;
      if (r.get(pos)) {
        bitset.set(k);
      }
      ++pos;
    }
    // TODO: Use previousClearBit in Java 7.
    for (int k = Math.max(highestFreeBitIndex, known.length()); --k >= 0; ) {
      if (!mu.get(k)) {
        assert !bitset.get(k);
        if (known.get(k) ^ bitset.get(k + 1)) {
          bitset.set(k);
        }
      }
    }
  }

  @Override
  public void copySectionFrom(int offset, BitVector src) {
    int srcSize = src.size();
    int toIndex = offset + srcSize;
    if (offset < 0 | toIndex > size) {
      throw new IndexOutOfBoundsException(
          "invalid range: offset=" + offset + " src.size()=" + src.size());
    }
    bitset.clear(offset, toIndex);
    for (int j = srcSize == 0 ? -1 : src.nextSetBit(0); j != -1;
        j = j == srcSize - 1 ? -1 : src.nextSetBit(j + 1)) {
      bitset.set(offset + j);
    }
  }

  @Override
  public long[] toLongArray() {
    long[] array = bitset.toLongArray();
    // Pad it to correct length.
    int n = (size + 63) >>> 6;
    if (array.length < n) {
    	return Arrays.copyOf(array, n);
    } else {
    	assert array.length == n;
    }
    return array;
  }
  
  @Override
  public byte[] toBigEndianByteArray() {
    int n = MathUtils.bitCountToByteCount(size);
    byte[] littleEndian = bitset.toByteArray();
    assert n >= littleEndian.length;
    byte[] a = Arrays.copyOf(littleEndian, n);
    ArrayUtils.reverse(a);
    return a;
  }

  @Override
  public BigInteger toBigInteger() {
    return new BigInteger(isEmpty() ? 0 : 1, toBigEndianByteArray());
  }
  
  @Override
  public void copyFrom(long[] array) {
    int len = (size + 63) >>> 6;
    Preconditions.checkArgument(array.length == len, "Length must be %s.", len);
    if (size == 0) {
      return;
    }
    Preconditions.checkArgument(
        Long.numberOfLeadingZeros(array[len - 1]) >= (len << 6) - size,
        "Some bit positions are too high.");
    BitSet bs = BitSet.valueOf(array);
    bitset.clear();
    bitset.or(bs);
  }

  @Override
  public void copyFromBigEndian(byte[] array) {
    int len = MathUtils.bitCountToByteCount(size);
    Preconditions.checkArgument(array.length == len, "Length must be %s.", len);
    if (len == 0) {
      return;
    }
    Preconditions.checkArgument(
        MathUtils.numberOfLeadingZeros(array[0]) >= (len << 3) - size,
        "Some bit positions are too high.");
    BitSet bs;
    ArrayUtils.reverse(array);
    try {
      bs = BitSet.valueOf(array);
    } finally {
      ArrayUtils.reverse(array);
    }
    bitset.clear();
    bitset.or(bs);
  }

  @Override
  public boolean areAllLowestBitsClear(int bitCount) {
    Preconditions.checkArgument(0 <= bitCount & bitCount <= size, "bitCount is out of range");
    int firstSetBit = bitset.nextSetBit(0);
    return firstSetBit == -1 | firstSetBit >= bitCount;
  }

  @Override
  public void copyFrom(BigInteger s) {
    byte[] array = BigIntegerMath.nonnegativeBigIntegerToBigEndianByteArrayForBitSize(s, size);
    copyFromBigEndian(array);
  }
}
