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

import static org.junit.Assert.assertArrayEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;

/**
 * @author Mehmet Akin
 * @author Daniel Aioanei
 * @author Radu Grigore
 */
public class BitVectorTest {

  private static final Random random = new Random(123);

  @Test
  public void compareTo() {
    for (Function<Integer, BitVector> factory1 : BitVectorFactories.values()) {
      for (Function<Integer, BitVector> factory2 : BitVectorFactories.values()) {
        checkCompareTo(factory1, factory2);
      }
    }
  }
  
  private void checkCompareTo(Function<Integer, BitVector> factory1, Function<Integer, BitVector> factory2) {
    for (int j = 0; j < 128; j++) {
      BitVector b = factory1.apply(j);
      boolean[] bBits = new boolean[j];
      for (int k = 0; k < j; ++k) {
        bBits[k] = random.nextBoolean();
      }
      for (int k = 0; k < j; ++k) {
        b.set(k, bBits[k]);
      }
      BitVector c = factory2.apply(j);
      boolean[] cBits = new boolean[j];
      for (int k = 0; k < j; ++k) {
        cBits[k] = random.nextBoolean();
      }
      for (int k = 0; k < j; ++k) {
        c.set(k, cBits[k]);
      }
      int cmp = b.compareTo(c);
      int k = j;
      while (--k != -1 && bBits[k] == cBits[k]) {}
      int expected = (k == -1) ? 0 : Boolean.compare(bBits[k], cBits[k]);
      Assert.assertEquals(Integer.signum(expected), Integer.signum(cmp));
      Assert.assertEquals(Integer.signum(cmp), -Integer.signum(-cmp));
      Assert.assertEquals(Integer.signum(cmp), b.toBigInteger().compareTo(c.toBigInteger()));
    }
  }

  @Test
  public void copyFromToBigInteger() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkCopyFromToBigInteger(factory);
    }
  }

  private void checkCopyFromToBigInteger(Function<Integer, BitVector> factory) {
    for (int j = 0; j < 128; j++) {
      BitVector b = factory.apply(j);
      boolean[] bits = new boolean[b.size()];
      for (int k = 0; k < b.size(); ++k) {
        bits[k] = random.nextBoolean();
      }
      BigInteger expected = BigInteger.ZERO;
      for (int k = 0; k < b.size(); ++k) {
        b.set(k, bits[k]);
        if (bits[k]) {
          expected = expected.setBit(k);
        }
      }
      Assert.assertEquals(expected, b.toBigInteger());
      BitVector revived = factory.apply(j);
      revived.copyFrom(expected);
      Assert.assertEquals(b, revived);
    }
  }

  @Test
  public void emptyRange() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkEmptyRange(factory);
    }
  }

  private void checkEmptyRange(Function<Integer, BitVector> factory) {
    for (int j = 0; j < 128; j++) {
      BitVector b = factory.apply(j);
      randomInit(b);
      BitVector expected = b.clone();
      b.clear(j, j);
      Assert.assertEquals(expected, b);
      factory.apply(0).copyFromSection(b, j);
      Assert.assertEquals(expected, b);
      b.flip(j, j);
      Assert.assertEquals(expected, b);
      b.set(j, j);
      Assert.assertEquals(expected, b);
      b.set(j, j, false);
      Assert.assertEquals(expected, b);
    }
  }

  public void randomInit(BitVector b) {
    for (int k = 0; k < b.size(); ++k) {
      b.set(k, random.nextBoolean());
    }
  }
  
  @Test
  public void set() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkSet(factory);
    }
  }

  private void checkSet(Function<Integer, BitVector> factory) {
    for (int j = 0; j < 128; j++) {
      BitVector b = factory.apply(j);
      for (int i = 0; i < j; i++) {
        b.set(i);
        Assert.assertTrue(b.get(i));
      }
    }
  }

  @Test
  public void setIllegalIndex() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkSetIllegalIndex(factory);
    }
  }

  private void checkSetIllegalIndex(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector b = factory.apply(i);
      try {
        b.set(-1);
        Assert.fail("IndexOutOfBoundsException expected");
      } catch (IndexOutOfBoundsException e) {
        // ok
      }
      try {
        b.set(i);
        Assert.fail("IndexOutOfBoundsException expected");
      } catch (IndexOutOfBoundsException e) {
        // ok
      }
    }
  }

  private void checkCardinality(Function<Integer, BitVector> factory) {
    for (int j = 0; j < 128; j++) {
      BitVector b = factory.apply(j);
      for (int i = 0; i < b.size(); i++) {
        Assert.assertEquals(i, b.cardinality());
        b.set(i);
      }
      Assert.assertEquals(b.size(), b.cardinality());
      for (int i = b.size() - 1; i >= 0; i--) {
        Assert.assertEquals(i + 1, b.cardinality());
        b.clear(i);
      }
      Assert.assertEquals(0, b.cardinality());
    }
  }

  @Test
  public void cardinality() {
    checkCardinality(BitVectorFactories.OPTIMAL);
    checkCardinality(BitVectorFactories.SLOW);
    checkCardinality(BitVectorFactories.LONG_ARRAY);
  }

  private static void setAllBits(BitVector b) {
    b.set(0, b.size());
  }

  public void setAllBitsToSize() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkSetAllBitsToSize(factory);
    }
  }

  private void checkSetAllBitsToSize(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector b = factory.apply(i);
      setAllBits(b);
      Assert.assertEquals(b.size(), b.cardinality());
    }
  }

  @Test
  public void clearAll() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkClearAll(factory);
    }
  }

  private void checkClearAll(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector b = factory.apply(i);
      b.clear();
      Assert.assertEquals(0, b.cardinality());
    }
  }

  private void checkClear(BitVector b) {
    setAllBits(b);
    for (int i = 0; i < b.size(); i++) {
      b.clear(i);
      Assert.assertFalse(b.get(i));
    }
  }

  @Test
  public void clear() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkClear(factory);
    }
  }

  private void checkClear(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      checkClear(factory.apply(i));
    }
  }

  @Test
  public void toLong() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkToLong(factory);
    }
  }

  private void checkToLong(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector bv = factory.apply(i);
      long reference = 0L;
      for (int j = 0; j < i; j += 2) {
        bv.set(j);
      }
      for (int j = 0; j < i && j < 64; j += 2) {
        reference |= 1L << j;
      }
      Assert.assertEquals(reference, bv.toLong());
    }
  }

  @Test
  public void copyFrom() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkCopyFrom(factory);
    }
  }
  
  private void checkCopyFrom(Function<Integer, BitVector> factory) {
    int size = 10;
    BitVector bv = factory.apply(size);
    for (long i = 1 << size; --i >= 0; ) {
      bv.copyFrom(i);
      Assert.assertEquals(i, bv.toLong());
    }

    final int bigSize = 1000;
    BitSet bs = new BitSet();
    for (int i = 0; i < bigSize; i += 3) {
      bs.set(i);
    }
    bv = factory.apply(bigSize);
    bv.copyFrom(bs);
    for (int i = 0; i < bigSize; ++i) {
      Assert.assertEquals(i % 3 == 0, bv.get(i));
    }
  }

  @Test
  public void copyFromBigEndian64Bits() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkCopyFromBigEndian64Bits(factory);
    }
  }

  private void checkCopyFromBigEndian64Bits(
      Function<Integer, BitVector> factory) {
    int bits = 10;
    for (int shift = 0; shift < 64 - bits; ++shift) {
      int size = bits + shift;
      BitVector bv = factory.apply(size);
      for (long i = 1 << bits; --i >= 0;) {
        for (long k : new long[] {
          i << shift, i << shift | ((1L << shift) - 1)}) {
          byte[] bigEndian = Longs.toByteArray(k);
          int n = (size + 7) >>> 3;
          for (int j = 0; j < bigEndian.length - n; ++j) {
            assert bigEndian[j] == 0;
          }
          byte[] bytes = Arrays.copyOfRange(
            bigEndian, bigEndian.length - n, bigEndian.length);
          bv.copyFromBigEndian(bytes);
          Assert.assertEquals(Long.bitCount(k), bv.cardinality());
          if (k != bv.toExactLong()) {
            bv.copyFromBigEndian(bytes);
            bv.toExactLong();
          }
          Assert.assertEquals(bv.getClass().toString(), k, bv.toExactLong());
        }
      }
    }
  }

  @Test
  public void length() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkLength(factory);
    }
  }

  private void checkLength(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    Assert.assertEquals(0, b.length());
    for (int j = 1; j < 128; j++) {
      Assert.assertEquals(0, b.length());
      for (int i = 0; i < b.length(); i++) {
        b.set(i);
        Assert.assertEquals(i + 1, b.length());
      }
    }
  }

  @Test
  public void clearRange() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkClearRange(factory);
    }
  }

  private void checkClearRange(Function<Integer, BitVector> factory) {
    String factoryString = factory.toString();
    for (int size = 0; size <= 100; size += 10) {
      BitVector b = factory.apply(size);
      // clearing (0, 0) shouldn't change anything.
      setAllBits(b);
      b.clear(0, 0);
      Assert.assertEquals(b.size(), b.cardinality());
      // clear all (0, len) second arg is exclusive.
      setAllBits(b);
      b.clear(0, b.size());
      try {
        Assert.assertEquals(factoryString, 0, b.cardinality());
      } catch (AssertionError e) {
        b.clear(0, b.size());
      }
      // Clear using all sizes and combinations up to length
      for (int i = 0; i < b.size(); i++) {
        for (int j = i; j < b.size(); j++) {
          setAllBits(b);
          b.clear(i, j);
          Assert.assertEquals(b.size() - (j - i), b.cardinality());
          // Test if it actually set correct bits to 0
          for (int k = i; k < j; k++) {
            Assert.assertFalse(b.get(k));
          }
        }
      }
    }
  }

  @Test
  public void setRange() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkSetRange(factory);
    }
  }

  private void checkSetRange(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 200; size += 10) {
      BitVector b = factory.apply(size);
      // setting (0, 0) shouldn't change anything.
      b.clear();
      b.set(0, 0);
      Assert.assertEquals(0, b.cardinality());
      // set all (0, len) second arg is exclusive.
      b.clear();
      b.set(0, b.size());
      Assert.assertEquals(b.size(), b.cardinality());
      // Set using all sizes and combinations up to length
      for (int i = 0; i < b.size(); i++) {
        for (int j = i; j < b.size(); j++) {
          b.clear();
          b.set(i, j);
          Assert.assertEquals((j - i), b.cardinality());
          // Test if it actually set correct bits to 1
          for (int k = i; k < j; k++) {
            Assert.assertTrue(b.get(k));
          }
        }
      }
    }
  }

  @Test
  public void flip() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkFlip(factory);
    }
  }

  private void checkFlip(Function<Integer, BitVector> factory) {
    for (int size = 0; size < 200; size++) {
      BitVector b = factory.apply(size);
      for (int i = 0; i < size; i++) {
        Assert.assertFalse(b.get(i));
        b.flip(i);
        Assert.assertTrue(b.get(i));
      }
    }
  }

  @Test
  public void flipRange() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkFlipRange(factory);
    }
  }

  private void checkFlipRange(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 200; size += 10) {
      BitVector b = factory.apply(size);
      // setting (0, 0) shouldn't change anything.
      b.clear();
      b.flip(0, 0);
      Assert.assertEquals(0, b.cardinality());
      // flip all (0, len) second arg is exclusive.
      b.clear();
      b.flip(0, b.size());
      Assert.assertEquals(b.size(), b.cardinality());
      // and set it back to 0
      b.flip(0, b.size());
      Assert.assertEquals(0, b.cardinality());
      // set using all sizes up to len
      for (int i = 0; i < b.size(); i++) {
        for (int j = i; j < b.size(); j++) {
          b.clear();
          b.flip(i, j);
          Assert.assertEquals((j - i), b.cardinality());
          for (int k = i; k < j; k++) {
            Assert.assertTrue(b.get(k));
          }
          b.flip(i, j);
          Assert.assertEquals(0, b.cardinality());
          setAllBits(b);
          b.flip(i, j);
          Assert.assertEquals(b.size() - (j - i), b.cardinality());
          for (int k = i; k < j; k++) {
            Assert.assertFalse(b.get(k));
          }
          b.flip(i, j);
          Assert.assertEquals(b.size(), b.cardinality());
        }
      }
    }
  }

  @Test
  public void get() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGet(factory);
    }
  }

  private void checkGet(Function<Integer, BitVector> factory) {
    for (int size = 0; size < 100; size += 10) {
      BitVector b = factory.apply(size);
      for (int i = 0; i < size; i++) {
        b.set(0, i);
        if (i <= 64) {
          checkGet(b, i, new LongBitVector(i));
        }
        checkGet(b, i, new BitSetBackedBitVector(i));
      }
    }
  }

  private void checkGet(BitVector b, int i, BitVector b2) {
    b2.copyFromSection(b, 0);
    Assert.assertEquals(i, b2.size());
    Assert.assertEquals(i, b2.cardinality());
  }

  @Test
  public void and() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkAnd(factory);
    }
  }

  private void checkAnd(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    BitVector b2 = b.clone();
    b.and(b2);
    Assert.assertTrue(b.isEmpty());
    for (int i = 1; i < 128; i++) {
      checkAnd(factory.apply(i));
    }
  }

  private void checkAnd(BitVector b) {
    BitVector b2 = b.clone();
    b2.clear();
    setAllBits(b);
    Assert.assertEquals(b.size(), b.cardinality());
    b.and(b2);
    Assert.assertEquals(0, b.cardinality());
    b.set(0);
    b2.set(0);
    b.and(b2);
    Assert.assertTrue(b.get(0));
    Assert.assertEquals(b, b2);
  }

  @Test
  public void andNotForSizeZero() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkAndNotForSizeZero(factory);
    }
  }

  private void checkAndNotForSizeZero(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    BitVector b2 = b.clone();
    b.andNot(b2);
    Assert.assertTrue(b.isEmpty());
  }

  @Test
  public void orForSizeZero() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkOrForSizeZero(factory);
    }
  }

  private void checkOrForSizeZero(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    BitVector b2 = b.clone();
    b.or(b2);
    Assert.assertTrue(b.isEmpty());
  }

  @Test
  public void xorForSizeZero() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkXorForSizeZero(factory);
    }
  }

  private void checkXorForSizeZero(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    BitVector b2 = b.clone();
    b.xor(b2);
    Assert.assertTrue(b.isEmpty());
  }

  @Test
  public void getNextClearBit() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGetNextClearBit(factory);
    }
  }

  private void checkGetNextClearBit(Function<Integer, BitVector> factory) {
    for (int size = 0; size < 128; size++) {
      BitVector b = factory.apply(size);
      for (int i = 0; i < b.size() - 1; i++) {
        b.set(i);
        Assert.assertEquals(i + 1, b.nextClearBit(0));
      }
    }
  }

  @Test
  public void getNextSetBit() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGetNextSetBit(factory);
    }
  }

  private void checkGetNextSetBit(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector b = factory.apply(i);
      setAllBits(b);
      for (int i1 = 0; i1 < b.size() - 1; i1++) {
        b.clear(i1);
        Assert.assertEquals(i1 + 1, b.nextSetBit(0));
      }
    }
  }

  @Test
  public void rotateAllZerosAndAllOnes() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkRotateAllZeroesAndAllOnes(factory);
    }
  }

  private void checkRotateAllZeroesAndAllOnes(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 100; size += 10) {
      BitVector b = factory.apply(size);
      b.clear();
      BitVector b2 = b.clone();
      for (int i = -size * 4; i < size * 4; i++) {
        b.rotate(i);
        Assert.assertEquals(b2, b);
      }
      setAllBits(b);
      b2 = b.clone();
      for (int i = -size * 4; i < size * 4; i++) {
        b.rotate(i);
        Assert.assertEquals(b2, b);
      }
    }
  }

  @Test
  public void rotate() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkRotate(factory);
    }
  }

  private void checkRotate(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 120; size += 20) {
      BitVector b = factory.apply(size);
      int step = size > 64 ? 39 : 5;
      for (int i = 0; i < size; i++) {
        for (int j = i; j < size; j += step) {
          b.clear();
          b.set(i, j);
          int cardinality = b.cardinality();
          for (int k = -size * 2; k < size * 2; k += step) {
            b.rotate(k);
            Assert.assertEquals(cardinality, b.cardinality());
          }
        }
      }
    }
  }

  private void checkGrayCode(BitVector b) {
    BitVector bo = b.clone();
    Assert.assertEquals(0, hammingDistance(b, bo));
    bo.grayCode();
    if (!b.increment()) {
      return;
    }
    b.grayCode();
    Assert.assertEquals(1, hammingDistance(b, bo));
  }

  private static int hammingDistance(BitVector x, BitVector y) {
    BitVector cx = x.clone();
    cx.xor(y);
    return cx.cardinality();
  }

  @Test
  public void grayCode() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGrayCode(factory);
    }
  }

  private void checkGrayCode(Function<Integer, BitVector> factory) {
    BitVector bv = factory.apply(0);
    bv.grayCode();
    Assert.assertTrue(bv.isEmpty());
    bv = factory.apply(1);
    bv.set(0);
    BitVector expected = bv.clone();
    bv.grayCode();
    Assert.assertEquals(expected, bv);
    for (int i = 0; i <= 200; i += 10) {
      BitVector b = factory.apply(i);
      for (int j = 0; j < 1000; ++j) {
        checkGrayCode(b);
      }
      b.flip(0, b.size());
      for (int j = 0; j < 1000; ++j) {
        checkGrayCode(b);
      }
      for (int j = 0; j < 1000; ++j) {
        randomInit(b);
        checkGrayCode(b);
      }
    }
  }

  private void checkGrayCodeInverse(BitVector b) {
    BitVector bo = b.clone();
    Assert.assertEquals(0, hammingDistance(b, bo));
    b.grayCode();
    b.grayCodeInverse();
    Assert.assertEquals(bo, b);
    b.increment();
  }

  @Test
  public void grayCodeInverse() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGrayCodeInverse(factory);
    }
  }

  private void checkGrayCodeInverse(Function<Integer, BitVector> factory) {
    BitVector bv = factory.apply(0);
    bv.grayCodeInverse();
    Assert.assertTrue(bv.isEmpty());
    bv = factory.apply(1);
    bv.set(0);
    BitVector expected = bv.clone();
    bv.grayCode();
    bv.grayCodeInverse();
    Assert.assertEquals(expected, bv);
    for (int i = 0; i <= 200; i += 10) {
      BitVector b = factory.apply(i);
      for (int j = 0; j < 1000; ++j) {
        checkGrayCodeInverse(b);
      }
      b.flip(0, b.size());
      for (int j = 0; j < 1000; ++j) {
        checkGrayCodeInverse(b);
      }
      for (int j = 0; j < 1000; ++j) {
        randomInit(b);
        checkGrayCodeInverse(b);
      }
    }
  }

  @Test
  public void equalsAndHashCode() {
    final int size = 10;
    for (long i = 1 << size; --i >= 0; ) {
      checkEqualsAndHashCode(size, i);
    }
    for (long i = Long.MIN_VALUE; --i >= Long.MAX_VALUE - 1024; ) {
      checkEqualsAndHashCode(64, i);
    }
    for (long i = Long.MIN_VALUE; ++i <= Long.MIN_VALUE + 1024; ) {
      checkEqualsAndHashCode(64, i);
    }
    final int bigSize = 1000;
    BitVector[] equals = new BitVector[4];
    equals[0] = new BitSetBackedBitVector(bigSize);
    equals[1] = new BitSetBackedBitVector(bigSize);
    equals[2] = new LongArrayBitVector(bigSize);
    equals[3] = new LongArrayBitVector(bigSize);
    for (int i = 0; i < 1000; ++i) {
      for (int j = 0; j < bigSize; ++j) {
        boolean value = random.nextBoolean();
        for (int k = 0; k < equals.length; ++k) {
          equals[k].set(j, value);
        }
      }
      for (int j = 0; j < equals.length; ++j) {
        for (int k = 0; k < equals.length; ++k) {
          MoreAsserts.checkEqualsAndHashCodeMethods(equals[j], equals[k], true);
        }
      }
    }
  }

  private void checkEqualsAndHashCode(int size, long i) {
    LongBitVector a = new LongBitVector(size);
    a.copyFrom(i);
    LongBitVector b = new LongBitVector(size);
    b.copyFrom(i);
    BitSetBackedBitVector c = new BitSetBackedBitVector(size);
    c.copyFrom(i);
    BitSetBackedBitVector d = new BitSetBackedBitVector(size);
    d.copyFrom(i);
    MoreAsserts.checkEqualsAndHashCodeMethods(a, b, true);
    MoreAsserts.checkEqualsAndHashCodeMethods(a, c, true);
    MoreAsserts.checkEqualsAndHashCodeMethods(a, d, true);
    MoreAsserts.checkEqualsAndHashCodeMethods(b, c, true);
    MoreAsserts.checkEqualsAndHashCodeMethods(b, d, true);
    MoreAsserts.checkEqualsAndHashCodeMethods(c, d, true);
  }
  
  @Test
  public void smallerEvenAndGrayCode() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkEntryVertex(factory);
    }
  }

  private void checkEntryVertex(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 1024; ++i) {
      BitVector bs = factory.apply(10);
      bs.copyFrom(i);
      bs.smallerEvenAndGrayCode();
      final BitVector expected;
      if (i == 0) {
        expected = TestUtils.createBitVector(0, 10);
      } else {
        int j = (i - 1) & ~1;
        expected = TestUtils.createBitVector(j ^ (j >>> 1), 10);
      }
      Assert.assertEquals(expected, bs);
    }
  }

  @Test
  public void smallerEvenAndGrayCodeForTwoBits() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkEntryVertexForTwoDimensions(factory);
    }
  }
  
  /**
   * This is a test for Theorem 2.10 much more than for the entry function
   * implementation itself.
   */
  public void checkEntryVertexForTwoDimensions(Function<Integer, BitVector> factory) {
    BitVector bs = factory.apply(2);
    bs.smallerEvenAndGrayCode();
    Assert.assertEquals(TestUtils.createBitVector(0, 2), bs);
    bs.set(0);
    bs.smallerEvenAndGrayCode();
    Assert.assertEquals(TestUtils.createBitVector(0, 2), bs);
    bs.set(1);
    bs.smallerEvenAndGrayCode();
    Assert.assertEquals(TestUtils.createBitVector(0, 2), bs);
    bs.set(0, 2);
    bs.smallerEvenAndGrayCode();
    Assert.assertEquals(TestUtils.createBitVector(3, 2), bs);
  }
  
  @Test
  public void grayCodeRankWithAllFreeBitsIsIdentityFunction() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGrayCodeRankWithAllFreeBitsIsIdentityFunction(factory);
    }
  }

  private void checkGrayCodeRankWithAllFreeBitsIsIdentityFunction(
      Function<Integer, BitVector> factory) {
    int bitCount = 15;
    BitVector mu = factory.apply(bitCount);
    for (int i = 0; i < bitCount; ++i) {
      mu.set(i);
    }
    BitVector bs = factory.apply(bitCount);
    int pow2n = 1 << bitCount;
    for (int i = 0; i < pow2n; ++i) {
      BitVector r = factory.apply(mu.cardinality());
      r.grayCodeRank(mu, bs);
      Assert.assertEquals(bs, r);
      bs.increment();
    }

    int bigBitCount = 200;
    mu = factory.apply(bigBitCount);
    bs = factory.apply(bigBitCount);
    BitVector r = factory.apply(bigBitCount);
    mu.set(0, bigBitCount);
    for (int i = 0; i < 1000; ++i) {
      for (int j = 0; j < bigBitCount; ++j) {
        bs.set(j, random.nextBoolean());
      }
      r.grayCodeRank(mu, bs);
      Assert.assertEquals(bs, r);
    }
  }

  @Test
  public void grayCodeRankRemovesConstrainedBits() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGrayCodeRankRemovesConstrainedBits(factory);
    }
  }
  
  public void checkGrayCodeRankRemovesConstrainedBits(
      Function<Integer, BitVector> factory) {
    BitVector mu = factory.apply(3);
    mu.set(1);
    BitVector bs = factory.apply(3);
    bs.set(0);
    BitVector r = factory.apply(1);
    r.grayCodeRank(mu, bs);
    Assert.assertTrue(r.isEmpty());
    bs.set(1);
    BitVector expected = TestUtils.createBitVector(1, 1);
    r.grayCodeRank(mu, bs);
    Assert.assertEquals(expected, r);
    bs.set(2);
    r.grayCodeRank(mu, bs);
    Assert.assertEquals(expected, r);
  }

  @Test
  public void grayCodeRankIsPlainPatternRank() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGrayCodeRankIsPlainPatternRank(factory);
    }
  }
  
  /**
   * What we're testing here is not so much grayCodeRank() as Theorem 3.5 in the
   * paper "Compact Hilbert Indices for Multi-Dimensional Data". This is still
   * useful compilable and runnable code documentation.
   */
  public void checkGrayCodeRankIsPlainPatternRank(Function<Integer, BitVector> factory) {
    final int bitCount = 7;
    final int n = 1 << bitCount;
    for (int i = 0; i < n; ++i) {
      BitVector freeBitsPattern = TestUtils.createBitVector(i, bitCount);
      List<BitVector> grayCodeInverseList = new ArrayList<BitVector>();
      BitVector bitSetCounter = factory.apply(bitCount);
      for (int j = 0; j < n; ++j) {
        BitVector counterCopy = bitSetCounter.clone();
        counterCopy.and(freeBitsPattern);
        if (counterCopy.equals(bitSetCounter)) {
          counterCopy.grayCodeInverse();
          grayCodeInverseList.add(counterCopy);
        }
        bitSetCounter.increment();
      }
      Collections.sort(grayCodeInverseList);
      for (int j = 0; j < grayCodeInverseList.size(); ++j) {
        BitVector jAsBitVector = TestUtils.createBitVector(j, freeBitsPattern.cardinality());
        BitVector r = factory.apply(freeBitsPattern.cardinality());
        r.grayCodeRank(freeBitsPattern, grayCodeInverseList.get(j));
        Assert.assertEquals(jAsBitVector, r);
      }
    }
  }

  @Test
  public void bigGrayCodeRankAndInverse() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkBigGrayCodeRankAndInverse(factory);
    }
  }

  public void checkBigGrayCodeRankAndInverse(Function<Integer, BitVector> factory) {
    final int bigSize = 200;
    for (int i = 0; i < 10000; ++i) {
      BitVector x = factory.apply(bigSize);
      BitVector m = factory.apply(bigSize);
      for (int j = 0; j < bigSize; ++j) {
        x.set(j, random.nextBoolean());
        m.set(j, random.nextBoolean());
      }
      BitVector r = factory.apply(m.cardinality());
      BitVector gx = x.clone();
      r.grayCodeRank(m, x);
      gx.grayCode();
      gx.andNot(m);
      BitVector shouldBeX = factory.apply(bigSize);
      shouldBeX.grayCodeRankInverse(m, gx, r);
      Assert.assertEquals(x, shouldBeX);
    }
  }
  
  @Test
  public void grayCodeRankInverse() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkGrayCodeRankInverse(factory);
    }
  }
  
  private static void checkGrayCodeRankInverse(Function<Integer, BitVector> factory) {
    for (int n = 0; n < 7; ++n) {
      final int pow2n = 1 << n;
      for (int i = 0; i < pow2n; ++i) {
        BitVector iAsBitVector = TestUtils.createBitVector(i, n);
        BitVector grayCodeInverse = iAsBitVector.clone();
        grayCodeInverse.grayCodeInverse();
        for (int j = 0; j < pow2n; ++j) {
          BitVector mu = TestUtils.createBitVector(j, n);
          BitVector grayCodeRank = factory.apply(mu.cardinality());          
          grayCodeRank.grayCodeRank(mu, grayCodeInverse);
          BitVector known = iAsBitVector.clone();
          known.andNot(mu);
          BitVector actual = factory.apply(n);
          actual.grayCodeRankInverse(mu, known, grayCodeRank);
          Assert.assertEquals(yetAnotherGrayCodeRankInverse(factory, n, mu, known, grayCodeRank), actual);
          actual.grayCode();
          Assert.assertEquals(iAsBitVector, actual);
        }
      }
    }
  }
  
  /**
   * A gray code rank inverse implementation that stays as close as possible to
   * the implementation in the Compact Hilbert Index technical report.
   */
  private static BitVector yetAnotherGrayCodeRankInverse(
      Function<Integer, BitVector> factory, int n, BitVector mu, BitVector known, BitVector r) {
    BitVector i = factory.apply(n);
    int j = mu.cardinality() - 1;
    for (int k = n - 1; k >= 0; --k) {
      if (mu.get(k)) {
        i.set(k, r.get(j));
        --j;
      } else {
        boolean bit = known.get(k) ^ (k == n - 1 ? false : i.get(k + 1));
        i.set(k, bit);
      }
    }
    assert j == -1;
    return i;
  }
  
  @Test
  public void firstDifferentLowestBitCount() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkIntraSubHypercubeDirection(factory);
    }
  }
  
  public void checkIntraSubHypercubeDirection(Function<Integer, BitVector> factory) {
    for (int n = 0; n < 10; ++n) {
      for (int i = 0; i < 1 << n; ++i) {
        final int expected;
        if (i == 0 | i == (1 << n) - 1) {
          expected = 0;
        } else {
          if ((i & 0x1) == 0) {
            expected = Integer.numberOfTrailingZeros(i);
            assert expected < n;
          } else {
            expected = Integer.numberOfTrailingZeros(~i);
            assert expected < n;
          }
        }
        BitVector bv = factory.apply(n);
        bv.copyFrom(i);
        Assert.assertEquals(expected, bv.lowestDifferentBit());
      }
    }
  }

  @Test
  public void areAllLowestBitsClear() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkAreAllLowestBitsClear(factory);
    }
  }
  
  private void checkAreAllLowestBitsClear(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 100; size += 10) {
      BitVector v = factory.apply(size);
      for (int i = 0; i <= size; ++i) {
        Assert.assertTrue(v.areAllLowestBitsClear(i));
      }
      for (int i = size; --i >= 0; ) {
        v.set(i);
        for (int j = 0; j <= size; ++j) {
          Assert.assertEquals(j <= i, v.areAllLowestBitsClear(j));
        }
      }
    }
  }

  @Test
  public void lowestDifferentBitForTwoDimensions() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkIntraSubHypercubeDirectionForTwoDimensions(factory);
    }
  }
  
  /**
   * This is a test for Lemma 2.8 much more than for the direction function
   * implementation itself.
   */
  public void checkIntraSubHypercubeDirectionForTwoDimensions(
      Function<Integer, BitVector> factory) {
    // dimension 0 is y; dimension 1 is x
    BitVector bv = factory.apply(2);
    bv.copyFrom(0);
    Assert.assertEquals(0, bv.lowestDifferentBit());
    bv.copyFrom(1);
    Assert.assertEquals(1, bv.lowestDifferentBit());
    bv.copyFrom(2);
    Assert.assertEquals(1, bv.lowestDifferentBit());
    bv.copyFrom(3);
    Assert.assertEquals(0, bv.lowestDifferentBit());
  }
  
  @Test
  public void copySectionFrom() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkCopySectionFrom(factory);
    }
  }
  
  private void checkCopySectionFrom(Function<Integer, BitVector> factory) {
    for (int bitCount = 0; bitCount < 6; ++bitCount) {
      BitVector bv = factory.apply(bitCount);
      for (int offset = 0; offset < bitCount; ++offset) {
        for (int len = 0; len < bitCount - offset; ++len) {
          bv.clear();
          BitVector src = factory.apply(len);
          for (int num = 0; num < 1 << len; ++num) {
            src.copyFrom(num);
            bv.copySectionFrom(offset, src);
            Assert.assertEquals(num << offset, bv.toExactLong());
          }
          bv.copyFrom((1 << bitCount) - 1);
          for (int num = 0; num < 1 << len; ++num) {
            src.copyFrom(num);
            bv.copySectionFrom(offset, src);
            long actual = bv.toExactLong();
            Assert.assertEquals(num, actual >>> offset & ((1 << len) - 1));
            Assert.assertEquals((1 << offset) - 1, actual & ((1 << offset) - 1));
            Assert.assertEquals((1 << bitCount - offset - len) - 1, actual >> offset + len);
          }
        }
      }
    }
  }

  @Test
  public void toLongArrayForSingleWord() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkToLongArrayForSingleWord(factory);
    }
  }

  @Test
  public void toLongArrayForTwoWords() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkToLongArrayForTwoWords(factory);
    }
  }
  
  @Test
  public void toBigEndianByteArrayForSingleWord() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkToBigEndianByteArrayForSingleWord(factory);
    }
  }

  @Test
  public void toBigEndianByteArrayForTwoWords() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkToBigEndianByteArrayForTwoWords(factory);
    }
  }

  @Test
  public void toBigEndianByteArrayAndCopyFromBigEndianAreInverse() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkToBigEndianByteArrayAndCopyFromBigEndianAreInverse(factory);
    }
  }

  private void checkToBigEndianByteArrayAndCopyFromBigEndianAreInverse(
      Function<Integer, BitVector> factory) {
    Random random = new Random(TestUtils.SEED);
    for (int sizeUpperLimit = 0; sizeUpperLimit < 128; ++sizeUpperLimit) {
      byte[] array = new byte[sizeUpperLimit];
      random.nextBytes(array);
      BitSet bs = BitSet.valueOf(array);
      ArrayUtils.reverse(array);
      int logicalSize = bs.length();
      int n = MathUtils.bitCountToByteCount(logicalSize);
      for (int i = 0; i < array.length - n; ++i) {
        assert array[i] == 0;
      }
      byte[] bigEndian = Arrays.copyOfRange(
        array, array.length - n, array.length);
      for (int size = logicalSize; size <= n << 3; ++size) {
        BitVector bv = factory.apply(size);
        bv.copyFromBigEndian(bigEndian);
        byte[] actual = bv.toBigEndianByteArray();
        assertArrayEquals(bigEndian, actual);
      }
    }
  }

  private void checkToLongArrayForTwoWords(Function<Integer, BitVector> factory) {
    checkEvenBitsToLongArray(factory);
    checkOddBitsToLongArray(factory);
  }

  private void checkToBigEndianByteArrayForTwoWords(Function<Integer, BitVector> factory) {
    checkEvenBitsToBigEndianByteArray(factory);
    checkOddBitsToBigEndianByteArray(factory);
  }

  private void checkEvenBitsToLongArray(Function<Integer, BitVector> factory) {
    BitVector v = factory.apply(128);
    for (int i = 0; i < 64; ++i) {
      v.set(2 * i);
    }
    long[] longArray = v.toLongArray();
    Assert.assertArrayEquals(new long[] {0x5555555555555555L, 0x5555555555555555L}, longArray);
  }

  private void checkEvenBitsToBigEndianByteArray(Function<Integer, BitVector> factory) {
    BitVector v = factory.apply(128);
    for (int i = 0; i < 64; ++i) {
      v.set(2 * i);
    }
    byte[] actual = v.toBigEndianByteArray();
    byte[] expected = Bytes.toArray(Collections.nCopies(16, (byte) 0x55));
    assertArrayEquals(expected, actual);
  }

  private void checkOddBitsToLongArray(Function<Integer, BitVector> factory) {
    BitVector v = factory.apply(128);
    for (int i = 0; i < 64; ++i) {
      v.set(2 * i + 1);
    }
    long[] longArray = v.toLongArray();
    Assert.assertArrayEquals(new long[] {0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL}, longArray);
  }
  
  private void checkOddBitsToBigEndianByteArray(Function<Integer, BitVector> factory) {
    BitVector v = factory.apply(128);
    for (int i = 0; i < 64; ++i) {
      v.set(2 * i + 1);
    }
    byte[] actual = v.toBigEndianByteArray();
    byte[] expected = Bytes.toArray(Collections.nCopies(16, (byte) 0xAA));
    assertArrayEquals(expected, actual);
  }

  private void checkToLongArrayForSingleWord(Function<Integer, BitVector> factory) {
    for (int size = 0; size < 10; ++size) {
      BitVector v = factory.apply(size);
      for (long i = 1 << size; --i >= 0; ) {
        checkToLongArrayForSingleWord(v, i);
      }
    }
    BitVector v2 = factory.apply(64);
    for (long i = Long.MIN_VALUE; --i >= Long.MAX_VALUE - 1024; ) {
      checkToLongArrayForSingleWord(v2, i);
    }
    for (long i = Long.MIN_VALUE; ++i <= Long.MIN_VALUE + 1024; ) {
      checkToLongArrayForSingleWord(v2, i);
    }
  }

  private void checkToBigEndianByteArrayForSingleWord(Function<Integer, BitVector> factory) {
    for (int size = 0; size < 10; ++size) {
      BitVector v = factory.apply(size);
      for (long i = 1 << size; --i >= 0; ) {
        checkToBigEndianByteArrayForSingleWord(v, i);
      }
    }
    BitVector v2 = factory.apply(64);
    for (long i = Long.MIN_VALUE; --i >= Long.MAX_VALUE - 1024; ) {
      checkToBigEndianByteArrayForSingleWord(v2, i);
    }
    for (long i = Long.MIN_VALUE; ++i <= Long.MIN_VALUE + 1024; ) {
      checkToBigEndianByteArrayForSingleWord(v2, i);
    }
  }
  
  private void checkToLongArrayForSingleWord(BitVector v, long i) {
    v.copyFrom(i);
    long[] array = v.toLongArray();
    if (v.size() == 0) {
      assert i == 0;
      Assert.assertEquals(0, array.length);
    } else {
      Assert.assertEquals(1, array.length);
      Assert.assertEquals(i, array[0]);
    }
  }

  private void checkToBigEndianByteArrayForSingleWord(BitVector v, long i) {
    v.copyFrom(i);
    byte[] array = v.toBigEndianByteArray();
    if (v.size() == 0) {
      assert i == 0;
      Assert.assertEquals(0, array.length);
    } else {
      int n = (v.size() + 7) >>> 3;
      Assert.assertEquals(n, array.length);
      byte[] expected = Longs.toByteArray(i);
      for (int j = 0; j < expected.length - n; ++j) {
        assert expected[j] == 0;
      }
      assertArrayEquals(Arrays.copyOfRange(expected, expected.length - n, expected.length), array);
    }
  }
 
  @Test
  public void copyFromLongArray() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkCopyFromLongArray(factory);
    }
  }
  
  private void checkCopyFromLongArray(Function<Integer, BitVector> factory) {
    BitVector v = factory.apply(128);
    long[] longArray = new long[2];
    for (long i = Long.MIN_VALUE; --i >= Long.MAX_VALUE - 80; ) {
      longArray[0] = i;
      for (long j = Long.MIN_VALUE; ++j <= Long.MIN_VALUE + 80; ) {
        longArray[1] = j;
        v.copyFrom(longArray);
        Assert.assertArrayEquals(longArray, v.toLongArray());
      }
    }
  }
  
  public static void testIncrementReturnValue() {
    for (Function<Integer, BitVector> factory : BitVectorFactories.values()) {
      checkIncrementReturnValue(factory);
    }
  }

  private static void checkIncrementReturnValue(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    Assert.assertFalse(b.increment());
    b = factory.apply(4);
    b.copyFrom(0xD);
    Assert.assertTrue(b.increment());
    Assert.assertTrue(b.increment());
    Assert.assertFalse(b.increment());
    Assert.assertFalse(b.increment());
    b = factory.apply(64);
    b.copyFrom(0xFFFFFFFFFFFFFFFEL);
    Assert.assertTrue(b.increment());
    Assert.assertFalse(b.increment());
    Assert.assertFalse(b.increment());
    b = factory.apply(128);
    b.copyFrom(new long[] {-3L, -1L});
    Assert.assertTrue(b.increment());
    Assert.assertTrue(b.increment());
    Assert.assertFalse(b.increment());
    Assert.assertEquals(-1L, b.toLong());
  }
}
