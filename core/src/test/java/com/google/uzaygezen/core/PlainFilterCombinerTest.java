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
public class PlainFilterCombinerTest {

  @Test
  public void combine() {
    Object filter = new Object();
    PlainFilterCombiner<Object, Long, LongContent, LongRange> combiner = new PlainFilterCombiner<Object, Long, LongContent, LongRange>(
      filter);
    for (int i = 2; --i >= 0;) {
      for (int j = 2; --j >= 0;) {
        for (int k = 2; --k >= 0;) {
          SelectiveFilter<Object> actual = combiner.combine(
            FilteredIndexRange.of(TestUtils.THREE_FOUR, new Object(), i == 1),
            FilteredIndexRange.of(TestUtils.SIX_SEVEN, new Object(), j == 1), new LongContent(k));
          Assert.assertEquals(SelectiveFilter.of(filter, i == 1 | j == 1 | k == 1), actual);
        }
      }
    }
  }
}
