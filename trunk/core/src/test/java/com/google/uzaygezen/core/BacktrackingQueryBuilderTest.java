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
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;
import java.util.logging.Level;

/**
 * @author Daniel Aioanei
 */
public class BacktrackingQueryBuilderTest extends TestCase {
  
  private static final Pow2LengthBitSetRange ZERO_ONE =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 0);
  private static final Pow2LengthBitSetRange ONE_TWO =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(1, 9), 0);
  private static final Pow2LengthBitSetRange ZERO_EIGHT =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 3);
  private static final Pow2LengthBitSetRange TWO_THREE =
    new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 9), 0);
  private static final Pow2LengthBitSetRange TWO_FOUR =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 9), 1);
  private static final Pow2LengthBitSetRange FOUR_FIVE =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(4, 9), 0);
  private static final Pow2LengthBitSetRange FOUR_SIX =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(4, 9), 1);
  private static final Pow2LengthBitSetRange FOUR_EIGHT =
      new Pow2LengthBitSetRange(TestUtils.createBitVector(4, 9), 2);
  
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ZERO_ONE_ZERO_ONE =
      ImmutableList.of(ZERO_ONE, ZERO_ONE, ZERO_ONE);
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ZERO_EIGHT_ZERO_EIGHT =
      ImmutableList.of(ZERO_ONE, ZERO_EIGHT, ZERO_EIGHT);
  private static final List<Pow2LengthBitSetRange> ZERO_EIGHT_ZERO_EIGHT_ZERO_EIGHT =
      ImmutableList.of(ZERO_EIGHT, ZERO_EIGHT, ZERO_EIGHT);
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ZERO_EIGHT_FOUR_EIGHT =
      ImmutableList.of(ZERO_ONE, ZERO_EIGHT, FOUR_EIGHT);
  private static final List<Pow2LengthBitSetRange> FOUR_EIGHT_ZERO_ONE_ZERO_ONE =
      ImmutableList.of(FOUR_EIGHT, ZERO_ONE, ZERO_ONE);
  private static final List<Pow2LengthBitSetRange> FOUR_SIX_FOUR_SIX_FOUR_SIX =
      ImmutableList.of(FOUR_SIX, FOUR_SIX, FOUR_SIX);
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ZERO_ONE_FOUR_FIVE =
      ImmutableList.of(ZERO_ONE, ZERO_ONE, FOUR_FIVE);
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ONE_TWO_TWO_FOUR =
      ImmutableList.of(ZERO_ONE, ONE_TWO, TWO_FOUR);
  
  public void testQueryBuilderForSingleSubRegionQueryEnoughRanges() {
    List<List<LongRange>> queryRegion = ImmutableList.of(TestUtils.ZERO_ONE_ZERO_TEN_ONE_TEN);
    RegionInspector<RangeListFilter> regionInspector = SimpleRegionInspector.create(
        queryRegion, 1, RangeListFilter.creator(Level.FINE));
    FilterCombiner<RangeListFilter> intervalCombiner = ListConcatCombiner.UNBOUNDED_INSTANCE;
    BacktrackingQueryBuilder<RangeListFilter> queryBuilder =
        BacktrackingQueryBuilder.create(regionInspector, intervalCombiner, 1, true);
    assertEquals(Query.emptyQuery(), queryBuilder.get());
    /*
     * In practice the indexRange will be different every time, but as long as
     * the are not COVERED, it doesn't really matter since we specify
     * canRelyOnIndex=false to the region inspector.
     */
    assertTrue(queryBuilder.visit(new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 9),
        ZERO_EIGHT_ZERO_EIGHT_ZERO_EIGHT));
    assertEquals(Query.emptyQuery(), queryBuilder.get());
    assertTrue(queryBuilder.visit(new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 6),
        ZERO_ONE_ZERO_EIGHT_ZERO_EIGHT));
    assertEquals(Query.emptyQuery(), queryBuilder.get());
    assertTrue(queryBuilder.visit(new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 6),
        ZERO_ONE_ZERO_EIGHT_ZERO_EIGHT));
    assertEquals(Query.emptyQuery(), queryBuilder.get());
    assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 5),
        ZERO_ONE_ZERO_EIGHT_FOUR_EIGHT));
    assertEquals(Query.of(ImmutableList.of(FilteredIndexRange.of(TestUtils.ZERO_THIRTY_TWO,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_THIRTY_TWO), false, Level.FINE),
        false))), queryBuilder.get());
  }
  
  public void testOneOrthotopeTwoMaxRangesThresholdNotExceeded() {
    BacktrackingQueryBuilder<RangeListFilter> queryBuilder =
        createBuilderAndVisit010101(ListConcatCombiner.UNBOUNDED_INSTANCE, 2, false);
    assertEquals(Query.of(ImmutableList.of(FilteredIndexRange.of(TestUtils.ZERO_ONE,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE), false))),
        queryBuilder.get());
  }
  
  public void testCoveredReturnedInsteadOfOverlapsWhenMaxZoomingLevelIsReached() {
    List<List<LongRange>> queryRegion =
        ImmutableList.of((List<LongRange>) ImmutableList.of(TestUtils.SIX_SEVEN));
    RegionInspector<RangeListFilter> regionInspector = SimpleRegionInspector.create(
        queryRegion, 11, RangeListFilter.creator(Level.FINE));
    BacktrackingQueryBuilder<RangeListFilter> queryBuilder = BacktrackingQueryBuilder.create(
        regionInspector, ListConcatCombiner.UNBOUNDED_INSTANCE, Integer.MAX_VALUE, true);
    assertEquals(Query.emptyQuery(), queryBuilder.get());
    assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 3),
        ImmutableList.of(ZERO_EIGHT)));
    assertEquals(Query.of(ImmutableList.of(FilteredIndexRange.of(TestUtils.ZERO_EIGHT,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_EIGHT), false, Level.FINE), true))),
        queryBuilder.get());
  }
  
  public void testTwoOrthotopeTwoMaxRangesThresholdNotExceeded() {
    BacktrackingQueryBuilder<RangeListFilter> queryBuilder =
        createBuilderAndVisit010101(ListConcatCombiner.UNBOUNDED_INSTANCE, 2, false);
    visit0101110(queryBuilder);
    List<FilteredIndexRange<RangeListFilter>> expected = Lists.newArrayList();
    expected.add(FilteredIndexRange.of(TestUtils.ZERO_ONE,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE), false));
    expected.add(FilteredIndexRange.of(TestUtils.ONE_TWO,
        new RangeListFilter(ImmutableList.of(TestUtils.ONE_TWO), false, Level.FINE), false));
    assertEquals(Query.of(expected), queryBuilder.get());
  }
  
  public void testTwoOrthotopeTwoMaxRangesMaxFilteredRangesExceededAndThresholdExceeded() {
    BacktrackingQueryBuilder<RangeListFilter> queryBuilder =
        createBuilderAndVisit010101(new ListConcatCombiner(1), 1, false);
    assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(TestUtils.createBitVector(1, 9), 0),
        ImmutableList.of(ZERO_ONE, TWO_THREE, FOUR_FIVE)));
    assertFalse(queryBuilder.visit(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 9), 0), ZERO_ONE_ZERO_ONE_ZERO_ONE));
    List<FilteredIndexRange<RangeListFilter>> expected = Lists.newArrayList();
    expected.add(FilteredIndexRange.of(TestUtils.ZERO_THREE,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_THREE), true, Level.FINE), true));
    assertEquals(Query.of(expected), queryBuilder.get());
  }

  public void testThreeOrthotopesLargeMaxRanges() {
    BacktrackingQueryBuilder<RangeListFilter> queryBuilder =
        createBuilderAndVisit010101(ListConcatCombiner.UNBOUNDED_INSTANCE, Integer.MAX_VALUE, true);
    visit0101110(queryBuilder);
    // Insert a 10 gap.
    visit011327(queryBuilder);
    visit1100101(queryBuilder);
    // The first 2 intervals have been joined since they had a zero gap.
    List<FilteredIndexRange<RangeListFilter>> expectedList = Lists.newArrayList();
    expectedList.add(FilteredIndexRange.of(TestUtils.ZERO_TWO,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_TWO), false, Level.FINE), false));
    expectedList.add(FilteredIndexRange.of(TestUtils.FOUR_EIGHT,
        new RangeListFilter(ImmutableList.of(TestUtils.FOUR_EIGHT), false, Level.FINE), false));
    assertEquals(Query.of(expectedList), queryBuilder.get());
  }

  private void visit011327(
      BacktrackingQueryBuilder<RangeListFilter> queryBuilder) {
    assertFalse(queryBuilder.visit(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 9), 1), ZERO_ONE_ONE_TWO_TWO_FOUR));
  }
  
  public void testQueryBuilderForSingleSubRegionQueryNotEnoughRangesExactSelectivity() {
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(
        ListConcatCombiner.UNBOUNDED_INSTANCE, false);
  }
  
  public void testQueryBuilderForSingleSubRegionQueryNotEnoughRangesOverSelectivity() {
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(
        new FilterCombiner<RangeListFilter>() {
          @Override
          public SelectiveFilter<RangeListFilter> combine(
              FilteredIndexRange<RangeListFilter> firstFilteredRange,
              FilteredIndexRange<RangeListFilter> secondFilteredRange,
              long gapEstimate) {
            return SelectiveFilter.of(ListConcatCombiner.UNBOUNDED_INSTANCE.combine(
                firstFilteredRange, secondFilteredRange, gapEstimate).getFilter(), true);
          }
        }, true);
  }

  private void checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(
      FilterCombiner<RangeListFilter> filterCombiner,
      boolean expectedPotentialOverSelectivity) {
    BacktrackingQueryBuilder<RangeListFilter> queryBuilder =
        createBuilderAndVisit010101(filterCombiner, 2, false);
    visit0101110(queryBuilder);
    visit011327(queryBuilder);
    visit1100101(queryBuilder);
    // Insert a 5 gap.
    assertFalse(queryBuilder.visit(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(8, 9), 3), FOUR_SIX_FOUR_SIX_FOUR_SIX));
    List<FilteredIndexRange<RangeListFilter>> expectedList = Lists.newArrayList();
    expectedList.add(FilteredIndexRange.of(TestUtils.ZERO_TWO,
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_TWO), false, Level.FINE),
        expectedPotentialOverSelectivity));
    expectedList.add(FilteredIndexRange.of(TestUtils.FOUR_EIGHT,
        new RangeListFilter(ImmutableList.of(TestUtils.FOUR_EIGHT), false, Level.FINE), false));
    Query<RangeListFilter> actual = queryBuilder.get();
    assertEquals(Query.of(expectedList), actual);
    assertEquals(expectedPotentialOverSelectivity, actual.isPotentialOverSelectivity());
  }

  private void visit1100101(BacktrackingQueryBuilder<RangeListFilter> queryBuilder) {
    assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
        TestUtils.createBitVector(4, 9), 2), FOUR_EIGHT_ZERO_ONE_ZERO_ONE));
  }

  private static void visit0101110(
      BacktrackingQueryBuilder<RangeListFilter> queryBuilder) {
    assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
        TestUtils.createBitVector(1, 9), 0), ZERO_ONE_ZERO_ONE_FOUR_FIVE));
  }

  private static BacktrackingQueryBuilder<RangeListFilter> createBuilderAndVisit010101(
      FilterCombiner<RangeListFilter> intervalCombiner, int maxRanges,
      boolean alwaysRemoveVacuum) {
    RegionInspector<RangeListFilter> regionInspector =
        SimpleRegionInspector.create(ImmutableList.of(TestUtils.ZERO_TEN_ZERO_ONE_ZERO_TEN),
            1, RangeListFilter.creator(Level.FINE));
    BacktrackingQueryBuilder<RangeListFilter> queryBuilder = BacktrackingQueryBuilder.create(
        regionInspector, intervalCombiner, maxRanges, alwaysRemoveVacuum);
    assertEquals(Query.emptyQuery(), queryBuilder.get());
    assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 0),
        ZERO_ONE_ZERO_ONE_ZERO_ONE));
    return queryBuilder;
  }
}
