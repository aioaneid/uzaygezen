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



import junit.framework.TestCase;

import java.util.BitSet;

/**
 * @author Daniel Aioanei
 */
public class BitSetMathTest extends TestCase {
  
  public void testIncrement() {
    BitSet actual = new BitSet();
    for (int i = 0; i < 64; ++i) {
      BitSet expected = TestUtils.unsignedIntToLittleEndianBitSet(i);
      assertEquals(expected, actual);
      BitSetMath.increment(actual);
    }
  }

  public void testRotateEmptyBitSet() {
    BitSet bs = new BitSet();
    BitSetMath.rotate(bs, 10, 1);
    assertTrue(bs.isEmpty());
    BitSetMath.rotate(bs, 1, 10);
    assertTrue(bs.isEmpty());
    BitSetMath.rotate(bs, 1, -10);
    assertTrue(bs.isEmpty());
    BitSetMath.rotate(bs, 10, -1);
    assertTrue(bs.isEmpty());
    BitSetMath.rotate(bs, 0, 10);
    assertTrue(bs.isEmpty());
    BitSetMath.rotate(bs, 0, -10);
    assertTrue(bs.isEmpty());
    BitSetMath.rotate(bs, 0, 0);
    assertTrue(bs.isEmpty());
  }

  public void testRotateCardinalityOne() {
    BitSet bs = new BitSet();
    bs.set(9);
    BitSet copy = (BitSet) bs.clone();
    BitSetMath.rotate(bs, 8, 0);
    assertEquals(copy, bs);
    BitSetMath.rotate(bs, 8, 1000);
    assertEquals(copy, bs);
    BitSetMath.rotate(bs, 8, -1000);
    assertEquals(copy, bs);
    BitSetMath.rotate(bs, 10, 1);
    BitSet expected = new BitSet();
    expected.set(8);
    assertEquals(expected, bs);
    BitSetMath.rotate(bs, 10, -1);
    assertEquals(copy, bs);
    BitSetMath.rotate(bs, 10, -1);
    expected.clear();
    expected.set(0);
    assertEquals(expected, bs);
    BitSetMath.rotate(bs, 10, 1);
    assertEquals(copy, bs);
  }

  public void testRotateCardinalityTwo() {
    BitSet bs = new BitSet();
    bs.set(5);
    bs.set(2);
    BitSet copy = (BitSet) bs.clone();
    BitSetMath.rotate(bs, 1000, 0);
    assertEquals(copy, bs);
    BitSetMath.rotate(bs, 5, -5);
    assertEquals(copy, bs);
    BitSet expected = new BitSet();
    expected.set(1);
    expected.set(5);
    BitSetMath.rotate(bs, 5, 1);
    assertEquals(expected, bs);
    BitSetMath.rotate(bs, 5, -1);
    assertEquals(copy, bs);
    BitSetMath.rotate(bs, 6, -1);
    expected.clear();
    expected.set(0);
    expected.set(3);
    assertEquals(expected, bs);
  }

  public void testExtractBitRange() {
    BitSet iAsBitSet = new BitSet();
    BitSet r = new BitSet();
    for (int i = 0; i < 64; ++i) {
      for (int from = 0; from < 32; ++from) {
        for (int to = from; to < 32; ++to) {
          BitSet expected = iAsBitSet.get(from, to);
          BitSetMath.extractBitRange(iAsBitSet, from, to, r);
          assertEquals(expected, r);
        }
      }
      BitSetMath.increment(iAsBitSet);
    }
  }

  public void testGrayCode() {
    BitSet bs = new BitSet();
    BitSetMath.grayCode(bs);
    assertTrue(bs.isEmpty());
    bs.set(0);
    BitSet expected = (BitSet) bs.clone();
    BitSetMath.grayCode(bs);
    assertEquals(expected, bs);
    bs.set(1);
    expected.clear();
    expected.set(1);
    BitSetMath.grayCode(bs);
    assertEquals(expected, bs);
    expected.clear();
    expected.set(0);
    expected.set(1);
    BitSetMath.grayCode(bs);
    assertEquals(expected, bs);
  }

  public void testGrayCodeInverse() {
    BitSet bs = new BitSet();
    BitSetMath.grayCodeInverse(bs);
    assertTrue(bs.isEmpty());
    bs.set(1);
    BitSet expected = new BitSet();
    expected.set(0, 2);
    BitSetMath.grayCodeInverse(bs);
    assertEquals(expected, bs);
  }
  
  public void testGrayCodeInverseOfGrayCodeIsIdentityFunction() {
    BitSet bitSetCounter = new BitSet();
    for (int i = 0; i < 100; i++) {
      BitSet copy = (BitSet) bitSetCounter.clone();
      BitSet bs = (BitSet) bitSetCounter.clone();
      BitSetMath.grayCode(bs);
      BitSetMath.grayCodeInverse(bs);
      assertEquals(copy, bs);
      BitSetMath.grayCodeInverse(bs);
      BitSetMath.grayCode(bs);
      assertEquals(copy, bs);
      BitSetMath.increment(bitSetCounter);
    }
  }
  
  /**
   * This is a test for Lemma 2.3 from the Compact Hilbert Indices technical
   * report. However it also acts a a test for {@link BitSetMath#grayCode} to a
   * smaller extent.
   */
  public void testDimensionOfChangeInGrayCodeIsTsb() {
    BitSet i = new BitSet();
    BitSet currentGc = new BitSet();
    BitSet tmp = new BitSet();
    for (int j = 0; j < 16; ++j) {
      int tsb = BitSetMath.increment(i);
      tmp.clear();
      tmp.or(i);
      BitSetMath.grayCode(tmp);
      currentGc.xor(tmp);
      currentGc.flip(tsb);
      assertTrue(currentGc.isEmpty());
      currentGc.clear();
      currentGc.or(tmp);
    }
  }

  public void testLittleEndianBitSetToNonNegativeLong() {
    for (int i = 0; i < 1024; ++i) {
      BitSet bs = TestUtils.unsignedIntToLittleEndianBitSet(i);
      long bigInt = BitSetMath.littleEndianBitSetToNonNegativeLong(bs);
      assertEquals(i, bigInt);
    }
  }

  public void testNonNegativeLongToLittleEndianBitSet() {
    for (int i = 0; i < 1024; ++i) {
      BitSet expected = TestUtils.unsignedIntToLittleEndianBitSet(i);
      BitSet actual = new BitSet();
      BitSetMath.nonNegativeLongToLittleEndianBitSet(i, actual);
      assertEquals(expected, actual);
    }
  }
}
