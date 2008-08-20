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

import java.util.logging.Level;

/**
 * @author Daniel Aioanei
 */
public class ListConcatCombinerTest extends TestCase {

  public void testTwoAdjacentRangesAreCombinedInOneRange() {
    FilterCombiner<RangeListFilter> c = ListConcatCombiner.UNBOUNDED_INSTANCE;
    FilteredIndexRange<RangeListFilter> range1 = FilteredIndexRange.of(TestUtils.ZERO_ONE,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE), false);
    FilteredIndexRange<RangeListFilter> range2 = FilteredIndexRange.of(TestUtils.ONE_TEN,
        new RangeListFilter(ImmutableList.of(TestUtils.ONE_TEN), false, Level.FINE), false);
    SelectiveFilter<RangeListFilter> filter = c.combine(range1, range2, 0);
    assertEquals(SelectiveFilter.of(new RangeListFilter(
        ImmutableList.of(TestUtils.ZERO_TEN), false, Level.FINE), false), filter);
  }

  public void testTwoBorderRangesCombinedInOneRangeWhenGapIsZeroEvenIfNotAdjacent() {
    FilterCombiner<RangeListFilter> c = ListConcatCombiner.UNBOUNDED_INSTANCE;
    FilteredIndexRange<RangeListFilter> range1 = FilteredIndexRange.of(TestUtils.ZERO_ONE,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE), false);
    FilteredIndexRange<RangeListFilter> range2 = FilteredIndexRange.of(TestUtils.TWO_TEN,
        new RangeListFilter(ImmutableList.of(TestUtils.TWO_TEN), false, Level.FINE), false);
    SelectiveFilter<RangeListFilter> filter = c.combine(range1, range2, 0);
    assertEquals(SelectiveFilter.of(new RangeListFilter(
        ImmutableList.of(TestUtils.ZERO_TEN), false, Level.FINE), false), filter);
  }

  public void testTwoBorderRangesCombinedInOneRangeWhenGapIsZero() {
    FilterCombiner<RangeListFilter> c = ListConcatCombiner.UNBOUNDED_INSTANCE;
    FilteredIndexRange<RangeListFilter> range1 = FilteredIndexRange.of(TestUtils.ZERO_FOUR,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_TWO, TestUtils.THREE_FOUR), false,
            Level.FINE), false);
    FilteredIndexRange<RangeListFilter> range2 = FilteredIndexRange.of(TestUtils.SIX_TEN,
        new RangeListFilter(ImmutableList.of(TestUtils.SIX_SEVEN, TestUtils.EIGHT_TEN), false,
            Level.FINE), false);
    SelectiveFilter<RangeListFilter> filter = c.combine(range1, range2, 0);
    assertEquals(SelectiveFilter.of(new RangeListFilter(ImmutableList.of(
        TestUtils.ZERO_TWO, TestUtils.THREE_SEVEN, TestUtils.EIGHT_TEN), false, Level.FINE), false),
        filter);
  }
  
  public void testConcatenatesListsWhenGapIsPositive() {
    FilterCombiner<RangeListFilter> c = ListConcatCombiner.UNBOUNDED_INSTANCE;
    FilteredIndexRange<RangeListFilter> range1 = FilteredIndexRange.of(TestUtils.ZERO_ONE,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE), false);
    FilteredIndexRange<RangeListFilter> range2 = FilteredIndexRange.of(TestUtils.SIX_TEN,
        new RangeListFilter(ImmutableList.of(TestUtils.SIX_TEN), false, Level.FINE), false);
    SelectiveFilter<RangeListFilter> filter = c.combine(range1, range2, 1);
    assertEquals(SelectiveFilter.of(new RangeListFilter(ImmutableList.of(
        TestUtils.ZERO_ONE, TestUtils.SIX_TEN), false, Level.FINE), false), filter);
  }
}
