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

import java.util.List;

/**
 * @author Daniel Aioanei
 */
public class QueryTest extends TestCase {

  public void testHashCode() {
    MoreAsserts.checkEqualsAndHashCodeMethods(Query.emptyQuery(),
        Query.of(ImmutableList.<FilteredIndexRange<Object>>of()), true);
    MoreAsserts.checkEqualsAndHashCodeMethods(Query.emptyQuery(),
        Query.of(ImmutableList.<FilteredIndexRange<Object>>of(new FilteredIndexRange<Object>(
            LongRange.of(1, 2), new Object(), false))), false);
  }

  public void testProperties() {
    List<FilteredIndexRange<Integer>> filteredIndexRanges = ImmutableList.of(
        FilteredIndexRange.of(TestUtils.ONE_TEN, 5, true));
    Query<Integer> query = Query.of(filteredIndexRanges);
    assertEquals(filteredIndexRanges, query.getFilteredIndexRanges());
    assertTrue(query.isPotentialOverSelectivity());
  }
}
