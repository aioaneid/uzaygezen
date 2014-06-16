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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.ranges.LongRangeHome;

/**
 * @author Daniel Aioanei
 */
public class Pow2LengthBitSetRangeTest {

  private static final Pow2LengthBitSetRange ZERO_ONE =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 2), 0);
  private static final Pow2LengthBitSetRange ONE_TWO =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(1, 2), 0);
  private static final Pow2LengthBitSetRange TWO_FOUR =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 2), 1);
  
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ONE_TWO_TWO_FOUR =
      ImmutableList.of(ZERO_ONE, ONE_TWO, TWO_FOUR);

  @Test
  public void levelSum() {
    Assert.assertEquals(1, Pow2LengthBitSetRange.levelSum(ZERO_ONE_ONE_TWO_TWO_FOUR));
  }
  
  @Test
  public void encloses() {
    final int n = 20;
    for (int i = 0; i < n; ++i) {
      for (int iLevel = 0; iLevel < n; ++iLevel) {
        BitVector iShifted = TestUtils.createBitVector(i << iLevel, n + 32);
        Pow2LengthBitSetRange iRange = new Pow2LengthBitSetRange(iShifted.clone(), iLevel);
        for (int j = 0; j < n; ++j) {
          for (int jLevel = 0; jLevel < n; ++jLevel) {
            BitVector jShifted = TestUtils.createBitVector(j << jLevel, n + 32);
            Pow2LengthBitSetRange jRange = new Pow2LengthBitSetRange(jShifted.clone(), jLevel);
            boolean expected = LongRangeHome.INSTANCE.toRange(iRange).getStart() <=
              LongRangeHome.INSTANCE.toRange(jRange).getStart()
                && LongRangeHome.INSTANCE.toRange(iRange).getEnd() >= LongRangeHome.INSTANCE.toRange(jRange).getEnd();
            Assert.assertEquals(expected, iRange.encloses(jRange));
            // Check that the bit sets haven't changed.
            Assert.assertEquals(iShifted, iRange.getStart());
            Assert.assertEquals(jShifted, jRange.getStart());
          }
        }
      }
    }
  }
}
