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



import junit.framework.TestCase;

/**
 * @author Daniel Aioanei
 */
public class PlainFilterCombinerTest extends TestCase {
  
  public void testCombine() {
    for (int i = 2; --i >= 0; ) {
      for (int j = 2; --j >= 0; ) {
        for (int k = 2; --k >= 0; ) {
          SelectiveFilter<Object> actual = PlainFilterCombiner.INSTANCE.combine(
              FilteredIndexRange.of(TestUtils.THREE_FOUR, new Object(), i == 1),
              FilteredIndexRange.of(TestUtils.SIX_SEVEN, new Object(), j == 1),
              k);
          assertEquals(
              SelectiveFilter.of(PlainFilterCombiner.FILTER, i == 1 | j == 1 | k == 1), actual);
        }
      }
    }
  }
}
