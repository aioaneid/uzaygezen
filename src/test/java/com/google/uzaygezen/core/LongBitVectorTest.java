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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Aioanei
 */
public class LongBitVectorTest {

  @Test
  public void compareTo() {
    int n = 10;
    LongBitVector x = new LongBitVector(64);
    LongBitVector negativeX = new LongBitVector(64);
    LongBitVector y = new LongBitVector(64);
    LongBitVector negativeY = new LongBitVector(64);
    // -=17 just to make it faster. Ideally we should use just plain --.
    for (long i = 1 << n; (i -= 17) >= 0; ) {
      x.copyFrom(i);
      negativeX.copyFrom(-i);
      for (long j = 1 << n; (j -= 17) >= 0; ) {
        y.copyFrom(j);
        negativeY.copyFrom(-j);
        Assert.assertEquals(Long.compare(i, j), Integer.signum(x.compareTo(y)));
        Assert.assertEquals(j == 0 ? Long.signum(i)
            : (i == 0 ? -1 : Long.compare(-i, -j)),
            Integer.signum(negativeX.compareTo(negativeY)));
        Assert.assertEquals(j == 0 ? Long.signum(i) : -1, Integer.signum(x.compareTo(negativeY)));
        Assert.assertEquals(j == 0 ? -Long.signum(i) : +1, Integer.signum(negativeY.compareTo(x)));
      }
    }
  }
  
  @Test
  public void toBigInteger() {
    int n = 10;
    LongBitVector x = new LongBitVector(64);
    LongBitVector negativeX = new LongBitVector(64);
    for (long i = 1 << n; --i >= 0; ) {
      x.copyFrom(i);
      Assert.assertEquals(BigInteger.valueOf(i), x.toBigInteger());
      long j = -i;
      negativeX.copyFrom(j);
      BigInteger expected = BigInteger.ZERO;
      for (int k = 0; k < 64; ++k) {
        if (((j >> k) & 1) == 1) {
          expected = expected.setBit(k);
        }
      }
      Assert.assertEquals(expected, negativeX.toBigInteger());
    }
  }
  
  @Test
  public void compareToCornerCases() {
    LongBitVector x = new LongBitVector(64);
    LongBitVector y = new LongBitVector(64);
    y.copyFrom(Long.MAX_VALUE);
    Assert.assertTrue(x.compareTo(y) < 0);
    x.copyFrom(Long.MIN_VALUE);
    Assert.assertTrue(y.compareTo(x) < 0);
    y.copyFrom(-1);
    Assert.assertTrue(x.compareTo(y) < 0);
  }
  
  @Test
  public void copyFromBitSet() {
    int n = 10;
    LongBitVector x = new LongBitVector(n);
    BitSet counter = new BitSet(n);
    for (long i = 0; i < 1 << n; ++i) {
      x.copyFrom(counter);
      Assert.assertEquals(i, x.toLong());
      Assert.assertEquals(64 - Long.numberOfLeadingZeros(i), x.length());
      BitSetMath.increment(counter);
    }
  }
  
  @Test
  public void optimisationForGrayCodeDoesNotAffectResults() {
    for (int bitCount = 5; --bitCount >= 0; ) {
      for (int i = 1 << bitCount; --i >= 0; ) {
        LongBitVector mu = new LongBitVector(bitCount);
        mu.copyFrom(i);
        LongBitVector r = new LongBitVector(mu.cardinality());
        for (int j = 1 << bitCount; --j >= 0; ) {
          LongBitVector w = new LongBitVector(bitCount);
          w.copyFrom(j);
          r.grayCodeRank(mu, w, true);
          LongBitVector rCopy = r.clone();
          r.grayCodeRank(mu, w, false);
          Assert.assertEquals(rCopy, r);
        }
      }
    }
  }
}
