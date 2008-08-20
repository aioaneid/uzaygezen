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
public class RangeListFilterTest extends TestCase {

  public void testCombineThresholdNotExceeded() {
    RangeListFilter x =
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE);
    RangeListFilter y =
        new RangeListFilter(ImmutableList.of(TestUtils.THREE_FOUR), false, Level.FINE);
    RangeListFilter z = x.combine(y, 2, 10);
    assertEquals(new RangeListFilter(
        ImmutableList.of(TestUtils.ZERO_ONE, TestUtils.THREE_FOUR), false, Level.FINE), z);
  }

  public void testCombineThresholdStaysExceededIfFirstExceeded() {
    RangeListFilter x = new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), true, Level.FINE);
    RangeListFilter y =
        new RangeListFilter(ImmutableList.of(TestUtils.THREE_FOUR), false, Level.FINE);
    RangeListFilter z = x.combine(y, 2, 10);
    assertEquals(new RangeListFilter(ImmutableList.of(TestUtils.ZERO_FOUR), true, Level.FINE), z);
  }

  public void testCombineThresholdStaysExceededIfSecondExceeded() {
    RangeListFilter x =
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE);
    RangeListFilter y =
      new RangeListFilter(ImmutableList.of(TestUtils.THREE_FOUR), true, Level.FINE);
    RangeListFilter z = x.combine(y, 2, 10);
    assertEquals(new RangeListFilter(ImmutableList.of(TestUtils.ZERO_FOUR), true, Level.FINE), z);
  }

  public void testCombineThresholdExceeded() {
    RangeListFilter x =
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE);
    RangeListFilter y =
        new RangeListFilter(ImmutableList.of(TestUtils.THREE_FOUR), false, Level.FINE);
    RangeListFilter z = x.combine(y, 1, 10);
    assertEquals(new RangeListFilter(ImmutableList.of(TestUtils.ZERO_FOUR), true, Level.FINE), z);
  }

  public void testCombineCollapsesVacuum() {
    RangeListFilter x =
        new RangeListFilter(ImmutableList.of(TestUtils.ZERO_ONE), false, Level.FINE);
    RangeListFilter y =
        new RangeListFilter(ImmutableList.of(TestUtils.THREE_FOUR), false, Level.FINE);
    for (int i = 1; i < 3; ++i) {
      RangeListFilter z = x.combine(y, i, 0);
      assertEquals(
          new RangeListFilter(ImmutableList.of(TestUtils.ZERO_FOUR), false, Level.FINE), z);
    }
  }
}
