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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.ranges.LongRange;

/**
 * @author Daniel Aioanei
 */
public class QueryTest {

  @Test
  public void equalsAndHashCode() {
    MoreAsserts.checkEqualsAndHashCodeMethods(
      Query.emptyQuery(), Query.of(ImmutableList.<FilteredIndexRange<Object, LongRange>> of()),
      true);
    FilteredIndexRange<Object, LongRange> fir = new FilteredIndexRange<Object, LongRange>(
      LongRange.of(1, 2), new Object(), false);
    MoreAsserts.checkEqualsAndHashCodeMethods(
      Query.emptyQuery(), Query.of(ImmutableList.<FilteredIndexRange<Object, LongRange>> of(fir)),
      false);
  }

  @Test
  public void properties() {
    FilteredIndexRange<Integer, LongRange> fir = FilteredIndexRange.of(TestUtils.ONE_TEN, 5, true);
    List<FilteredIndexRange<Integer, LongRange>> filteredIndexRanges = ImmutableList.of(fir);
    Query<Integer, LongRange> query = Query.of(filteredIndexRanges);
    Assert.assertEquals(filteredIndexRanges, query.getFilteredIndexRanges());
    Assert.assertTrue(query.isPotentialOverSelectivity());
  }
}
