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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Aioanei
 */
public class BitVectorMathTest {
  
  @Test
  public void split() {
    final int n = 5;
    BitVector[] actual = new BitVector[3];
    for (int i = 0; i < n; ++i) {
      actual[0] = BitVectorFactories.OPTIMAL.apply(i);
      for (int j = 0; j < n; ++j) {
        actual[1] = BitVectorFactories.OPTIMAL.apply(j);
        for (int k = 0; k < n; ++k) {
          actual[2] = BitVectorFactories.OPTIMAL.apply(k);
          for (int x = 1; x < 1 << (i + j + k); ++x) {
            BitVector xAsBitVector = TestUtils.createBitVector(x, i + j + k);
            BitVectorMath.split(xAsBitVector, actual);
            Assert.assertEquals(TestUtils.createBitVector(x >> (j + k), i), actual[0]);
            Assert.assertEquals(TestUtils.createBitVector(
                (x & ((1 << (j + k)) - (1 << k))) >>> k, j), actual[1]);
            Assert.assertEquals(TestUtils.createBitVector(x & ((1 << k) - 1), k), actual[2]);
          }
        }
      }
    }
  }
}
