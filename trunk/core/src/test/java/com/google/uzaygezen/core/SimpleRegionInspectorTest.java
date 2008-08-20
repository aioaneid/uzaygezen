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

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;


import junit.framework.TestCase;

import java.util.List;

/**
 * @author Daniel Aioanei
 */
public class SimpleRegionInspectorTest extends TestCase {

  public void testCover() {
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 7), 1));
    List<List<LongRange>> query =
        ImmutableList.of((List<LongRange>) ImmutableList.of(TestUtils.TWO_FOUR));
    SimpleRegionInspector<LongRange> inspector =
        SimpleRegionInspector.create(query, 1, Functions.<LongRange>identity());
    final Pow2LengthBitSetRange indexRange = new Pow2LengthBitSetRange(
        TestUtils.createBitVector(100, 7), 1);
    Assessment<LongRange> actual = inspector.assess(indexRange, orthotope);
    Assessment<LongRange> expected = Assessment.makeCovered(indexRange.toLongRange(), false);
    assertEquals(expected, actual);
  }

  public void testDisjoint() {
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 7), 0));
    List<? extends List<LongRange>> query =
        ImmutableList.of(ImmutableList.of(TestUtils.ONE_TEN));
    SimpleRegionInspector<LongRange> inspector =
        SimpleRegionInspector.create(query, 1, Functions.<LongRange>identity());
    final Pow2LengthBitSetRange indexRange = new Pow2LengthBitSetRange(
        TestUtils.createBitVector(100, 7), 0);
    Assessment<LongRange> actual = inspector.assess(indexRange, orthotope);
    Assessment<LongRange> expected = Assessment.makeDisjoint(1);
    assertEquals(expected, actual);
  }

  public void testOverlap() {
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 7), 2));
    List<? extends List<LongRange>> query =
        ImmutableList.of(ImmutableList.of(TestUtils.ONE_TEN));
    SimpleRegionInspector<LongRange> inspector =
        SimpleRegionInspector.create(query, 1, Functions.<LongRange>identity());
    final Pow2LengthBitSetRange indexRange = new Pow2LengthBitSetRange(
        TestUtils.createBitVector(100, 7), 2);
    Assessment<LongRange> actual = inspector.assess(indexRange, orthotope);
    Assessment<LongRange> expected = Assessment.makeOverlaps();
    assertEquals(expected, actual);
  }

  public void testCoverWithPotentialOverSelectivity() {
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(8, 7), 3));
    List<? extends List<LongRange>> query =
        ImmutableList.of(ImmutableList.of(TestUtils.ONE_TEN));
    final Pow2LengthBitSetRange indexRange = new Pow2LengthBitSetRange(
        TestUtils.createBitVector(80, 7), 3);
    long minOverlappingContent = indexRange.toLongRange().getEnd()
        - indexRange.toLongRange().getStart() + 1;
    SimpleRegionInspector<LongRange> inspector = SimpleRegionInspector.create(
        query, minOverlappingContent, Functions.<LongRange>identity());
    Assessment<LongRange> actual = inspector.assess(indexRange, orthotope);
    Assessment<LongRange> expected = Assessment.makeCovered(indexRange.toLongRange(), true);
    assertEquals(expected, actual);
  }
}
