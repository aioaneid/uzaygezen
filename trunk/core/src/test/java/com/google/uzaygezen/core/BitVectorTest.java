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

import com.google.common.base.Function;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Mehmet Akin
 * @author Daniel Aioanei
 */
public class BitVectorTest extends TestCase {

  public void testSet() {
    checkSet(BitVectorFactories.OPTIMAL);
    checkSet(BitVectorFactories.SLOW);
  }

  private void checkSet(Function<Integer, BitVector> factory) {
    for (int j = 0; j < 128; j++) {
      BitVector b = factory.apply(j);
      for (int i = 0; i < j; i++) {
        b.set(i);
        assertTrue(b.get(i));
      }
    }
  }

  public void testSetIllegalIndex() {
    checkSetIllegalIndex(BitVectorFactories.OPTIMAL);
    checkSetIllegalIndex(BitVectorFactories.SLOW);
  }

  private void checkSetIllegalIndex(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector b = factory.apply(i);
      try {
        b.set(-1);
        fail("IndexOutOfBoundsException expected");
      } catch (IndexOutOfBoundsException e) {
        // ok
      }
      try {
        b.set(i);
        fail("IndexOutOfBoundsException expected");
      } catch (IndexOutOfBoundsException e) {
        // ok
      }
    }
  }

  private void checkCardinality(Function<Integer, BitVector> factory) {
    for (int j = 0; j < 128; j++) {
      BitVector b = factory.apply(j);
      for (int i = 0; i < b.size(); i++) {
        assertEquals(i, b.cardinality());
        b.set(i);
      }
      assertEquals(b.size(), b.cardinality());
      for (int i = b.size() - 1; i >= 0; i--) {
        assertEquals(i + 1, b.cardinality());
        b.clear(i);
      }
      assertEquals(0, b.cardinality());
    }
  }

  public void testCardinality() {
    checkCardinality(BitVectorFactories.OPTIMAL);
    checkCardinality(BitVectorFactories.SLOW);
  }

  private static void setAllBits(BitVector b) {
    b.set(0, b.size());
  }

  public void setAllBitsToSize() {
    checkSetAllBitsToSize(BitVectorFactories.OPTIMAL);
    checkSetAllBitsToSize(BitVectorFactories.SLOW);
  }

  private void checkSetAllBitsToSize(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector b = factory.apply(i);
      setAllBits(b);
      assertEquals(b.size(), b.cardinality());
    }
  }

  public void testClearAll() {
    checkClearAll(BitVectorFactories.OPTIMAL);
    checkClearAll(BitVectorFactories.SLOW);
  }

  private void checkClearAll(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector b = factory.apply(i);
      b.clear();
      assertEquals(0, b.cardinality());
    }
  }

  private void checkClear(BitVector b) {
    setAllBits(b);
    for (int i = 0; i < b.size(); i++) {
      b.clear(i);
      assertFalse(b.get(i));
    }
  }

  public void testClear() {
    checkClear(BitVectorFactories.OPTIMAL);
    checkClear(BitVectorFactories.SLOW);
  }

  private void checkClear(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      checkClear(factory.apply(i));
    }
  }

  public void testToLong() {
    checkToLong(BitVectorFactories.OPTIMAL);
    checkToLong(BitVectorFactories.SLOW);
  }

  private void checkToLong(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    assertEquals(0, b.toLong());
    for (int i = 0; i < 128; i++) {
      checkClear(factory.apply(i));
    }
  }

  public void testCopyFrom() {
    checkCopyFrom(BitVectorFactories.OPTIMAL);
    checkCopyFrom(BitVectorFactories.SLOW);
  }
  
  private void checkCopyFrom(Function<Integer, BitVector> factory) {
    int size = 10;
    BitVector bv = factory.apply(size);
    for (long i = 1 << size; --i >= 0; ) {
      bv.copyFrom(i);
      assertEquals(i, bv.toLong());
    }
  }

  public void testLength() {
    checkLength(BitVectorFactories.OPTIMAL);
    checkLength(BitVectorFactories.SLOW);
  }

  private void checkLength(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    assertEquals(0, b.length());
    for (int j = 1; j < 128; j++) {
      BitVector v = factory.apply(j);
      assertEquals(0, b.length());
      for (int i = 0; i < b.length(); i++) {
        b.set(i);
        assertEquals(i + 1, b.length());
      }
    }
  }

  public void testClearRange() {
    checkClearRange(BitVectorFactories.OPTIMAL);
    checkClearRange(BitVectorFactories.SLOW);
  }

  private void checkClearRange(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 100; size += 10) {
      BitVector b = factory.apply(size);
      // clearing (0, 0) shouldn't change anything.
      setAllBits(b);
      b.clear(0, 0);
      assertEquals(b.size(), b.cardinality());
      // clear all (0, len) second arg is exclusive.
      setAllBits(b);
      b.clear(0, b.size());
      assertEquals(0, b.cardinality());
      // Clear using all sizes and combinations up to length
      for (int i = 0; i < b.size(); i++) {
        for (int j = i; j < b.size(); j++) {
          setAllBits(b);
          b.clear(i, j);
          assertEquals(b.size() - (j - i), b.cardinality());
          // Test if it actually set correct bits to 0
          for (int k = i; k < j; k++) {
            assertFalse(b.get(k));
          }
        }
      }
    }
  }

  public void testSetRange() {
    checkSetRange(BitVectorFactories.OPTIMAL);
    checkSetRange(BitVectorFactories.SLOW);
  }

  private void checkSetRange(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 80; size += 10) {
      BitVector b = factory.apply(size);
      // setting (0, 0) shouldn't change anything.
      b.clear();
      b.set(0, 0);
      assertEquals(0, b.cardinality());
      // set all (0, len) second arg is exclusive.
      b.clear();
      b.set(0, b.size());
      assertEquals(b.size(), b.cardinality());
      // Set using all sizes and combinations up to length
      for (int i = 0; i < b.size(); i++) {
        for (int j = i; j < b.size(); j++) {
          b.clear();
          b.set(i, j);
          assertEquals((j - i), b.cardinality());
          // Test if it actually set correct bits to 1
          for (int k = i; k < j; k++) {
            assertTrue(b.get(k));
          }
        }
      }
    }
  }

  public void testFlip() {
    checkFlip(BitVectorFactories.OPTIMAL);
    checkFlip(BitVectorFactories.SLOW);
  }

  private void checkFlip(Function<Integer, BitVector> factory) {
    for (int size = 0; size < 80; size++) {
      BitVector b = factory.apply(size);
      for (int i = 0; i < size; i++) {
        assertFalse(b.get(i));
        b.flip(i);
        assertTrue(b.get(i));
      }
    }
  }

  public void testFlipRange() {
    checkFlipRange(BitVectorFactories.OPTIMAL);
    checkFlipRange(BitVectorFactories.SLOW);
  }

  private void checkFlipRange(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 80; size += 10) {
      BitVector b = factory.apply(size);
      // setting (0, 0) shouldn't change anything.
      b.clear();
      b.flip(0, 0);
      assertEquals(0, b.cardinality());
      // flip all (0, len) second arg is exclusive.
      b.clear();
      b.flip(0, b.size());
      assertEquals(b.size(), b.cardinality());
      // and set it back to 0
      b.flip(0, b.size());
      assertEquals(0, b.cardinality());
      // set using all sizes up to len
      for (int i = 0; i < b.size(); i++) {
        for (int j = i; j < b.size(); j++) {
          b.clear();
          b.flip(i, j);
          assertEquals((j - i), b.cardinality());
          for (int k = i; k < j; k++) {
            assertTrue(b.get(k));
          }
          b.flip(i, j);
          assertEquals(0, b.cardinality());
          setAllBits(b);
          b.flip(i, j);
          assertEquals(b.size() - (j - i), b.cardinality());
          for (int k = i; k < j; k++) {
            assertFalse(b.get(k));
          }
          b.flip(i, j);
          assertEquals(b.size(), b.cardinality());
        }
      }
    }
  }

  public void testGet() {
    checkGet(BitVectorFactories.OPTIMAL);
    checkGet(BitVectorFactories.SLOW);
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
    assertEquals(i, b2.size());
    assertEquals(i, b2.cardinality());
  }

  public void testAnd() {
    checkAnd(BitVectorFactories.OPTIMAL);
    checkAnd(BitVectorFactories.SLOW);
  }

  private void checkAnd(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    BitVector b2 = b.clone();
    b.and(b2);
    assertTrue(b.isEmpty());
    for (int i = 1; i < 128; i++) {
      checkAnd(factory.apply(i));
    }
  }

  private void checkAnd(BitVector b) {
    BitVector b2 = b.clone();
    b2.clear();
    setAllBits(b);
    assertEquals(b.size(), b.cardinality());
    b.and(b2);
    assertEquals(0, b.cardinality());
    b.set(0);
    b2.set(0);
    b.and(b2);
    assertTrue(b.get(0));
    assertEquals(b, b2);
  }

  public void testAndNotForSizeZero() {
    checkAndNotForSizeZero(BitVectorFactories.OPTIMAL);
    checkAndNotForSizeZero(BitVectorFactories.SLOW);
  }

  private void checkAndNotForSizeZero(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    BitVector b2 = b.clone();
    b.andNot(b2);
    assertTrue(b.isEmpty());
  }

  public void testOrForSizeZero() {
    checkOrForSizeZero(BitVectorFactories.OPTIMAL);
    checkOrForSizeZero(BitVectorFactories.SLOW);
  }

  private void checkOrForSizeZero(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    BitVector b2 = b.clone();
    b.or(b2);
    assertTrue(b.isEmpty());
  }

  public void testXorForSizeZero() {
    checkXorForSizeZero(BitVectorFactories.OPTIMAL);
    checkXorForSizeZero(BitVectorFactories.SLOW);
  }

  private void checkXorForSizeZero(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    BitVector b2 = b.clone();
    b.xor(b2);
    assertTrue(b.isEmpty());
  }

  public void testGetNextClearBit() {
    checkGetNextClearBit(BitVectorFactories.OPTIMAL);
    checkGetNextClearBit(BitVectorFactories.SLOW);
  }

  private void checkGetNextClearBit(Function<Integer, BitVector> factory) {
    for (int size = 0; size < 128; size++) {
      BitVector b = factory.apply(size);
      for (int i = 0; i < b.size() - 1; i++) {
        b.set(i);
        assertEquals(i + 1, b.nextClearBit(0));
      }
    }
  }

  public void testGetNextSetBit() {
    checkGetNextSetBit(BitVectorFactories.OPTIMAL);
    checkGetNextSetBit(BitVectorFactories.SLOW);
  }

  private void checkGetNextSetBit(Function<Integer, BitVector> factory) {
    for (int i = 0; i < 128; i++) {
      BitVector b = factory.apply(i);
      setAllBits(b);
      for (int i1 = 0; i1 < b.size() - 1; i1++) {
        b.clear(i1);
        assertEquals(i1 + 1, b.nextSetBit(0));
      }
    }
  }

  public void testRotateAllZerosAndAllOnes() {
    checkRotateAllZeroesAndAllOnes(BitVectorFactories.OPTIMAL);
    checkRotateAllZeroesAndAllOnes(BitVectorFactories.SLOW);
  }

  private void checkRotateAllZeroesAndAllOnes(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 100; size += 10) {
      BitVector b = factory.apply(size);
      b.clear();
      BitVector b2 = b.clone();
      for (int i = -size * 4; i < size * 4; i++) {
        b.rotate(i);
        assertEquals(b2, b);
      }
      setAllBits(b);
      b2 = b.clone();
      for (int i = -size * 4; i < size * 4; i++) {
        b.rotate(i);
        assertEquals(b2, b);
      }
    }
  }

  public void testRotate() {
    checkRotate(BitVectorFactories.OPTIMAL);
    checkRotate(BitVectorFactories.SLOW);
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
            assertEquals(cardinality, b.cardinality());
          }
        }
      }
    }
  }

  private void checkGrayCode(BitVector b, int end) {
    for (int i = 0; i < end - 1; i++) {
      BitVector bo = b.clone();
      assertEquals(0, hammingDistance(b, bo));
      bo.grayCode();
      if (!b.increment()) {
        break;
      }
      b.grayCode();
      assertEquals(1, hammingDistance(b, bo));
    }
  }

  private static int hammingDistance(BitVector x, BitVector y) {
    BitVector cx = x.clone();
    cx.xor(y);
    return cx.cardinality();
  }

  public void testGrayCode() {
    checkGrayCode(BitVectorFactories.OPTIMAL);
    checkGrayCode(BitVectorFactories.SLOW);
  }

  private void checkGrayCode(Function<Integer, BitVector> factory) {
    BitVector bv = factory.apply(0);
    bv.grayCode();
    assertTrue(bv.isEmpty());
    bv = factory.apply(1);
    bv.set(0);
    BitVector expected = bv.clone();
    bv.grayCode();
    assertEquals(expected, bv);
    for (int i = 0; i <= 80; i += 10) {
      BitVector b = factory.apply(i);
      checkGrayCode(b, 1000);
      b.flip(0, b.size());
      checkGrayCode(b, 1000);
    }
  }

  private void checkGrayCodeInverse(BitVector b, int end) {
    for (int i = 0; i < end - 1; i++) {
      BitVector bo = b.clone();
      assertEquals(0, hammingDistance(b, bo));
      b.grayCode();
      b.grayCodeInverse();
      assertEquals(bo, b);
      if (b.increment() == false) {
        break;
      }
    }
  }

  public void testGrayCodeInverse() {
    checkGrayCodeInverse(BitVectorFactories.OPTIMAL);
    checkGrayCodeInverse(BitVectorFactories.SLOW);
  }

  private void checkGrayCodeInverse(Function<Integer, BitVector> factory) {
    BitVector bv = factory.apply(0);
    bv.grayCodeInverse();
    assertTrue(bv.isEmpty());
    bv = factory.apply(1);
    bv.set(0);
    BitVector expected = bv.clone();
    bv.grayCode();
    bv.grayCodeInverse();
    assertEquals(expected, bv);
    for (int i = 0; i <= 80; i += 10) {
      BitVector b = factory.apply(i);
      checkGrayCodeInverse(b, 1000);
      b.flip(0, b.size());
      checkGrayCodeInverse(b, 1000);
    }
  }

  public void testEqualsAndHashCode() {
    int size = 10;
    for (long i = 1 << size; --i >= 0; ) {
      checkEqualsAndHashCode(size, i);
    }
    for (long i = Long.MIN_VALUE; --i >= Long.MAX_VALUE - 1024; ) {
      checkEqualsAndHashCode(64, i);
    }
    for (long i = Long.MIN_VALUE; ++i <= Long.MIN_VALUE + 1024; ) {
      checkEqualsAndHashCode(64, i);
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
  
  public void testSmallerEvenAndGrayCode() {
    checkEntryVertex(BitVectorFactories.SLOW);
    checkEntryVertex(BitVectorFactories.OPTIMAL);
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
      assertEquals(expected, bs);
    }
  }

  public void testSmallerEvenAndGrayCodeForTwoBits() {
    checkEntryVertexForTwoDimensions(BitVectorFactories.SLOW);
    checkEntryVertexForTwoDimensions(BitVectorFactories.OPTIMAL);
  }
  
  /**
   * This is a test for Theorem 2.10 much more than for the entry function
   * implementation itself.
   */
  public void checkEntryVertexForTwoDimensions(Function<Integer, BitVector> factory) {
    BitVector bs = factory.apply(2);
    bs.smallerEvenAndGrayCode();
    assertEquals(TestUtils.createBitVector(0, 2), bs);
    bs.set(0);
    bs.smallerEvenAndGrayCode();
    assertEquals(TestUtils.createBitVector(0, 2), bs);
    bs.set(1);
    bs.smallerEvenAndGrayCode();
    assertEquals(TestUtils.createBitVector(0, 2), bs);
    bs.set(0, 2);
    bs.smallerEvenAndGrayCode();
    assertEquals(TestUtils.createBitVector(3, 2), bs);
  }
  
  public void testGrayCodeRankWithAllFreeBitsIsIdentityFunction() {
    checkGrayCodeRankWithAllFreeBitsIsIdentityFunction(BitVectorFactories.SLOW);
    checkGrayCodeRankWithAllFreeBitsIsIdentityFunction(BitVectorFactories.OPTIMAL);
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
      assertEquals(bs, r);
      bs.increment();
    }
  }

  public void testGrayCodeRankRemovesConstrainedBits() {
    checkGrayCodeRankRemovesConstrainedBits(BitVectorFactories.SLOW);
    checkGrayCodeRankRemovesConstrainedBits(BitVectorFactories.OPTIMAL);
  }
  
  public void checkGrayCodeRankRemovesConstrainedBits(
      Function<Integer, BitVector> factory) {
    BitVector mu = factory.apply(3);
    mu.set(1);
    BitVector bs = factory.apply(3);
    bs.set(0);
    BitVector r = factory.apply(1);
    r.grayCodeRank(mu, bs);
    assertTrue(r.isEmpty());
    bs.set(1);
    BitVector expected = TestUtils.createBitVector(1, 1);
    r.grayCodeRank(mu, bs);
    assertEquals(expected, r);
    bs.set(2);
    r.grayCodeRank(mu, bs);
    assertEquals(expected, r);
  }

  public void testGrayCodeRankIsPlainPatternRank() {
    checkGrayCodeRankIsPlainPatternRank(BitVectorFactories.SLOW);
    checkGrayCodeRankIsPlainPatternRank(BitVectorFactories.OPTIMAL);
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
        assertEquals(jAsBitVector, r);
      }
    }
  }
  
  public void testGrayCodeRankInverse() {
    checkGrayCodeRankInverse(BitVectorFactories.SLOW);
    checkGrayCodeRankInverse(BitVectorFactories.OPTIMAL);
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
          assertEquals(yetAnotherGrayCodeRankInverse(factory, n, mu, known, grayCodeRank), actual);
          actual.grayCode();
          assertEquals(iAsBitVector, actual);
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
  
  public void testFirstDifferentLowestBitCount() {
    checkIntraSubHypercubeDirection(BitVectorFactories.SLOW);
    checkIntraSubHypercubeDirection(BitVectorFactories.OPTIMAL);
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
        assertEquals(expected, bv.lowestDifferentBit());
      }
    }
  }

  public void testAreAllLowestBitsClear() {
    checkAreAllLowestBitsClear(BitVectorFactories.SLOW);
    checkAreAllLowestBitsClear(BitVectorFactories.OPTIMAL);
  }
  
  private void checkAreAllLowestBitsClear(Function<Integer, BitVector> factory) {
    for (int size = 0; size <= 100; size += 10) {
      BitVector v = factory.apply(size);
      for (int i = 0; i <= size; ++i) {
        assertTrue(v.areAllLowestBitsClear(i));
      }
      for (int i = size; --i >= 0; ) {
        v.set(i);
        for (int j = 0; j <= size; ++j) {
          assertEquals(j <= i, v.areAllLowestBitsClear(j));
        }
      }
    }
  }

  public void testLowestDifferentBitForTwoDimensions() {
    checkIntraSubHypercubeDirectionForTwoDimensions(BitVectorFactories.SLOW);
    checkIntraSubHypercubeDirectionForTwoDimensions(BitVectorFactories.OPTIMAL);
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
    assertEquals(0, bv.lowestDifferentBit());
    bv.copyFrom(1);
    assertEquals(1, bv.lowestDifferentBit());
    bv.copyFrom(2);
    assertEquals(1, bv.lowestDifferentBit());
    bv.copyFrom(3);
    assertEquals(0, bv.lowestDifferentBit());
  }
  
  public void testCopySectionFrom() {
    checkCopySectionFrom(BitVectorFactories.SLOW);
    checkCopySectionFrom(BitVectorFactories.OPTIMAL);
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
            assertEquals(num << offset, bv.toExactLong());
          }
          bv.copyFrom((1 << bitCount) - 1);
          for (int num = 0; num < 1 << len; ++num) {
            src.copyFrom(num);
            bv.copySectionFrom(offset, src);
            long actual = bv.toExactLong();
            assertEquals(num, actual >>> offset & ((1 << len) - 1));
            assertEquals((1 << offset) - 1, actual & ((1 << offset) - 1));
            assertEquals((1 << bitCount - offset - len) - 1, actual >> offset + len);
          }
        }
      }
    }
  }

  public void testToLongArrayForSingleWord() {
    checkToLongArrayForSingleWord(BitVectorFactories.SLOW);
    checkToLongArrayForSingleWord(BitVectorFactories.OPTIMAL);
  }

  public void testToLongArrayForTwoWords() {
    checkToLongArrayForTwoWords(BitVectorFactories.SLOW);
    checkToLongArrayForTwoWords(BitVectorFactories.OPTIMAL);
  }
  
  private void checkToLongArrayForTwoWords(Function<Integer, BitVector> factory) {
    checkEvenBitsToLongArray(factory);
    checkOddBitsToLongArray(factory);
  }

  private void checkEvenBitsToLongArray(Function<Integer, BitVector> factory) {
    BitVector v = factory.apply(128);
    for (int i = 0; i < 64; ++i) {
      v.set(2 * i);
    }
    long[] longArray = v.toLongArray();
    MoreAsserts.assertEquals(new long[] {0x5555555555555555L, 0x5555555555555555L}, longArray);
  }

  private void checkOddBitsToLongArray(Function<Integer, BitVector> factory) {
    BitVector v = factory.apply(128);
    for (int i = 0; i < 64; ++i) {
      v.set(2 * i + 1);
    }
    long[] longArray = v.toLongArray();
    MoreAsserts.assertEquals(new long[] {0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL}, longArray);
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

  private void checkToLongArrayForSingleWord(BitVector v, long i) {
    v.copyFrom(i);
    long[] array = v.toLongArray();
    if (v.size() == 0) {
      assert i == 0;
      assertEquals(0, array.length);
    } else {
      assertEquals(1, array.length);
      assertEquals(i, array[0]);
    }
  }

  public void testCopyFromLongArray() {
    checkCopyFromLongArray(BitVectorFactories.SLOW);
    checkCopyFromLongArray(BitVectorFactories.OPTIMAL);
  }
  
  private void checkCopyFromLongArray(Function<Integer, BitVector> factory) {
    BitVector v = factory.apply(128);
    long[] longArray = new long[2];
    for (long i = Long.MIN_VALUE; --i >= Long.MAX_VALUE - 80; ) {
      longArray[0] = i;
      for (long j = Long.MIN_VALUE; ++j <= Long.MIN_VALUE + 80; ) {
        longArray[1] = j;
        v.copyFrom(longArray);
        MoreAsserts.assertEquals(longArray, v.toLongArray());
      }
    }
  }
  
  public static void testIncrementReturnValue() {
    checkIncrementReturnValue(BitVectorFactories.OPTIMAL);
    checkIncrementReturnValue(BitVectorFactories.SLOW);
  }

  private static void checkIncrementReturnValue(Function<Integer, BitVector> factory) {
    BitVector b = factory.apply(0);
    assertFalse(b.increment());
    b = factory.apply(4);
    b.copyFrom(0xD);
    assertTrue(b.increment());
    assertTrue(b.increment());
    assertFalse(b.increment());
    assertFalse(b.increment());
    b = factory.apply(64);
    b.copyFrom(0xFFFFFFFFFFFFFFFEL);
    assertTrue(b.increment());
    assertFalse(b.increment());
    assertFalse(b.increment());
  }
}
