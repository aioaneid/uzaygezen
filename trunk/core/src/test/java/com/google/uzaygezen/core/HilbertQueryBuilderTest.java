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
 * Test case that puts together {@link CompactHilbertCurve}, {@link
 * SimpleRegionInspector}, {@link ListConcatCombiner} and {@link
 * BacktrackingQueryBuilder}.
 * 
 * @author Daniel Aioanei
 */
public class HilbertQueryBuilderTest extends TestCase {
  
  public void testSinglePointQuery() {
    TestUtils.generateSpec(4, 5, new TestUtils.IntArrayCallback() {
      @Override
      public void call(int[] m) {
        singlePointQuery(m);
      }
    });
  }
  
  private static void singlePointQuery(int[] m) {
    CompactHilbertCurve chc = new CompactHilbertCurve(m);
    final int n = m.length;
    BitVector[] p = new BitVector[n];
    for (int i = 0; i < n; ++i) {
      p[i] = BitVectorFactories.OPTIMAL.apply(chc.getSpec().getBitsPerDimension().get(i));
    }
    BitVector chi = BitVectorFactories.OPTIMAL.apply(chc.getSpec().sumBitsPerDimension());
    int pow2mSum = 1 << chc.getSpec().sumBitsPerDimension();
    List<LongRange> queryPoint = Lists.newArrayList();
    for (int i = 0; i < pow2mSum; ++i) {
      queryPoint.clear();
      int k = 0;
      for (int j = 0; j < n; ++j) {
        int val = (i >> k) & ((1 << m[j]) - 1);
        p[j].clear();
        for (int l = 0; l < m[j]; ++l) {
          if ((val & (1 << l)) != 0) {
            p[j].set(l);
          }
        }
        queryPoint.add(LongRange.of(val, val + 1));
        k += m[j];
      }
      RegionInspector<RangeListFilter> regionInspector = SimpleRegionInspector.create(
          ImmutableList.of(queryPoint), 1, RangeListFilter.creator(Level.FINE));
      QueryBuilder<RangeListFilter> queryBuilder = BacktrackingQueryBuilder.create(
          regionInspector, ListConcatCombiner.UNBOUNDED_INSTANCE, 1, true);
      chc.accept(new ZoomingSpaceVisitorAdapter(chc, queryBuilder));
      Query<RangeListFilter> actual = queryBuilder.get();
      chc.index(p, 0, chi);
      long pointCompactHilbertIndex = chi.toExactLong();
      LongRange expectedSingleRange =
          LongRange.of(pointCompactHilbertIndex, pointCompactHilbertIndex + 1);
      FilteredIndexRange<RangeListFilter> expectedIndexQueryRange =
          FilteredIndexRange.<RangeListFilter>of(expectedSingleRange,
              new RangeListFilter(ImmutableList.of(expectedSingleRange), false, Level.FINE), false);
      Query<RangeListFilter> expected = Query.of(
          ImmutableList.<FilteredIndexRange<RangeListFilter>>of(expectedIndexQueryRange));
      assertEquals(expected, actual);
      assertFalse(expected.isPotentialOverSelectivity());
    }
  }

  public void testOrder2Query() {
    RegionInspector<RangeListFilter> regionInspector = SimpleRegionInspector.create(
        ImmutableList.of(ImmutableList.<LongRange>of(TestUtils.TWO_SIX, TestUtils.TWO_FOUR)),
        1, RangeListFilter.creator(Level.FINE));
    CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {3, 3});
    LongRange expectedRange1 = LongRange.of(8, 12);
    LongRange expectedRange2 = LongRange.of(52, 56);
    FilteredIndexRange<RangeListFilter> expectedIndexQueryRange1 = FilteredIndexRange.of(
        expectedRange1, new RangeListFilter(ImmutableList.of(expectedRange1), false, Level.FINE),
        false);
    FilteredIndexRange<RangeListFilter> expectedIndexQueryRange2 = FilteredIndexRange.of(
        expectedRange2, new RangeListFilter(ImmutableList.of(expectedRange2), false, Level.FINE),
        false);
    List<FilteredIndexRange<RangeListFilter>> expected = Lists.newArrayList();
    expected.add(expectedIndexQueryRange1);
    expected.add(expectedIndexQueryRange2);
    for (int i = 2; i < 5; ++i) {
      QueryBuilder<RangeListFilter> queryBuilder = BacktrackingQueryBuilder.create(
          regionInspector, ListConcatCombiner.UNBOUNDED_INSTANCE, i, true);
      chc.accept(new ZoomingSpaceVisitorAdapter(chc, queryBuilder));
      Query<RangeListFilter> actual = queryBuilder.get();
      assertEquals(Query.of(expected), actual);
      assertFalse(actual.isPotentialOverSelectivity());
    }
  }
}
