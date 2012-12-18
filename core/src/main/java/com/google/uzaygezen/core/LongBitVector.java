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
import java.util.BitSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

/**
 * BitVector implementation for vectors of length 64 or less.
 * 
 * @author Mehmet Akin
 * @author Daniel Aioanei
 */
public final class LongBitVector implements BitVector, Cloneable {

  private static final long WORD_MASK = -1L;
  private static final int BITS_PER_WORD = 64;
  
  private final int size;
  
  // used to clear excess bits after operations.
  // Equal to WORD_MASK >>> BITS_PER_WORD - size
  private final long mask;

  private long data;
  
  public LongBitVector(int size) {
    this(size, 0);
    Preconditions.checkArgument(size >= 0 && size <= BITS_PER_WORD,
        "Size must be positive and <= {1} size: {2}", BITS_PER_WORD, size);
  }

  /**
   * Unsafe constructor. Keep it private.
   */
  private LongBitVector(int size, long data) {
    assert 64 - Long.numberOfLeadingZeros(data) <= size;
    this.size = size;
    this.data = data;
    mask = size == 0 ? 0 : WORD_MASK >>> BITS_PER_WORD - size;
  }

  private void checkSize(BitVector other) {
    if (size != other.size()) {
      throw new IllegalArgumentException(
          "Sizes must be equal. " + this.size + " : " + other.size());
    }
  }
  
  private void checkIndex(int bitIndex) {
    if (bitIndex < 0 | bitIndex >= size) {
      throw new IndexOutOfBoundsException("Bit index out of range: " + bitIndex);
    }
  }
  
  private void checkBounds(int fromIndex, int toIndex) {
    if (fromIndex < 0 | toIndex > size | fromIndex > toIndex) {
      throw new IndexOutOfBoundsException(
          "Range [" + fromIndex + ", " + toIndex + ") is invalid for this bit vector");
    }
  }
  
  @Override
  public void and(BitVector o) {
    checkSize(o);
    data &= o.toExactLong();
  }

  @Override
  public void andNot(BitVector o) {
    checkSize(o);
    data &= ~o.toExactLong();
  }

  @Override
  public int cardinality() {
    return Long.bitCount(data);
  }

  @Override
  public void clear() {
    data = 0;
  }

  @Override
  public void clear(int bitIndex) {
    checkIndex(bitIndex);
    data &= ~(1L << bitIndex);
  }

  @Override
  public void clear(int fromIndex, int toIndex) {
    checkBounds(fromIndex, toIndex);
    if (fromIndex != toIndex) {
      unsafeClearNonEmptySection(fromIndex, toIndex);
    }
  }

  private void unsafeClearNonEmptySection(int fromIndex, int toIndex) {
    data &= ~((WORD_MASK << fromIndex) & (WORD_MASK >>> -toIndex));
  }

  @Override
  public void copyFrom(BitVector from) {
    checkSize(from);
    data = from.toExactLong();
  }

  @Override
  public void flip(int bitIndex) {
    checkIndex(bitIndex);
    data ^= (1L << bitIndex);
  }

  @Override
  public void flip(int fromIndex, int toIndex) {
    checkBounds(fromIndex, toIndex);
    if (fromIndex != toIndex) {
      data ^= ((WORD_MASK << fromIndex) & (WORD_MASK >>> -toIndex));
    }
  }

  @Override
  public boolean get(int bitIndex) {
    checkIndex(bitIndex);
    return unsafeGet(bitIndex);
  }

  private boolean unsafeGet(int bitIndex) {
    return (data & (1L << bitIndex)) != 0;
  }
  
  @Override
  public void grayCode() {
    data ^= (data >>> 1);
  }

  @Override
  public void grayCodeInverse() {
    long localData = data;
    localData ^= localData >>> 1;
    localData ^= localData >>> 2;
    localData ^= localData >>> 4;
    localData ^= localData >>> 8;
    localData ^= localData >>> 16;
    localData ^= localData >>> 32;
    data = localData;
  }

  @Override
  public boolean increment() {
    // Check for overflow
    if (data == mask) {
      return false;
    }
    data++;
    return true;
  }

  @Override
  public boolean intersects(BitVector o) {
    checkSize(o);
    return (data & o.toExactLong()) != 0;
  }

  @Override
  public int length() {
    return BITS_PER_WORD - Long.numberOfLeadingZeros(data);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public int nextClearBit(int fromIndex) {
    Preconditions.checkArgument(fromIndex >= 0);
    if (fromIndex >= size) {
      return -1;
    }
    long w = ~data & (WORD_MASK << fromIndex);
    int tcb = Long.numberOfTrailingZeros(w);
    return tcb == size ? -1 : tcb;
  }

  @Override
  public int nextSetBit(int fromIndex) {
    Preconditions.checkArgument(fromIndex >= 0);
    if (fromIndex >= size) {
      return -1;
    }
    long w = data & (WORD_MASK << fromIndex);
    int tcb = Long.numberOfTrailingZeros(w);
    return tcb == 64 ? -1 : tcb;
  }

  @Override
  public void or(BitVector o) {
    checkSize(o);
    this.data |= o.toExactLong();
  }

  @Override
  public void rotate(int count) {
    final int localSize = size;
    count %= localSize;
    final long localData = data;
    if (count > 0) {
      data = ((localData >>> count) | (localData << localSize - count)) & mask;
    } else {
      data = ((localData >>> localSize + count) | (localData << -count)) & mask;
    }
  }

  @Override
  public void set(int bitIndex) {
    checkIndex(bitIndex);
    data |= (1L << bitIndex);
  }

  public void set(int bitIndex, boolean value) {
    if (value) {
      set(bitIndex);
    } else {
      clear(bitIndex);
    }
  }

  @Override
  public void set(int fromIndex, int toIndex) {
    checkBounds(fromIndex, toIndex);
    if (fromIndex != toIndex) {
      data |= ((WORD_MASK << fromIndex) & (WORD_MASK >>> -toIndex));
    }
  }

  @Override
  public void set(int fromIndex, int toIndex, boolean value) {
    if (value) {
      set(fromIndex, toIndex);
    } else {
      clear(fromIndex, toIndex);
    }
  }

  @Override
  public void xor(BitVector o) {
    checkSize(o);
    this.data ^= o.toExactLong();
  }

  @Override
  public boolean isEmpty() {
    return data == 0;
  }

  @Override
  public LongBitVector clone() {
    try {
      return (LongBitVector) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InternalError("Cloning error. ");
    }
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BitVector) {
      BitVector other = (BitVector) obj;
      // optimisation
      if (size <= 64) {
        return size == other.size() && data == other.toExactLong();
      } else {
        return size == other.size() && toBitSet().equals(other.toBitSet());
      }
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    // We imitate BitSet's hashcode implementation 
    long h = 1234 ^ data;
    int bitSetHashCode = (int) ((h >> 32) ^ h);
    return size + 31 * bitSetHashCode;
  }

  @Override
  public String toString() {
    return StringUtils.leftPad(Long.toBinaryString(data), size, '0');
  }

  @Override
  public BitSet toBitSet() {
    BitSet b = new BitSet(size);
    for (int i = 0; i < size; i++) {
      if (unsafeGet(i)) {
        b.set(i);
      }
    }
    return b;
  }

  @Override
  public long toLong() {
    return data;
  }

  @Override
  public BigInteger toBigInteger() {
    final BigInteger result;
    if (data >= 0) {
      result = BigInteger.valueOf(data);
    } else {
      BigInteger missingLowestBit = BigInteger.valueOf(data >>> 1).shiftLeft(1);
      if ((data & 1) == 1) {
        result = missingLowestBit.setBit(0);
      } else {
        result = missingLowestBit;
      }
    }
    return result;
  }
  
  public void copyFrom(long value) {
    Preconditions.checkArgument(64 - Long.numberOfLeadingZeros(value) <= size, "value doesn't fit");
    data = value;
  }

  @Override
  public int compareTo(BitVector o) {
    checkSize(o);
    final int cmp;
    // optimisation
    if (o.size() <= 64) {
      // 0, positives, Long.MAX_VALUE, Long.MIN_VALUE, negatives, -1
      long x = data + Long.MIN_VALUE;
      long y = o.toExactLong() + Long.MIN_VALUE;
      cmp = Long.compare(x, y);
      assert Integer.signum(cmp) == Integer.signum(
          BitSetComparator.INSTANCE.compare(toBitSet(), o.toBitSet()));
    } else {
      cmp = BitSetComparator.INSTANCE.compare(toBitSet(), o.toBitSet());
    }
    return cmp;
  }

  @Override
  public void copyFrom(BitSet from) {
    int localSize = size;
    long value = 0;
    for (int i = from.nextSetBit(0); i != -1; i = from.nextSetBit(i + 1)) {
      Preconditions.checkArgument(i < localSize, "bit set too large");
      value |= 1L << i;
    }
    data = value;
  }

  @Override
  public void copyFromSection(BitVector src, int fromIndex) {
    Preconditions.checkArgument(fromIndex >= 0, "fromIndex must be non-negative");
    int srcSize = src.size();
    int toIndex = fromIndex + size;
    Preconditions.checkArgument(toIndex <= srcSize, "not enough bits in src");
    long value;
    if (toIndex <= 64) {
      long srcData = src.toLong();
      value = (srcData >>> fromIndex) & mask;
    } else {
      value = 0;
      for (int i = src.nextSetBit(fromIndex); i < toIndex && i != -1; i = src.nextSetBit(i + 1)) {
        value |= 1L << (i - fromIndex);
      }
    }
    data = value;
  }

  @Override
  public long toExactLong() {
    return data;
  }

  @Override
  public void smallerEvenAndGrayCode() {
    long localData = data;
    if ((localData & 0x1) == 1) {
      assert size > 0;
      data = localData ^ (localData >>> 1) ^ 0x1;
    } else {
      if (localData != 0) {
        long dataMinusTwo = localData - 2;
        data = dataMinusTwo ^ (dataMinusTwo >>> 1);
      }
    }
  }

  @Override
  public void grayCodeRank(BitVector mu, BitVector w) {
    grayCodeRank(mu, w, true);
  }

  /**
   * Visible for testing.
   */
  void grayCodeRank(BitVector mu, BitVector w, boolean optimiseIfPossible) {
    int theirSize = mu.size();
    Preconditions.checkArgument(theirSize == w.size(), "mu/w size mismatch");
    int muLen = mu.length();
    long pow2pos = 1L;
    long value = 0;
    if (optimiseIfPossible & muLen <= 64) {
      // mu doesn't have any set bits over index 63
      long muLong = mu.toExactLong();
      // w might have some set bits over index 63, but they don't matter anyway
      long wLong = w.toLong();
      long pow2i = 1L;
      for (int i = 0; i < muLen; ++i) {
        if ((muLong & pow2i) != 0) {
          if ((wLong & pow2i) != 0) {
            value |= pow2pos;
          }
          pow2pos <<= 1;
        }
        pow2i <<= 1;
      }
    } else {
      for (int j = theirSize == 0 ? -1 : mu.nextSetBit(0); j != -1;
          j = j == theirSize - 1 ? -1 : mu.nextSetBit(j + 1)) {
        if (w.get(j)) {
          value |= pow2pos;
        }
        pow2pos <<= 1;
      }
    }
    assert pow2pos == 1L << mu.cardinality();
    Preconditions.checkArgument(1L << size == pow2pos, "wrong size");
    data = value;
  }

  @Override
  public int lowestDifferentBit() {
    long localData = data;
    final int value;
    if ((localData & 0x1L) == 0) {
      if (localData == 0) {
        value = 0;
      } else {
        value = Long.numberOfTrailingZeros(localData);
      }
    } else {
      if (localData == mask) {
        value = 0;
      } else {
        value = Long.numberOfTrailingZeros(~localData);
      }
    }
    assert value == 0 || (0 < value & value < size);
    return value;
  }

  @Override
  public void grayCodeRankInverse(BitVector mu, BitVector known, BitVector r) {
    int muSize = mu.size();
    Preconditions.checkArgument(size == muSize, "i/mu size mismatch");
    // Will fail if the sizes are different.
    Preconditions.checkArgument(!known.intersects(mu), "known and mu must not intersect");
    
    long muLong = mu.toExactLong();
    long knownLong = known.toExactLong();
    
    // Will check r.size() against mu.cardinality later.
    int rSize = r.size();
    Preconditions.checkArgument(rSize <= muSize, "r is too large");
    long rLong = r.toExactLong();
    
    long value = 0;
    int pos = 0;
    int muLength = mu.length();
    
    long pow2k = 1L;
    for (int k = 0; k < muLength; ++k) {
      if ((muLong & pow2k) != 0) {
        if ((rLong >> pos & 1L) != 0) {
          value |= pow2k;
        }
        ++pos;
      }
      pow2k <<= 1;
    }
    assert pos == mu.cardinality();
    Preconditions.checkArgument(pos == rSize, "r.size()/mu.cardinality() mismatch");
    int knownLength = known.length();
    for (int k = Math.max(muLength - 1, knownLength); --k >= 0; ) {
      pow2k = 1L << k;
      if ((muLong & pow2k) == 0) {
        assert (value & pow2k) == 0;
        if (((knownLong & pow2k) ^ (value >> 1 & pow2k)) != 0) {
          value |= pow2k;
        }
      }
    }
    data = value;
  }

  @Override
  public void copySectionFrom(int offset, BitVector src) {
    int srcSize = src.size();
    int toIndex = offset + srcSize;
    if (offset < 0 | toIndex > size) {
      throw new IndexOutOfBoundsException(
          "invalid range: offset=" + offset + " src.size()=" + src.size());
    }
    if (offset != toIndex) {
      unsafeClearNonEmptySection(offset, toIndex);
      long srcData = src.toExactLong();
      data |= srcData << offset;
    }
  }

  @Override
  public long[] toLongArray() {
    return size == 0 ? ArrayUtils.EMPTY_LONG_ARRAY : new long[] {data};
  }
  
  @Override
  public byte[] toBigEndianByteArray() {
    int n = MathUtils.bitCountToByteCount(size);
    byte[] a = new byte[n];
    long x = data;
    for (int i = 0; i < n; ) {
      a[n - ++i] = (byte) (x & 0xFF);
      x >>>= 8;
    }
    assert x == 0;
    return a;
  }

  @Override
  public void copyFrom(long[] array) {
    if (size == 0) {
      Preconditions.checkArgument(array.length == 0, "Array must be empty.");
    } else {
      Preconditions.checkArgument(array.length == 1, "Array length must be 1.");
      copyFrom(array[0]);
    }
  }

  @Override
  public void copyFromBigEndian(byte[] array) {
    int n = MathUtils.bitCountToByteCount(size);
    Preconditions.checkArgument(array.length == n, "Array length must be %s.", n);
    long x = 0;
    for (int i = 0; i < n - 1; ) {
      x |= (array[i++] & 0xFF);
      x <<= 8;
    }
    if (n != 0) {
      x |= (array[n - 1] & 0xFF);
    }
    copyFrom(x);
  }
 
  @Override
  public boolean areAllLowestBitsClear(int bitCount) {
    Preconditions.checkArgument(0 <= bitCount & bitCount <= size, "bitCount is out of range");
    // Only bitCount == 64 is affected by xoring with (bitCount >> 6). 
    return (data & (((1L << bitCount) ^ (bitCount >> 6)) - 1)) == 0;
  }

  @Override
  public void copyFrom(BigInteger s) {
    Preconditions.checkArgument(s.signum() >= 0, s);
    Preconditions.checkArgument(s.bitLength() <= size());
    // Note that the long value will be negative iff bitLength == 644 and bit 63
    // is set.
    copyFrom(s.longValue());
  }
}
