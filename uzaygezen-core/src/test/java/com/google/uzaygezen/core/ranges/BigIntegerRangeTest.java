/*
 * Copyright (C) 2012 Daniel Aioanei.
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
package com.google.uzaygezen.core.ranges;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.MoreAsserts;

/**
 * @author Daniel Aioanei
 */
public class BigIntegerRangeTest {

  private static final BigIntegerRange ZERO_ONE = BigIntegerRange.of(0, 1);
  private static final BigIntegerRange ZERO_TEN = BigIntegerRange.of(0, 10);
  private static final BigIntegerRange ONE_TEN = BigIntegerRange.of(1, 10);

  @Test
  public void overlapForKnownValues() {
    Assert.assertEquals(BigInteger.ONE, ZERO_ONE.overlap(ZERO_ONE));
    Assert.assertEquals(BigInteger.ONE, ZERO_ONE.overlap(ZERO_TEN));
    Assert.assertEquals(BigInteger.ONE, ZERO_TEN.overlap(ZERO_ONE));
    Assert.assertEquals(BigInteger.ZERO, ZERO_ONE.overlap(ONE_TEN));
    Assert.assertEquals(BigInteger.ZERO, ONE_TEN.overlap(ZERO_ONE));
    Assert.assertEquals(BigInteger.valueOf(9), ZERO_TEN.overlap(ONE_TEN));
    Assert.assertEquals(BigInteger.valueOf(9), ONE_TEN.overlap(ZERO_TEN));
  }
  
  @Test
  public void multiDimensionalOverlap() {
    BigIntegerRange x0 = BigIntegerRange.of(100, 105);
    BigIntegerRange x1 = BigIntegerRange.of(103, 200);
    BigIntegerRange y0 = BigIntegerRange.of(1, 10);
    BigIntegerRange y1 = BigIntegerRange.of(0, 5);
    BigInteger actual = BigIntegerRange.overlap(ImmutableList.of(x0, y0), ImmutableList.of(x1, y1));
    Assert.assertEquals(BigInteger.valueOf(8), actual);
  }

  @Test
  public void equalsAndHashCode() {
    MoreAsserts.checkEqualsAndHashCodeMethods(ONE_TEN, ONE_TEN, true);
    MoreAsserts.checkEqualsAndHashCodeMethods(ONE_TEN, new BigIntegerRange(BigInteger.ONE, BigInteger.valueOf(10)), true);
    MoreAsserts.checkEqualsAndHashCodeMethods(ONE_TEN, ZERO_TEN, false);
  }

  @Test
  public void getters() {
    Assert.assertEquals(BigInteger.ONE, ONE_TEN.getStart());
    Assert.assertEquals(BigInteger.valueOf(10), ONE_TEN.getEnd());
  }

  @Test
  public void toStringImplementation() {
    Assert.assertTrue(ONE_TEN.toString().contains("1"));
    Assert.assertTrue(ONE_TEN.toString().contains("10"));
  }
  
  @Test
  public void contains() {
    Assert.assertFalse(ONE_TEN.contains(BigInteger.ZERO));
    Assert.assertTrue(ONE_TEN.contains(BigInteger.ONE));
    Assert.assertTrue(ONE_TEN.contains(BigInteger.valueOf(5)));
    Assert.assertFalse(ONE_TEN.contains(BigInteger.valueOf(10)));
    Assert.assertFalse(ONE_TEN.contains(BigInteger.valueOf(11)));
  }
}
