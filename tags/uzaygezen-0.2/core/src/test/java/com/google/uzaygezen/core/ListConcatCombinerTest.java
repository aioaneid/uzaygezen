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
public class ListConcatCombinerTest {

  @Test
  public void twoAdjacentRangesAreCombinedInOneRange() {
    ListConcatCombiner<Long, LongContent, LongRange> c = ListConcatCombiner.unbounded();
    FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> range1 = FilteredIndexRange.of(
      TestUtils.ZERO_ONE,
      new RangeListFilter<Long, LongContent, LongRange>(
        ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE, LongRangeHome.INSTANCE), false);
    FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> range2 = FilteredIndexRange.of(
      TestUtils.ONE_TEN,
      new RangeListFilter<Long, LongContent, LongRange>(
        ImmutableList.of(TestUtils.ONE_TEN), false, Level.FINE, LongRangeHome.INSTANCE), false);
    SelectiveFilter<RangeListFilter<Long, LongContent, LongRange>> filter = c.combine(
      range1, range2, TestUtils.ZERO_LONG_CONTENT);
    Assert.assertEquals(
      SelectiveFilter.of(
        new RangeListFilter<Long, LongContent, LongRange>(
          ImmutableList.of(TestUtils.ZERO_TEN), false, Level.FINE, LongRangeHome.INSTANCE), false),
      filter);
  }

  @Test
  public void twoBorderRangesCombinedInOneRangeWhenGapIsZeroEvenIfNotAdjacent() {
    ListConcatCombiner<Long, LongContent, LongRange> c = ListConcatCombiner.unbounded();
    FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> range1 = FilteredIndexRange.of(
      TestUtils.ZERO_ONE,
      new RangeListFilter<Long, LongContent, LongRange>(
        ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE, LongRangeHome.INSTANCE), false);
    FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> range2 = FilteredIndexRange.of(
      TestUtils.TWO_TEN,
      new RangeListFilter<Long, LongContent, LongRange>(
        ImmutableList.of(TestUtils.TWO_TEN), false, Level.FINE, LongRangeHome.INSTANCE), false);
    SelectiveFilter<RangeListFilter<Long, LongContent, LongRange>> filter = c.combine(
      range1, range2, TestUtils.ZERO_LONG_CONTENT);
    Assert.assertEquals(
      SelectiveFilter.of(
        new RangeListFilter<Long, LongContent, LongRange>(
          ImmutableList.of(TestUtils.ZERO_TEN), false, Level.FINE, LongRangeHome.INSTANCE), false),
      filter);
  }

  @Test
  public void twoBorderRangesCombinedInOneRangeWhenGapIsZero() {
    ListConcatCombiner<Long, LongContent, LongRange> c = ListConcatCombiner.unbounded();
    FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> range1 = FilteredIndexRange.of(
      TestUtils.ZERO_FOUR,
      new RangeListFilter<Long, LongContent, LongRange>(ImmutableList.of(
        TestUtils.ZERO_TWO, TestUtils.THREE_FOUR), false, Level.FINE, LongRangeHome.INSTANCE),
      false);
    FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> range2 = FilteredIndexRange.of(
      TestUtils.SIX_TEN,
      new RangeListFilter<Long, LongContent, LongRange>(ImmutableList.of(
        TestUtils.SIX_SEVEN, TestUtils.EIGHT_TEN), false, Level.FINE, LongRangeHome.INSTANCE),
      false);
    Object filter = c.combine(range1, range2, TestUtils.ZERO_LONG_CONTENT);
    Assert.assertEquals(SelectiveFilter.of(
      new RangeListFilter<Long, LongContent, LongRange>(
        ImmutableList.of(TestUtils.ZERO_TWO, TestUtils.THREE_SEVEN, TestUtils.EIGHT_TEN), false,
        Level.FINE, LongRangeHome.INSTANCE), false), filter);
  }

  @Test
  public void concatenatesListsWhenGapIsPositive() {
    ListConcatCombiner<Long, LongContent, LongRange> c = ListConcatCombiner.unbounded();
    FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> range1 = FilteredIndexRange.of(
      TestUtils.ZERO_ONE,
      new RangeListFilter<Long, LongContent, LongRange>(
        ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE, LongRangeHome.INSTANCE), false);
    FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> range2 = FilteredIndexRange.of(
      TestUtils.SIX_TEN,
      new RangeListFilter<Long, LongContent, LongRange>(
        ImmutableList.of(TestUtils.SIX_TEN), false, Level.FINE, LongRangeHome.INSTANCE), false);
    SelectiveFilter<RangeListFilter<Long, LongContent, LongRange>> filter = c.combine(
      range1, range2, TestUtils.ONE_LONG_CONTENT);
    Assert.assertEquals(
      SelectiveFilter.of(
        new RangeListFilter<Long, LongContent, LongRange>(ImmutableList.of(
          TestUtils.ZERO_ONE, TestUtils.SIX_TEN), false, Level.FINE, LongRangeHome.INSTANCE), false),
      filter);
  }
}
