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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.uzaygezen.core.ranges.LongRange;
import com.google.uzaygezen.core.ranges.LongRangeHome;

/**
 * @author Daniel Aioanei
 */
public class BacktrackingQueryBuilderTest {

  private static final Pow2LengthBitSetRange ZERO_ONE = new Pow2LengthBitSetRange(
    TestUtils.createBitVector(0, 9), 0);
  private static final Pow2LengthBitSetRange ONE_TWO = new Pow2LengthBitSetRange(
    TestUtils.createBitVector(1, 9), 0);
  private static final Pow2LengthBitSetRange ZERO_EIGHT = new Pow2LengthBitSetRange(
    TestUtils.createBitVector(0, 9), 3);
  private static final Pow2LengthBitSetRange TWO_THREE = new Pow2LengthBitSetRange(
    TestUtils.createBitVector(2, 9), 0);
  private static final Pow2LengthBitSetRange TWO_FOUR = new Pow2LengthBitSetRange(
    TestUtils.createBitVector(2, 9), 1);
  private static final Pow2LengthBitSetRange FOUR_FIVE = new Pow2LengthBitSetRange(
    TestUtils.createBitVector(4, 9), 0);
  private static final Pow2LengthBitSetRange FOUR_SIX = new Pow2LengthBitSetRange(
    TestUtils.createBitVector(4, 9), 1);
  private static final Pow2LengthBitSetRange FOUR_EIGHT = new Pow2LengthBitSetRange(
    TestUtils.createBitVector(4, 9), 2);

  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ZERO_ONE_ZERO_ONE = ImmutableList.of(
    ZERO_ONE, ZERO_ONE, ZERO_ONE);
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ZERO_EIGHT_ZERO_EIGHT = ImmutableList.of(
    ZERO_ONE, ZERO_EIGHT, ZERO_EIGHT);
  private static final List<Pow2LengthBitSetRange> ZERO_EIGHT_ZERO_EIGHT_ZERO_EIGHT = ImmutableList.of(
    ZERO_EIGHT, ZERO_EIGHT, ZERO_EIGHT);
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ZERO_EIGHT_FOUR_EIGHT = ImmutableList.of(
    ZERO_ONE, ZERO_EIGHT, FOUR_EIGHT);
  private static final List<Pow2LengthBitSetRange> FOUR_EIGHT_ZERO_ONE_ZERO_ONE = ImmutableList.of(
    FOUR_EIGHT, ZERO_ONE, ZERO_ONE);
  private static final List<Pow2LengthBitSetRange> FOUR_SIX_FOUR_SIX_FOUR_SIX = ImmutableList.of(
    FOUR_SIX, FOUR_SIX, FOUR_SIX);
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ZERO_ONE_FOUR_FIVE = ImmutableList.of(
    ZERO_ONE, ZERO_ONE, FOUR_FIVE);
  private static final List<Pow2LengthBitSetRange> ZERO_ONE_ONE_TWO_TWO_FOUR = ImmutableList.of(
    ZERO_ONE, ONE_TWO, TWO_FOUR);

  @Test
  public void queryBuilderForSingleSubRegionQueryEnoughRanges() {
    List<List<LongRange>> queryRegion = ImmutableList.of(TestUtils.ZERO_ONE_ZERO_TEN_ONE_TEN);
    RegionInspector<RangeListFilter<Long, LongContent, LongRange>, LongContent> regionInspector = SimpleRegionInspector.create(
      queryRegion, TestUtils.ONE_LONG_CONTENT,
      RangeListFilter.creator(Level.FINE, LongRangeHome.INSTANCE), LongRangeHome.INSTANCE,
      TestUtils.ZERO_LONG_CONTENT);
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> intervalCombiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder = BacktrackingQueryBuilder.create(
      regionInspector, intervalCombiner, 1, true, LongRangeHome.INSTANCE,
      TestUtils.ZERO_LONG_CONTENT);
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    /*
     * In practice the indexRange will be different every time, but as long as
     * the are not COVERED, it doesn't really matter since we specify
     * canRelyOnIndex=false to the region inspector.
     */
    Assert.assertTrue(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 9),
      ZERO_EIGHT_ZERO_EIGHT_ZERO_EIGHT));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertTrue(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 6), ZERO_ONE_ZERO_EIGHT_ZERO_EIGHT));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertTrue(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 6), ZERO_ONE_ZERO_EIGHT_ZERO_EIGHT));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 5), ZERO_ONE_ZERO_EIGHT_FOUR_EIGHT));
    Assert.assertEquals(Query.of(ImmutableList.of(FilteredIndexRange.of(
      TestUtils.ZERO_THIRTY_TWO, new RangeListFilter<>(
        ImmutableList.of(TestUtils.ZERO_THIRTY_TWO), false, Level.FINE, LongRangeHome.INSTANCE),
      false))), queryBuilder.get());
  }

  @Test
  public void oneOrthotopeTwoMaxRangesThresholdNotExceeded() {
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> combiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder = createBuilderAndVisit010101(
      combiner, 2, false);
    Assert.assertEquals(
      Query.of(ImmutableList.of(FilteredIndexRange.of(
        TestUtils.ZERO_ONE, new RangeListFilter<>(
          ImmutableList.<LongRange>of(TestUtils.ZERO_ONE), false, Level.FINE,
          LongRangeHome.INSTANCE), false))), queryBuilder.get());
  }

  @Test
  public void coveredReturnedInsteadOfOverlapsWhenMaxZoomingLevelIsReached() {
    List<List<LongRange>> queryRegion = ImmutableList.of((List<LongRange>) ImmutableList.of(TestUtils.SIX_SEVEN));
    RegionInspector<RangeListFilter<Long, LongContent, LongRange>, LongContent> regionInspector = SimpleRegionInspector.create(
      queryRegion, new LongContent(11),
      RangeListFilter.creator(Level.FINE, LongRangeHome.INSTANCE), LongRangeHome.INSTANCE,
      TestUtils.ZERO_LONG_CONTENT);
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> combiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder = BacktrackingQueryBuilder.create(
      regionInspector, combiner, Integer.MAX_VALUE, true, LongRangeHome.INSTANCE,
      TestUtils.ZERO_LONG_CONTENT);
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 3), ImmutableList.of(ZERO_EIGHT)));
    Assert.assertEquals(
      Query.of(ImmutableList.of(FilteredIndexRange.of(TestUtils.ZERO_EIGHT, new RangeListFilter<>(
        ImmutableList.of(TestUtils.ZERO_EIGHT), false, Level.FINE, LongRangeHome.INSTANCE), true))),
      queryBuilder.get());
  }

  @Test
  public void twoOrthotopeTwoMaxRangesThresholdNotExceeded() {
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> combiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder = createBuilderAndVisit010101(
      combiner, 2, false);
    visit0101110(queryBuilder);
    List<FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange>> expected = new ArrayList<>();
    expected.add(FilteredIndexRange.of(
      TestUtils.ZERO_ONE, new RangeListFilter<>(
        ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE, LongRangeHome.INSTANCE), false));
    expected.add(FilteredIndexRange.of(
      TestUtils.ONE_TWO, new RangeListFilter<>(
        ImmutableList.of(TestUtils.ONE_TWO), false, Level.FINE, LongRangeHome.INSTANCE), false));
    Assert.assertEquals(Query.of(expected), queryBuilder.get());
  }

  @Test
  public void twoOrthotopeTwoMaxRangesMaxFilteredRangesExceededAndThresholdExceeded() {
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> concatCombiner = new ListConcatCombiner<>(
      1);
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder = createBuilderAndVisit010101(
      concatCombiner, 1, false);
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(1, 9), 0),
      ImmutableList.of(ZERO_ONE, TWO_THREE, FOUR_FIVE)));
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 9), 0), ZERO_ONE_ZERO_ONE_ZERO_ONE));
    List<FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange>> expected = Lists.newArrayList();
    expected.add(FilteredIndexRange.of(
      TestUtils.ZERO_THREE, new RangeListFilter<>(
        ImmutableList.of(TestUtils.ZERO_THREE), true, Level.FINE, LongRangeHome.INSTANCE), true));
    Assert.assertEquals(Query.of(expected), queryBuilder.get());
  }

  @Test
  public void threeOrthotopesLargeMaxRanges() {
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> combiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder = createBuilderAndVisit010101(
      combiner, Integer.MAX_VALUE, true);
    visit0101110(queryBuilder);
    // Insert a 10 gap.
    visit011327(queryBuilder);
    visit1100101(queryBuilder);
    // The first 2 intervals have been joined since they had a zero gap.
    List<FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange>> expectedList = Lists.newArrayList();
    expectedList.add(FilteredIndexRange.of(
      TestUtils.ZERO_TWO, new RangeListFilter<>(
        ImmutableList.of(TestUtils.ZERO_TWO), false, Level.FINE, LongRangeHome.INSTANCE), false));
    expectedList.add(FilteredIndexRange.of(TestUtils.FOUR_EIGHT, new RangeListFilter<>(
      ImmutableList.of(TestUtils.FOUR_EIGHT), false, Level.FINE, LongRangeHome.INSTANCE), false));
    Assert.assertEquals(Query.of(expectedList), queryBuilder.get());
  }

  private void visit011327(
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder) {
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 9), 1), ZERO_ONE_ONE_TWO_TWO_FOUR));
  }

  @Test
  public void queryBuilderForSingleSubRegionQueryNotEnoughRangesExactSelectivity() {
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> combiner = ListConcatCombiner.unbounded();
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(combiner, false);
  }

  @Test
  public void queryBuilderForSingleSubRegionQueryNotEnoughRangesOverSelectivity() {
    final FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> combiner = ListConcatCombiner.unbounded();
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(
      new FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange>() {
        @Override
        public SelectiveFilter<RangeListFilter<Long, LongContent, LongRange>> combine(
          FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> firstFilteredRange,
          FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange> secondFilteredRange,
          LongContent gapEstimate) {
          return SelectiveFilter.of(
            combiner.combine(firstFilteredRange, secondFilteredRange, gapEstimate).getFilter(),
            true);
        }
      }, true);
  }

  private void checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> filterCombiner,
    boolean expectedPotentialOverSelectivity) {
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder = createBuilderAndVisit010101(
      filterCombiner, 2, false);
    visit0101110(queryBuilder);
    visit011327(queryBuilder);
    visit1100101(queryBuilder);
    // Insert a 5 gap.
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(8, 9), 3), FOUR_SIX_FOUR_SIX_FOUR_SIX));
    List<FilteredIndexRange<RangeListFilter<Long, LongContent, LongRange>, LongRange>> expectedList = Lists.newArrayList();
    expectedList.add(FilteredIndexRange.of(
      TestUtils.ZERO_TWO, new RangeListFilter<>(
        ImmutableList.of(TestUtils.ZERO_TWO), false, Level.FINE, LongRangeHome.INSTANCE),
      expectedPotentialOverSelectivity));
    expectedList.add(FilteredIndexRange.of(TestUtils.FOUR_EIGHT, new RangeListFilter<>(
      ImmutableList.of(TestUtils.FOUR_EIGHT), false, Level.FINE, LongRangeHome.INSTANCE), false));
    Query<RangeListFilter<Long, LongContent, LongRange>, LongRange> actual = queryBuilder.get();
    Assert.assertEquals(Query.of(expectedList), actual);
    Assert.assertEquals(expectedPotentialOverSelectivity, actual.isPotentialOverSelectivity());
  }

  private void visit1100101(
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder) {
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(4, 9), 2), FOUR_EIGHT_ZERO_ONE_ZERO_ONE));
  }

  private static void visit0101110(
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder) {
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(1, 9), 0), ZERO_ONE_ZERO_ONE_FOUR_FIVE));
  }

  private static QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> createBuilderAndVisit010101(
    FilterCombiner<RangeListFilter<Long, LongContent, LongRange>, LongContent, LongRange> intervalCombiner,
    int maxRanges, boolean alwaysRemoveVacuum) {
    RegionInspector<RangeListFilter<Long, LongContent, LongRange>, LongContent> regionInspector = SimpleRegionInspector.create(
      ImmutableList.of(TestUtils.ZERO_TEN_ZERO_ONE_ZERO_TEN), new LongContent(1),
      RangeListFilter.creator(Level.FINE, LongRangeHome.INSTANCE), LongRangeHome.INSTANCE,
      TestUtils.ZERO_LONG_CONTENT);
    QueryBuilder<RangeListFilter<Long, LongContent, LongRange>, LongRange> queryBuilder = BacktrackingQueryBuilder.create(
      regionInspector, intervalCombiner, maxRanges, alwaysRemoveVacuum, LongRangeHome.INSTANCE,
      TestUtils.ZERO_LONG_CONTENT);
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertFalse(queryBuilder.visit(
      new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 0), ZERO_ONE_ZERO_ONE_ZERO_ONE));
    return queryBuilder;
  }
}
