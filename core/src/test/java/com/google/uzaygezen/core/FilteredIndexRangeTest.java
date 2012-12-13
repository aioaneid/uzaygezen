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

import org.junit.Assert;
import org.junit.Test;

import com.google.uzaygezen.core.ranges.LongRange;

/**
 * @author Daniel Aioanei
 */
public class FilteredIndexRangeTest {

  @Test
  public void equalsAndHashCode() {
    FilteredIndexRange<Integer, LongRange> x = FilteredIndexRange.of(
        LongRange.of(4, 10), 20, false);
    MoreAsserts.checkEqualsAndHashCodeMethods(x, x, true);
    MoreAsserts.checkEqualsAndHashCodeMethods(
        x, FilteredIndexRange.of(LongRange.of(5, 10), 20, true), false);
    MoreAsserts.checkEqualsAndHashCodeMethods(
        x, FilteredIndexRange.of(LongRange.of(4, 11), 20, true), false);
    MoreAsserts.checkEqualsAndHashCodeMethods(
        x, FilteredIndexRange.of(LongRange.of(4, 10), 21, true), false);
  }

  @Test
  public void getIndexRange() {
    FilteredIndexRange<Long, LongRange> filteredIndexRange =
        FilteredIndexRange.of(TestUtils.ONE_TEN, 0L, false);
    Assert.assertSame(TestUtils.ONE_TEN, filteredIndexRange.getIndexRange());
    Assert.assertEquals(0, filteredIndexRange.getFilter().longValue());
    Assert.assertFalse(filteredIndexRange.isPotentialOverSelectivity());
  }
}
