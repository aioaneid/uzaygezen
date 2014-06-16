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

package com.google.uzaygezen.core.ranges;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.MoreAsserts;
import com.google.uzaygezen.core.TestUtils;

/**
 * @author Daniel Aioanei
 */
public class LongRangeTest {

  @Test
  public void overlapForKnownValues() {
    Assert.assertEquals(1, TestUtils.ZERO_ONE.overlap(TestUtils.ZERO_ONE));
    Assert.assertEquals(1, TestUtils.ZERO_ONE.overlap(TestUtils.ZERO_TEN));
    Assert.assertEquals(1, TestUtils.ZERO_TEN.overlap(TestUtils.ZERO_ONE));
    Assert.assertEquals(0, TestUtils.ZERO_ONE.overlap(TestUtils.ONE_TEN));
    Assert.assertEquals(0, TestUtils.ONE_TEN.overlap(TestUtils.ZERO_ONE));
    Assert.assertEquals(9, TestUtils.ZERO_TEN.overlap(TestUtils.ONE_TEN));
    Assert.assertEquals(9, TestUtils.ONE_TEN.overlap(TestUtils.ZERO_TEN));
  }
  
  @Test
  public void overlapForOneDimension() {
    int n = 10;
    for (int i = 0; i < n; ++i) {
      for (int j = i + 1; j < n; ++j) {
        final LongRange x = LongRange.of(i, j);
        for (int k = 0; k < n; ++k) {
          for (int l = k + 1; l < n; ++l) {
            final LongRange y = LongRange.of(k, l);
            long actual0 = x.overlap(y);
            long actual1 = LongRange.overlap(ImmutableList.of(x), ImmutableList.of(y));
            long expected = overlap(i, j, k, l);
            Assert.assertEquals(expected, actual0);
            Assert.assertEquals(expected, actual1);
          }
        }
      }
    }
  }
  
  @Test
  public void multiDimensionalOverlap() {
    LongRange x0 = LongRange.of(100, 105);
    LongRange x1 = LongRange.of(103, 200);
    LongRange y0 = LongRange.of(1, 10);
    LongRange y1 = LongRange.of(0, 5);
    long actual = LongRange.overlap(ImmutableList.of(x0, y0), ImmutableList.of(x1, y1));
    Assert.assertEquals(8, actual);
  }
  
  /**
   * Very simple and inefficient overlap algorithm.
   * 
   * @param i inclusive start of the first range
   * @param j exclusive end of the first range
   * @param k inclusive start of the second range
   * @param l exclusive end of the second range
   * @return the overlap of [i, j) with [k, l)
   */
  private int overlap(int i, int j, int k, int l) {
    int overlap = 0;
    for (int m = i; m < j; ++m) {
      if (k <= m & m < l) {
        overlap++;
      }
    }
    return overlap;
  }

  @Test
  public void equalsAndHashCode() {
    MoreAsserts.checkEqualsAndHashCodeMethods(TestUtils.ONE_TEN, TestUtils.ONE_TEN, true);
    MoreAsserts.checkEqualsAndHashCodeMethods(TestUtils.ONE_TEN, new LongRange(1, 10), true);
    MoreAsserts.checkEqualsAndHashCodeMethods(TestUtils.ONE_TEN, TestUtils.ZERO_TEN, false);
  }

  @Test
  public void getters() {
    Assert.assertEquals(Long.valueOf(1), TestUtils.ONE_TEN.getStart());
    Assert.assertEquals(Long.valueOf(10), TestUtils.ONE_TEN.getEnd());
  }

  @Test
  public void toStringImplementation() {
    Assert.assertTrue(TestUtils.ONE_TEN.toString().contains("1"));
    Assert.assertTrue(TestUtils.ONE_TEN.toString().contains("10"));
  }
  
  @Test
  public void contains() {
    Assert.assertFalse(TestUtils.ONE_TEN.contains(0L));
    Assert.assertTrue(TestUtils.ONE_TEN.contains(1L));
    Assert.assertTrue(TestUtils.ONE_TEN.contains(5L));
    Assert.assertFalse(TestUtils.ONE_TEN.contains(10L));
    Assert.assertFalse(TestUtils.ONE_TEN.contains(11L));
  }
}
