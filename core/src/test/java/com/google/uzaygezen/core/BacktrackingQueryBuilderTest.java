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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.ranges.BigIntegerContent;
import com.google.uzaygezen.core.ranges.BigIntegerRangeHome;
import com.google.uzaygezen.core.ranges.LongRangeHome;
import com.google.uzaygezen.core.ranges.Range;
import com.google.uzaygezen.core.ranges.RangeHome;

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

  private static Function<Long, Long> longFactory = Functions.identity();

  private static Function<Long, LongContent> longContentFactory = new Function<Long, LongContent>() {
    public LongContent apply(Long input) {
      return new LongContent(input);
    }
  };

  private static Function<Long, BigInteger> bigIntegerFactory = new Function<Long, BigInteger>() {
    @Override
    public BigInteger apply(Long input) {
      return BigInteger.valueOf(input);
    }
  };

  private static Function<Long, BigIntegerContent> bigIntegerContentFactory = new Function<Long, BigIntegerContent>() {
    public BigIntegerContent apply(Long input) {
      return new BigIntegerContent(BigInteger.valueOf(input));
    }
  };

  @Test
  public void queryBuilderForSingleSubRegionQueryEnoughRanges() {
    checkQueryBuilderForSingleSubRegionQueryEnoughRanges(
      longFactory, LongRangeHome.INSTANCE, longContentFactory);
    checkQueryBuilderForSingleSubRegionQueryEnoughRanges(
      bigIntegerFactory, BigIntegerRangeHome.INSTANCE, bigIntegerContentFactory);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkQueryBuilderForSingleSubRegionQueryEnoughRanges(
    Function<Long, T> tFactory, RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory) {
    List<ImmutableList<R>> queryRegion = ImmutableList.of(ImmutableList.of(
      rangeHome.of(tFactory.apply(0L), tFactory.apply(1L)),
      rangeHome.of(tFactory.apply(0L), tFactory.apply(10L)),
      rangeHome.of(tFactory.apply(1L), tFactory.apply(10L))));
    RegionInspector<RangeListFilter<T, V, R>, V> regionInspector = SimpleRegionInspector.create(
      queryRegion, vFactory.apply(1L), RangeListFilter.creator(Level.FINE, rangeHome), rangeHome,
      vFactory.apply(0L));
    FilterCombiner<RangeListFilter<T, V, R>, V, R> intervalCombiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder = BacktrackingQueryBuilder.create(
      regionInspector, intervalCombiner, 1, true, rangeHome, vFactory.apply(0L));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    /*
     * In practice the indexRange will be different every time, but as long as
     * the are not COVERED, it doesn't really matter since we specify
     * canRelyOnIndex=false to the region inspector.
     */
    Assert.assertTrue(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 9), 9), ZERO_EIGHT_ZERO_EIGHT_ZERO_EIGHT));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertTrue(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 9), 6), ZERO_ONE_ZERO_EIGHT_ZERO_EIGHT));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertTrue(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 9), 6), ZERO_ONE_ZERO_EIGHT_ZERO_EIGHT));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 9), 5), ZERO_ONE_ZERO_EIGHT_FOUR_EIGHT));
    Assert.assertEquals(Query.of(ImmutableList.of(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(0L), tFactory.apply(32L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(0L), tFactory.apply(32L))), false, Level.FINE,
        rangeHome), false))), queryBuilder.get());
  }

  @Test
  public void oneOrthotopeTwoMaxRangesThresholdNotExceeded() {
    checkOneOrthotopeTwoMaxRangesThresholdNotExceeded(
      longFactory, LongRangeHome.INSTANCE, longContentFactory);
    checkOneOrthotopeTwoMaxRangesThresholdNotExceeded(
      bigIntegerFactory, BigIntegerRangeHome.INSTANCE, bigIntegerContentFactory);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkOneOrthotopeTwoMaxRangesThresholdNotExceeded(
    Function<Long, T> tFactory, RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory) {
    FilterCombiner<RangeListFilter<T, V, R>, V, R> combiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder = createBuilderAndVisit010101(
      combiner, tFactory, rangeHome, vFactory, 2, false);
    Assert.assertEquals(Query.of(ImmutableList.of(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(0L), tFactory.apply(1L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(0L), tFactory.apply(1L))), false, Level.FINE,
        rangeHome), false))), queryBuilder.get());
  }

  @Test
  public void coveredReturnedInsteadOfOverlapsWhenMaxZoomingLevelIsReached() {
    checkCoveredReturnedInsteadOfOverlapsWhenMaxZoomingLevelIsReached(
      longFactory, LongRangeHome.INSTANCE, longContentFactory);
    checkCoveredReturnedInsteadOfOverlapsWhenMaxZoomingLevelIsReached(
      bigIntegerFactory, BigIntegerRangeHome.INSTANCE, bigIntegerContentFactory);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkCoveredReturnedInsteadOfOverlapsWhenMaxZoomingLevelIsReached(
    Function<Long, T> tFactory, RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory) {
    List<ImmutableList<R>> queryRegion = ImmutableList.of(ImmutableList.of(rangeHome.of(
      tFactory.apply(6L), tFactory.apply(7L))));
    RegionInspector<RangeListFilter<T, V, R>, V> regionInspector = SimpleRegionInspector.create(
      queryRegion, vFactory.apply(11L), RangeListFilter.creator(Level.FINE, rangeHome), rangeHome,
      vFactory.apply(0L));
    FilterCombiner<RangeListFilter<T, V, R>, V, R> combiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder = BacktrackingQueryBuilder.create(
      regionInspector, combiner, Integer.MAX_VALUE, true, rangeHome, vFactory.apply(0L));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 9), 3), ImmutableList.of(ZERO_EIGHT)));
    Assert.assertEquals(Query.of(ImmutableList.of(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(0L), tFactory.apply(8L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(0L), tFactory.apply(8L))), false, Level.FINE,
        rangeHome), true))), queryBuilder.get());
  }

  @Test
  public void twoOrthotopeTwoMaxRangesThresholdNotExceeded() {
    checkTwoOrthotopeTwoMaxRangesThresholdNotExceeded(
      longFactory, LongRangeHome.INSTANCE, longContentFactory);
    checkTwoOrthotopeTwoMaxRangesThresholdNotExceeded(
      bigIntegerFactory, BigIntegerRangeHome.INSTANCE, bigIntegerContentFactory);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkTwoOrthotopeTwoMaxRangesThresholdNotExceeded(
    Function<Long, T> tFactory, RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory) {
    FilterCombiner<RangeListFilter<T, V, R>, V, R> combiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder = createBuilderAndVisit010101(
      combiner, tFactory, rangeHome, vFactory, 2, false);
    visit0101110(queryBuilder);
    List<FilteredIndexRange<RangeListFilter<T, V, R>, R>> expected = new ArrayList<>();
    expected.add(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(0L), tFactory.apply(1L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(0L), tFactory.apply(1L))), false, Level.FINE,
        rangeHome), false));
    expected.add(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(1L), tFactory.apply(2L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(1L), tFactory.apply(2L))), false, Level.FINE,
        rangeHome), false));
    Assert.assertEquals(Query.of(expected), queryBuilder.get());
  }

  @Test
  public void twoOrthotopeTwoMaxRangesMaxFilteredRangesExceededAndThresholdExceeded() {
    checkTwoOrthotopeTwoMaxRangesMaxFilteredRangesExceededAndThresholdExceeded(
      longFactory, LongRangeHome.INSTANCE, longContentFactory);
    checkTwoOrthotopeTwoMaxRangesMaxFilteredRangesExceededAndThresholdExceeded(
      bigIntegerFactory, BigIntegerRangeHome.INSTANCE, bigIntegerContentFactory);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkTwoOrthotopeTwoMaxRangesMaxFilteredRangesExceededAndThresholdExceeded(
    Function<Long, T> tFactory, RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory) {
    FilterCombiner<RangeListFilter<T, V, R>, V, R> concatCombiner = new ListConcatCombiner<>(1);
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder = createBuilderAndVisit010101(
      concatCombiner, tFactory, rangeHome, vFactory, 1, false);
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(1, 9), 0), ImmutableList.of(ZERO_ONE, TWO_THREE, FOUR_FIVE)));
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(2, 9), 0), ZERO_ONE_ZERO_ONE_ZERO_ONE));
    List<FilteredIndexRange<RangeListFilter<T, V, R>, R>> expected = new ArrayList<>();
    expected.add(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(0L), tFactory.apply(3L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(0L), tFactory.apply(3L))), true, Level.FINE,
        rangeHome), true));
    Assert.assertEquals(Query.of(expected), queryBuilder.get());
  }

  @Test
  public void threeOrthotopesLargeMaxRanges() {
    checkThreeOrthotopesLargeMaxRanges(longFactory, LongRangeHome.INSTANCE, longContentFactory);
    checkThreeOrthotopesLargeMaxRanges(
      bigIntegerFactory, BigIntegerRangeHome.INSTANCE, bigIntegerContentFactory);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkThreeOrthotopesLargeMaxRanges(
    Function<Long, T> tFactory, RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory) {
    FilterCombiner<RangeListFilter<T, V, R>, V, R> combiner = ListConcatCombiner.unbounded();
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder = createBuilderAndVisit010101(
      combiner, tFactory, rangeHome, vFactory, Integer.MAX_VALUE, true);
    visit0101110(queryBuilder);
    // Insert a 10 gap.
    visit011327(queryBuilder);
    visit1100101(queryBuilder);
    // The first 2 intervals have been joined since they had a zero gap.
    List<FilteredIndexRange<RangeListFilter<T, V, R>, R>> expectedList = new ArrayList<>();
    expectedList.add(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(0L), tFactory.apply(2L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(0L), tFactory.apply(2L))), false, Level.FINE,
        rangeHome), false));
    expectedList.add(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(4L), tFactory.apply(8L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(4L), tFactory.apply(8L))), false, Level.FINE,
        rangeHome), false));
    Assert.assertEquals(Query.of(expectedList), queryBuilder.get());
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void visit011327(
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder) {
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(2, 9), 1), ZERO_ONE_ONE_TWO_TWO_FOUR));
  }

  @Test
  public void queryBuilderForSingleSubRegionQueryNotEnoughRangesExactSelectivity() {
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRangesExactSelectivity(
      longFactory, LongRangeHome.INSTANCE, longContentFactory);
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRangesExactSelectivity(
      bigIntegerFactory, BigIntegerRangeHome.INSTANCE, bigIntegerContentFactory);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkQueryBuilderForSingleSubRegionQueryNotEnoughRangesExactSelectivity(
    Function<Long, T> tFactory, RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory) {
    FilterCombiner<RangeListFilter<T, V, R>, V, R> combiner = ListConcatCombiner.unbounded();
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(
      combiner, tFactory, rangeHome, vFactory, false);
  }

  @Test
  public void queryBuilderForSingleSubRegionQueryNotEnoughRangesOverSelectivity() {
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRangesOverSelectivity(
      longFactory, LongRangeHome.INSTANCE, longContentFactory);
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRangesOverSelectivity(
      bigIntegerFactory, BigIntegerRangeHome.INSTANCE, bigIntegerContentFactory);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkQueryBuilderForSingleSubRegionQueryNotEnoughRangesOverSelectivity(
    Function<Long, T> tFactory, RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory) {
    final FilterCombiner<RangeListFilter<T, V, R>, V, R> combiner = ListConcatCombiner.unbounded();
    checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(
      new FilterCombiner<RangeListFilter<T, V, R>, V, R>() {
        @Override
        public SelectiveFilter<RangeListFilter<T, V, R>> combine(
          FilteredIndexRange<RangeListFilter<T, V, R>, R> firstFilteredRange,
          FilteredIndexRange<RangeListFilter<T, V, R>, R> secondFilteredRange, V gapEstimate) {
          return SelectiveFilter.of(
            combiner.combine(firstFilteredRange, secondFilteredRange, gapEstimate).getFilter(),
            true);
        }
      }, tFactory, rangeHome, vFactory, true);
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void checkQueryBuilderForSingleSubRegionQueryNotEnoughRanges(
    FilterCombiner<RangeListFilter<T, V, R>, V, R> filterCombiner, Function<Long, T> tFactory,
    RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory,
    boolean expectedPotentialOverSelectivity) {
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder = createBuilderAndVisit010101(
      filterCombiner, tFactory, rangeHome, vFactory, 2, false);
    visit0101110(queryBuilder);
    visit011327(queryBuilder);
    visit1100101(queryBuilder);
    // Insert a 5 gap.
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(8, 9), 3), FOUR_SIX_FOUR_SIX_FOUR_SIX));
    List<FilteredIndexRange<RangeListFilter<T, V, R>, R>> expectedList = new ArrayList<>();
    expectedList.add(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(0L), tFactory.apply(2L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(0L), tFactory.apply(2L))), false, Level.FINE,
        rangeHome), expectedPotentialOverSelectivity));
    expectedList.add(FilteredIndexRange.of(
      rangeHome.of(tFactory.apply(4L), tFactory.apply(8L)),
      new RangeListFilter<>(
        ImmutableList.of(rangeHome.of(tFactory.apply(4L), tFactory.apply(8L))), false, Level.FINE,
        rangeHome), false));
    Query<RangeListFilter<T, V, R>, R> actual = queryBuilder.get();
    Assert.assertEquals(Query.of(expectedList), actual);
    Assert.assertEquals(expectedPotentialOverSelectivity, actual.isPotentialOverSelectivity());
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void visit1100101(
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder) {
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(4, 9), 2), FOUR_EIGHT_ZERO_ONE_ZERO_ONE));
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> void visit0101110(
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder) {
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(1, 9), 0), ZERO_ONE_ZERO_ONE_FOUR_FIVE));
  }

  private static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> QueryBuilder<RangeListFilter<T, V, R>, R> createBuilderAndVisit010101(
    FilterCombiner<RangeListFilter<T, V, R>, V, R> intervalCombiner, Function<Long, T> tFactory,
    RangeHome<T, V, R> rangeHome, Function<Long, V> vFactory, int maxRanges,
    boolean alwaysRemoveVacuum) {
    R zeroTen = rangeHome.of(tFactory.apply(0L), tFactory.apply(10L));
    R zeroOne = rangeHome.of(tFactory.apply(0L), tFactory.apply(1L));
    RegionInspector<RangeListFilter<T, V, R>, V> regionInspector = SimpleRegionInspector.create(
      ImmutableList.of(ImmutableList.of(zeroTen, zeroOne, zeroTen)), vFactory.apply(1L),
      RangeListFilter.creator(Level.FINE, rangeHome), rangeHome, vFactory.apply(0L));
    QueryBuilder<RangeListFilter<T, V, R>, R> queryBuilder = BacktrackingQueryBuilder.create(
      regionInspector, intervalCombiner, maxRanges, alwaysRemoveVacuum, rangeHome,
      vFactory.apply(0L));
    Assert.assertEquals(Query.emptyQuery(), queryBuilder.get());
    Assert.assertFalse(queryBuilder.visit(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 9), 0), ZERO_ONE_ZERO_ONE_ZERO_ONE));
    return queryBuilder;
  }
}
