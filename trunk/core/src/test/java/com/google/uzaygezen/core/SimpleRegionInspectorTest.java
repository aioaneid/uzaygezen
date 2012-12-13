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

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.ranges.LongRange;
import com.google.uzaygezen.core.ranges.LongRangeHome;

/**
 * @author Daniel Aioanei
 */
public class SimpleRegionInspectorTest {

  @Test
  public void cover() {
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(2, 7), 1));
    List<List<LongRange>> query = ImmutableList.of((List<LongRange>) ImmutableList.of(TestUtils.TWO_FOUR));
    LongContent zero = TestUtils.ZERO_LONG_CONTENT;
    SimpleRegionInspector<LongRange, Long, LongContent, LongRange> inspector = SimpleRegionInspector.create(
      query, TestUtils.ONE_LONG_CONTENT, Functions.<LongRange> identity(), LongRangeHome.INSTANCE, zero);
    final Pow2LengthBitSetRange indexRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(
      100, 7), 1);
    Assessment<LongRange, LongContent> actual = inspector.assess(indexRange, orthotope);
    Assessment<LongRange, LongContent> expected = Assessment.makeCovered(
      LongRangeHome.INSTANCE.toRange(indexRange), false, zero);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void disjoint() {
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 7), 0));
    List<? extends List<LongRange>> query = ImmutableList.of(ImmutableList.of(TestUtils.ONE_TEN));
    LongContent zero = TestUtils.ZERO_LONG_CONTENT;
    SimpleRegionInspector<LongRange, Long, LongContent, LongRange> inspector = SimpleRegionInspector.create(
      query, TestUtils.ONE_LONG_CONTENT, Functions.<LongRange> identity(), LongRangeHome.INSTANCE, zero);
    final Pow2LengthBitSetRange indexRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(
      100, 7), 0);
    Assessment<LongRange, LongContent> actual = inspector.assess(indexRange, orthotope);
    Assessment<LongRange, LongContent> expected = Assessment.makeDisjoint(TestUtils.ONE_LONG_CONTENT);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void overlap() {
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 7), 2));
    List<? extends List<LongRange>> query = ImmutableList.of(ImmutableList.of(TestUtils.ONE_TEN));
    LongContent zero = TestUtils.ZERO_LONG_CONTENT;
    SimpleRegionInspector<LongRange, Long, LongContent, LongRange> inspector = SimpleRegionInspector.create(
      query, TestUtils.ONE_LONG_CONTENT, Functions.<LongRange> identity(), LongRangeHome.INSTANCE, zero);
    final Pow2LengthBitSetRange indexRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(
      100, 7), 2);
    Assessment<LongRange, LongContent> actual = inspector.assess(indexRange, orthotope);
    Assessment<LongRange, LongContent> expected = Assessment.<LongRange, LongContent>makeOverlaps(zero);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void coverWithPotentialOverSelectivity() {
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(8, 7), 3));
    List<? extends List<LongRange>> query = ImmutableList.of(ImmutableList.of(TestUtils.ONE_TEN));
    Pow2LengthBitSetRange indexRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(
      80, 7), 3);
    long minOverlappingContent = LongRangeHome.INSTANCE.toRange(indexRange).getEnd()
      - LongRangeHome.INSTANCE.toRange(indexRange).getStart() + 1;
    LongContent zero = TestUtils.ZERO_LONG_CONTENT;
    SimpleRegionInspector<LongRange, Long, LongContent, LongRange> inspector = SimpleRegionInspector.create(
      query, new LongContent(minOverlappingContent), Functions.<LongRange> identity(),
      LongRangeHome.INSTANCE, zero);
    Assessment<LongRange, LongContent> actual = inspector.assess(indexRange, orthotope);
    Assessment<LongRange, LongContent> expected = Assessment.makeCovered(
       LongRangeHome.INSTANCE.toRange(indexRange), true, zero);
    Assert.assertEquals(expected, actual);
  }
}
