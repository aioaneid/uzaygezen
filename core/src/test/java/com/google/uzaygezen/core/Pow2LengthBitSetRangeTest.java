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

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.util.List;

/**
 * @author Daniel Aioanei
 */
public class Pow2LengthBitSetRangeTest extends TestCase {

  private static final Pow2LengthBitSetRange ZERO_ONE =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 2), 0);
  private static final Pow2LengthBitSetRange ONE_TWO =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(1, 2), 0);
  private static final Pow2LengthBitSetRange TWO_FOUR =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 2), 1);
  
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ONE_TWO_TWO_FOUR =
      ImmutableList.of(ZERO_ONE, ONE_TWO, TWO_FOUR);

  public void testContent() {
    assertEquals(2, Pow2LengthBitSetRange.content(ZERO_ONE_ONE_TWO_TWO_FOUR));
  }

  public void testToBigIntOrthotope() {
    assertEquals(ImmutableList.of(TestUtils.TWO_FOUR, TestUtils.FOUR_EIGHT),
        Pow2LengthBitSetRange.toLongOrthotope(ImmutableList.of(
            new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 3), 1),
            new Pow2LengthBitSetRange(TestUtils.createBitVector(4, 3), 2))));
  }
  
  public void testEncloses() {
    final int n = 20;
    for (int i = 0; i < n; ++i) {
      for (int iLevel = 0; iLevel < n; ++iLevel) {
        BitVector iShifted = TestUtils.createBitVector(i << iLevel, n + 32);
        Pow2LengthBitSetRange iRange = new Pow2LengthBitSetRange(iShifted.clone(), iLevel);
        for (int j = 0; j < n; ++j) {
          for (int jLevel = 0; jLevel < n; ++jLevel) {
            BitVector jShifted = TestUtils.createBitVector(j << jLevel, n + 32);
            Pow2LengthBitSetRange jRange = new Pow2LengthBitSetRange(jShifted.clone(), jLevel);
            boolean expected = iRange.toLongRange().getStart() <=
                jRange.toLongRange().getStart()
                && iRange.toLongRange().getEnd() >= jRange.toLongRange().getEnd();
            assertEquals(expected, iRange.encloses(jRange));
            // Check that the bit sets haven't changed.
            assertEquals(iShifted, iRange.getStart());
            assertEquals(jShifted, jRange.getStart());
          }
        }
      }
    }
  }
}
