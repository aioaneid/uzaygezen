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

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;

/**
 * An implementation of {@code BitVector} based on an array of longs.
 * It also has methods for easy concatenation and for easy slicing.
 *
 * @author Radu Grigore
 */
public class LongArrayBitVector implements BitVector {
  private final long[] data;
  private final int size;

  private static final int BYTE = 8;
  private static final int WORD = 64;
  private static final int BYTES_IN_WORD = WORD / BYTE;

  private LongArrayBitVector(long[] data, int size) {
    assert (size + WORD - 1) / WORD == data.length;
    this.data = data;
    this.size = size;
    assert checkSanity();
  }

  public LongArrayBitVector(int size) {
    this(new long[(size + WORD - 1) / WORD], size);
  }

  public static LongArrayBitVector of(long value, int size) {
    LongArrayBitVector result = new LongArrayBitVector(size);
    result.copyFrom(value);
    return result;
  }

  public static LongArrayBitVector of(BitVector bitvector) {
    LongArrayBitVector result = new LongArrayBitVector(bitvector.size());
    result.copyFrom(bitvector);
    return result;
  }

  public static LongArrayBitVector concat(Iterable<BitVector> bitVectors) {
    int size = 0;
    for (BitVector bv : bitVectors) {
      size += bv.size();
    }
    long[] data = new long[(size + WORD - 1) / WORD];
    int i = 0;
    for (BitVector bv : bitVectors) {
      copy(toPotentiallySharedLongArray(bv), 0, data, i, bv.size());
      i += bv.size();
    }
    return new LongArrayBitVector(data, size);
  }

  public static LongArrayBitVector concat(BitVector... bits) {
    return concat(Arrays.asList(bits));
  }

  private void checkRange(int low, int high, int... values) {
    for (int v : values) {
      if (v < low || v > high) {
        throw new IndexOutOfBoundsException(
            "index " + v + " should be in [" + low + ".." + high + "]");
      }
    }
  }

  /* Should always return true. */
  private boolean checkSanity() {
    return (size & (WORD - 1)) == 0 || (data[data.length - 1] & (-1L << size)) == 0L;
  }

  @Override
  public boolean isEmpty() {
    for (int i = 0; i < data.length; ++i) {
      if (data[i] != 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void set(int bitIndex) {
    checkRange(0, size - 1, bitIndex);
    data[bitIndex / WORD] |= 1L << bitIndex;
    assert checkSanity();
  }

  @Override
  public void set(int bitIndex, boolean value) {
    if (value) {
      set(bitIndex);
    } else {
      clear(bitIndex);
    }
  }

  @Override
  public void set(int fromIndex, int toIndex) {
    checkRange(0, size, fromIndex, toIndex);
    int fromBucket = fromIndex / WORD;
    int toBucket = toIndex / WORD;
    if (fromBucket == toBucket) {
      if (fromBucket != data.length) {
        data[fromBucket] |= (1L << toIndex) - (1L << fromIndex);
      } else {
        assert fromIndex == toIndex;
        assert toIndex == size;
      }
    } else {
      data[fromBucket] |= -(1L << fromIndex);
      if (toBucket != data.length) {
        data[toBucket] |= (1L << toIndex) - 1L;
      } else {
        assert toIndex == size;
      }
      Arrays.fill(data, fromBucket + 1, toBucket, -1L);
    }
    assert checkSanity();
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
  public boolean get(int bitIndex) {
    checkRange(0, size - 1, bitIndex);
    return (data[bitIndex / WORD] & (1L << bitIndex)) != 0L;
  }

  @Override
  public void copyFromSection(BitVector src, int fromIndex) {
    checkRange(0, src.size() - size, fromIndex);
    clear();
    copyFromSection(toPotentiallySharedLongArray(src), fromIndex);
    assert checkSanity();
  }

  @Override
  public void copySectionFrom(int offset, BitVector src) {
    checkRange(0, size - src.size(), offset);
    copySectionFrom(offset, toPotentiallySharedLongArray(src), src.size());
    assert checkSanity();
  }

  public LongArrayBitVector slice(int from, int to) {
    Preconditions.checkArgument(0 <= from && from <= to && to <= size);
    long[] newData = new long[(to - from + WORD - 1) / WORD];
    copy(data, from, newData, 0, to - from);
    return new LongArrayBitVector(newData, to - from);
  }

  public LongArrayBitVector[] slice(int... widths) {
    LongArrayBitVector[] result = new LongArrayBitVector[widths.length];
    int widthsSum = 0;
    for (int i = 0; i < widths.length; ++i) {
      Preconditions.checkArgument(widths[i] >= 0);
      Preconditions.checkArgument(widthsSum + widths[i] <= size);
      long[] newData = new long[(widths[i] + WORD - 1) / WORD];
      copy(data, widthsSum, newData, 0, widths[i]);
      result[i] = new LongArrayBitVector(newData, widths[i]);
      widthsSum += widths[i];
    }
    return result;
  }

  @Override
  public int length() {
    int i;
    for (i = data.length; --i >= 0 && data[i] == 0L;);
    if (i < 0) {
      return 0;
    }
    return WORD * (i + 1) - Long.numberOfLeadingZeros(data[i]);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void clear() {
    Arrays.fill(data, 0L);
  }

  @Override
  public void clear(int bitIndex) {
    checkRange(0, size - 1, bitIndex);
    data[bitIndex / WORD] &= ~(1L << bitIndex);
    assert checkSanity();
  }

  @Override
  public void clear(int fromIndex, int toIndex) {
    checkRange(0, size, fromIndex, toIndex);
    int fromBucket = fromIndex / WORD;
    int toBucket = toIndex / WORD;
    if (fromBucket == toBucket) {
      if (fromBucket != data.length) {
        data[fromBucket] &= ~((1L << toIndex) - (1L << fromIndex));
      } else {
        assert fromIndex == toIndex;
        assert toIndex == size;
      }
    } else {
      assert fromIndex != size;
      data[fromBucket] &= ~-(1L << fromIndex);
      if (toBucket != data.length) {
        data[toBucket] &= ~((1L << toIndex) - 1L);
      } else {
        assert toIndex == size;
      }
      Arrays.fill(data, fromBucket + 1, toBucket, 0L);
    }
    assert checkSanity();
  }

  @Override
  public int cardinality() {
    int result = 0;
    for (int i = 0; i < data.length; ++i) {
      result += Long.bitCount(data[i]);
    }
    return result;
  }

  @Override
  public void flip(int bitIndex) {
    checkRange(0, size - 1, bitIndex);
    data[bitIndex / WORD] ^= 1L << bitIndex;
    assert checkSanity();
  }

  @Override
  public void flip(int fromIndex, int toIndex) {
    checkRange(0, size, fromIndex, toIndex);
    int fromBucket = fromIndex / WORD;
    int toBucket = toIndex / WORD;
    if (fromBucket == toBucket) {
      if (fromBucket != data.length) {
        data[fromBucket] ^= (1L << toIndex) - (1L << fromIndex);
      } else {
        assert fromIndex == toIndex;
        assert toIndex == size;
      }
    } else {
      data[fromBucket] ^= -(1L << fromIndex);
      if (toBucket != data.length) {
        data[toBucket] ^= (1L << toIndex) - 1L;
      } else {
        assert toIndex == size;
      }
      for (++fromBucket; fromBucket < toBucket; ++fromBucket) {
        data[fromBucket] ^= -1L;
      }
    }
    assert checkSanity();
  }

  @Override
  public boolean intersects(BitVector set) {
    return intersects(toPotentiallySharedLongArray(set));
  }

  @Override
  public int nextSetBit(int fromIndex) {
    checkRange(0, size, fromIndex);
    if (size == 0) {
      return -1;
    }
    int fromBucket = fromIndex / WORD;
    long word = data[fromBucket] & -(1L << fromIndex);
    while (word == 0L && ++fromBucket < data.length) {
      word = data[fromBucket];
    }
    if (fromBucket == data.length) {
      return -1;
    }
    return WORD * fromBucket + Long.numberOfTrailingZeros(word);
  }

  @Override
  public int nextClearBit(int fromIndex) {
    checkRange(0, size, fromIndex);
    if (size == 0) {
      return -1;
    }
    int fromBucket = fromIndex / WORD;
    long word = data[fromBucket] & -(1L << fromIndex);
    while (word == -1L && ++fromBucket < data.length) {
      word = data[fromBucket];
    }
    if (fromBucket == data.length) {
      return -1;
    }
    int result = WORD * fromBucket + Long.numberOfTrailingZeros(~word);
    return result >= size ? -1 : result;
  }

  @Override
  public boolean increment() {
    int i;
    for (i = 0; i < data.length && data[i] == -1L; ++i) {
      data[i] = 0L;
    }
    if (i == data.length) {
      Arrays.fill(data, -1L);
      assert checkSanity();
      return false;
    }
    if (i == data.length - 1 && data[i] == (1L << size) - 1L) {
      Arrays.fill(data, -1L);
      data[i] = (1L << size) - 1L;
      assert checkSanity();
      return false;
    }
    ++data[i];
    assert checkSanity();
    return true;
  }

  /** If it is non-zero then it decreases the value by one and returns
   *  {@code true}; otherwise it does nothing and returns false. */
  public boolean decrement() {
    int i;
    for (i = 0; i < data.length && data[i] == 0L; ++i) {
      data[i] = -1L;
    }
    if (i == data.length) {
      Arrays.fill(data, 0L);
      assert checkSanity();
      return false;
    }
    --data[i];
    assert checkSanity();
    return true;
  }

  @Override
  public void andNot(BitVector o) {
    Preconditions.checkArgument(size == o.size());
    andNot(toPotentiallySharedLongArray(o));
    assert checkSanity();
  }

  @Override
  public void and(BitVector o) {
    Preconditions.checkArgument(size == o.size());
    and(toPotentiallySharedLongArray(o));
    assert checkSanity();
  }

  @Override
  public void or(BitVector o) {
    Preconditions.checkArgument(size == o.size());
    or(toPotentiallySharedLongArray(o));
    assert checkSanity();
  }

  @Override
  public void xor(BitVector o) {
    Preconditions.checkArgument(size == o.size());
    xor(toPotentiallySharedLongArray(o));
    assert checkSanity();
  }

  @Override
  public void rotate(int count) {
    long[] old = Arrays.copyOf(data, data.length);
    count = ((count % size) + size) % size;
    copy(old, 0, data, size - count, count);
    copy(old, count, data, 0, size - count);
    assert checkSanity();
  }

  @Override
  public void grayCode() {
    if (size == 0) {
      return;
    }
    int i;
    for (i = 0; i < data.length - 1; ++i) {
      data[i] ^= data[i] >>> 1;
      if ((data[i + 1] & 1L) != 0) {
        data[i] ^= 1L << (WORD - 1);
      }
    }
    data[i] ^= data[i] >>> 1;
    assert checkSanity();
  }

  /* Let b[i] be the i-th bit of the result and a[i] the i-th bit in the
   * pre-state. Then b[i] = a[i] ^ a[i+1] ^ ... ^ a[size-1] = a[i] ^ b[i+1].
   * So it can be computed with one loop from size-1 to 0. But for longs
   * we can compute y = x ^ (x/2) ^ (x/4) ^ ... in logarithmic time. */
  @Override
  public void grayCodeInverse() {
    boolean last = false;
    for (int i = data.length; --i >= 0;) {
      data[i] ^= data[i] >>> 1;
      data[i] ^= data[i] >>> 2;
      data[i] ^= data[i] >>> 4;
      data[i] ^= data[i] >>> 8;
      data[i] ^= data[i] >>> 16;
      data[i] ^= data[i] >>> 32;
      if (last) {
        data[i] = ~data[i];
      }
      last = (data[i] & 1L) != 0;
    }
    assert checkSanity();
  }

  @Override
  public void smallerEvenAndGrayCode() {
    if (!decrement()) {
      return;
    }
    data[0] &= ~1L;
    grayCode();
    assert checkSanity();
  }

  @Override
  public int lowestDifferentBit() {
    if (size == 0) {
      return 0;
    }
    int i;
    boolean last = (data[0] & 1L) != 0;
    for (i = 0; i < data.length && data[i] == (last ? -1L : 0L); ++i);
    /* All bits in data[i-1], data[i-1], ..., data[0] are equal to last... */
    if (i == data.length) {
      return 0;
    }
    /* ... and in data[i] there is a bit != last. */
    int result = WORD * i + Long.numberOfTrailingZeros(last ? ~data[i] : data[i]);
    return result >= size ? 0 : result;
  }

  @Override
  public boolean areAllLowestBitsClear(int bitCount) {
    checkRange(0, size, bitCount);
    int i;
    int bucket = bitCount / WORD;
    for (i = 0; i < bucket && data[i] == 0L; ++i);
    return i == bucket && (size == 0 || (data[i] & ((1L << bitCount) - 1L)) == 0L);
  }

  /* This function selects bits of w according to the mask mu and
   * packs them. */
  @Override
  public void grayCodeRank(BitVector mu, BitVector w) {
    Preconditions.checkArgument(mu.size() == w.size());
    Preconditions.checkArgument(size == mu.cardinality());
    clear();
    int j = 0;
    if (mu.size() == 0) {
      assert checkSanity();
      return;
    }
    for (int i = mu.nextSetBit(0); i >= 0; i = i + 1 < mu.size() ? mu.nextSetBit(i + 1) : -1) {
      if (w.get(i)) {
        set(j);
      }
      ++j;
    }
    assert checkSanity();
  }

  /* We have x=R(m,g(x)&~m,r(m,x)), where
   *   g=grayCode, G=grayCodeInverse,
   *   r=grayCodeRank, R=grayCodeRankInverse.
   * In other words we know the bits of x for the positions given by the
   * mask mu and the bits of g(x) for the other positions. We want to
   * produce x. */
  @Override
  public void grayCodeRankInverse(
      BitVector mu,
      BitVector known,
      BitVector r) {
    Preconditions.checkArgument(size == mu.size());
    Preconditions.checkArgument(size == known.size());
    Preconditions.checkArgument(r.size() == mu.cardinality());
    Preconditions.checkArgument(!known.intersects(mu));
    clear();
    int j = r.size() - 1;
    boolean previous = false, current;
    for (int i = size - 1; i >= 0; --i) {
      current = mu.get(i) ? r.get(j--) : (previous ^ known.get(i));
      if (current) {
        set(i);
      }
      previous = current;
    }
  }

  @Override
  public void copyFrom(BitVector from) {
    Preconditions.checkArgument(size == from.size());
    copyFrom(toPotentiallySharedLongArray(from));
    assert checkSanity();
  }

  @Override
  public void copyFrom(BitSet from) {
    Preconditions.checkArgument(size >= from.length());
    clear();
    for (int i = from.nextSetBit(0); i >= 0; i = from.nextSetBit(i + 1)) {
      set(i);
    }
    assert checkSanity();
  }

  @Override
  public LongArrayBitVector clone() {
    return new LongArrayBitVector(Arrays.copyOf(data, data.length), size);
  }

  @Override
  public BitSet toBitSet() {
    BitSet result = new BitSet(size);
    for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
      result.set(i);
    }
    return result;
  }

  @Override
  public long toLong() {
    if (size == 0) {
      return 0L;
    } else {
      return data[0];
    }
  }

  @Override
  public long toExactLong() {
    Preconditions.checkState(size - length() <= WORD);
    return toLong();
  }

  @Override
  public void copyFrom(long d) {
    Preconditions.checkArgument(WORD - Long.numberOfLeadingZeros(d) <= size);
    clear();
    if (size > 0) {
      data[0] = d;
    }
  }

  @Override
  public long[] toLongArray() {
    return Arrays.copyOf(data, data.length);
  }

  @Override
  public byte[] toBigEndianByteArray() {
    int n = MathUtils.bitCountToByteCount(size);
    byte[] a = new byte[n];
    long x = 0;
    int wordIndex = -1;
    for (int i = 0; i < n; ) {
      if ((i & 7) == 0) {
        assert x == 0;
        x = data[++wordIndex];
      }
      a[n - ++i] = (byte) (x & 0xFF);
      x >>>= 8;
    }
    assert x == 0;
    assert wordIndex == data.length - 1;
    return a;
  }

  @Override
  public void copyFrom(long[] array) {
    Preconditions.checkArgument(data.length == array.length);
    System.arraycopy(array, 0, data, 0, data.length);
    assert checkSanity();
  }

  @Override
  public void copyFromBigEndian(byte[] array) {
    ArrayUtils.reverse(array);
    try {
      copyFrom(array);
    } finally {
      ArrayUtils.reverse(array);
    }
    assert checkSanity();
  }
  
  public void copyFrom(byte[] array) {
    Preconditions.checkArgument((size + BYTE - 1) / BYTE == array.length);
    clear();
    for (int i = 0; i < array.length; ++i) {
      data[i / BYTES_IN_WORD] |= ((long) array[i] & 0xff) << (i % BYTES_IN_WORD * BYTE);
    }
    assert checkSanity();
  }

  @Override
  public int hashCode() {
    // Imitate BitSet's hashCode.
    long h = 1234;
    for (int i = data.length; --i >= 0;) {
      h ^= data[i] * (i + 1);
    }
    return size + 31 * (int) ((h >> 32) ^ h);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BitVector)) {
      return false;
    }
    BitVector other = (BitVector) o;
    return size == other.size() &&
        Arrays.equals(data, toPotentiallySharedLongArray(other));
  }

  @Override
  public int compareTo(BitVector o) {
    return compareTo(toPotentiallySharedLongArray(o));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[LongArrayBitVector: size=");
    sb.append(Integer.toString(size));
    sb.append(" 0x");
    if (size > 0) {
      sb.append(String.format("%X", data[data.length - 1]));
    } else {
      sb.append("0");
    }
    for (int i = data.length - 1; --i >= 0;) {
      sb.append(String.format("%016X", data[i]));
    }
    sb.append("]");
    return sb.toString();
  }

  private static long[] toPotentiallySharedLongArray(BitVector vector) {
    if (vector instanceof LongArrayBitVector) {
      return ((LongArrayBitVector) vector).data;
    } else {
      return vector.toLongArray();
    }
  }

  private void copyFromSection(long[] src, int fromIndex) {
    copy(src, fromIndex, data, 0, size);
  }

  private void copySectionFrom(int offset, long[] src, int srcSize) {
    copy(src, 0, data, offset, srcSize);
  }

  private void andNot(long[] other) {
    for (int i = 0; i < data.length; ++i) {
      data[i] &= ~other[i];
    }
  }

  private void and(long[] other) {
    for (int i = 0; i < data.length; ++i) {
      data[i] &= other[i];
    }
  }

  private void or(long[] other) {
    for (int i = 0; i < data.length; ++i) {
      data[i] |= other[i];
    }
  }

  private void xor(long[] other) {
    for (int i = 0; i < data.length; ++i) {
      data[i] ^= other[i];
    }
  }

  private int compareTo(long[] other) {
    int i;
    for (i = data.length; --i >= 0 && data[i] == other[i];);
    if (i < 0) {
      return 0;
    }
    if (data[i] < 0 && other[i] > 0) {
      return +1;
    }
    if (data[i] > 0 && other[i] < 0) {
      return -1;
    }
    if (data[i] < other[i]) {
      return -1;
    } else {
      return +1;
    }
  }

  private boolean intersects(long[] other) {
    for (int i = 0; i < data.length; ++i) {
      if ((data[i] & other[i]) != 0) {
        return true;
      }
    }
    return false;
  }

  private static void copy(
      long[] source,
      int srcIndex,
      long[] destination,
      int destIndex,
      int length) {
    while (length > 0) {
      int toCopy = length;
      toCopy = Math.min(toCopy, left(srcIndex));
      toCopy = Math.min(toCopy, left(destIndex));
      int fromBucket = srcIndex / WORD;
      int toBucket = destIndex / WORD;
      long w = (mask(toCopy) << srcIndex) & source[fromBucket];
      destination[toBucket] &= ~(mask(toCopy) << destIndex);
      destination[toBucket] |= (w >>> srcIndex) << destIndex;
      srcIndex += toCopy;
      destIndex += toCopy;
      length -= toCopy;
    }
  }

  private static int left(int index) {
    return (index + WORD) / WORD * WORD - index;
  }

  private static long mask(int k) {
    if (k == WORD) {
      return -1L;
    } else {
      return (1L << k) - 1L;
    }
  }
}
