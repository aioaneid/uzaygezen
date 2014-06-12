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

import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.ranges.LongRange;
import com.google.uzaygezen.core.ranges.LongRangeHome;

/**
 * @author Daniel Aioanei
 */
public class RangeListFilterTest {

  @Test
  public void combineThresholdNotExceeded() {
    RangeListFilter<Long, LongContent, LongRange> x = new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> y = new RangeListFilter<>(
      ImmutableList.of(TestUtils.THREE_FOUR), false, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> z = x.combine(y, 2, new LongContent(10));
    Assert.assertEquals(new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_ONE, TestUtils.THREE_FOUR), false, Level.FINE,
      LongRangeHome.INSTANCE), z);
  }

  @Test
  public void combineThresholdStaysExceededIfFirstExceeded() {
    RangeListFilter<Long, LongContent, LongRange> x = new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_ONE), true, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> y = new RangeListFilter<>(
      ImmutableList.of(TestUtils.THREE_FOUR), false, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> z = x.combine(y, 2, new LongContent(10));
    Assert.assertEquals(new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_FOUR), true, Level.FINE, LongRangeHome.INSTANCE), z);
  }

  @Test
  public void combineThresholdStaysExceededIfSecondExceeded() {
    RangeListFilter<Long, LongContent, LongRange> x = new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> y = new RangeListFilter<>(
      ImmutableList.of(TestUtils.THREE_FOUR), true, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> z = x.combine(y, 2, new LongContent(10));
    Assert.assertEquals(new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_FOUR), true, Level.FINE, LongRangeHome.INSTANCE), z);
  }

  @Test
  public void combineThresholdExceeded() {
    RangeListFilter<Long, LongContent, LongRange> x = new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> y = new RangeListFilter<>(
      ImmutableList.of(TestUtils.THREE_FOUR), false, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> z = x.combine(y, 1, new LongContent(10));
    Assert.assertEquals(new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_FOUR), true, Level.FINE, LongRangeHome.INSTANCE), z);
  }

  @Test
  public void combineCollapsesVacuum() {
    RangeListFilter<Long, LongContent, LongRange> x = new RangeListFilter<>(
      ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE, LongRangeHome.INSTANCE);
    RangeListFilter<Long, LongContent, LongRange> y = new RangeListFilter<>(
      ImmutableList.of(TestUtils.THREE_FOUR), false, Level.FINE, LongRangeHome.INSTANCE);
    for (int i = 1; i < 3; ++i) {
      RangeListFilter<Long, LongContent, LongRange> z = x.combine(y, i, new LongContent(0));
      Assert.assertEquals(new RangeListFilter<>(
        ImmutableList.of(TestUtils.ZERO_FOUR), false, Level.FINE, LongRangeHome.INSTANCE), z);
    }
  }
}
