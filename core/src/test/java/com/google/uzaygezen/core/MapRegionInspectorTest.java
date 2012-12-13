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
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * TODO: Add some more unit tests!
 * 
 * @author Daniel Aioanei
 */
public class MapRegionInspectorTest {

  private RegionInspector<Object, LongContent> mock;

  @Before
  public void setUp() {
    mock = EasyMock.createStrictMock(ObjectRegionInspector.class);
  }

  @After
  public void tearDown() {
    EasyMock.verify(mock);
  }

  @Test
  public void disjointWhenMapIsEmpty() {
    MapRegionInspector<Object, LongContent> mapInspector = MapRegionInspector.create(
      ImmutableMap.<Pow2LengthBitSetRange, NodeValue<LongContent>> of(), mock, false,
      TestUtils.ZERO_LONG_CONTENT, TestUtils.ONE_LONG_CONTENT);
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of();
    EasyMock.replay(mock);
    Assessment<Object, LongContent> actual = mapInspector.assess(new Pow2LengthBitSetRange(
      TestUtils.createBitVector(0, 9), 2), orthotope);
    Assert.assertEquals(Assessment.makeDisjoint(TestUtils.ZERO_LONG_CONTENT), actual);
    Assert.assertTrue(mapInspector.getDisguisedCacheHits().isEmpty());
  }

  @Test
  public void minimumEstimateWins() {
    Pow2LengthBitSetRange rootRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 5), 5);
    List<Pow2LengthBitSetRange> rootOrthotope = ImmutableList.of(rootRange);
    EasyMock.expect(mock.assess(rootRange, rootOrthotope)).andReturn(
      Assessment.makeOverlaps(TestUtils.ZERO_LONG_CONTENT));
    Pow2LengthBitSetRange leftRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(4, 5), 2);
    List<Pow2LengthBitSetRange> leftOrthotope = ImmutableList.of(leftRange);
    EasyMock.expect(mock.assess(leftRange, leftOrthotope)).andReturn(
      Assessment.makeDisjoint(new LongContent(5)));
    Pow2LengthBitSetRange rightRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(8, 5), 2);
    List<Pow2LengthBitSetRange> rightOrthotope = ImmutableList.of(rightRange);
    EasyMock.expect(mock.assess(rightRange, rightOrthotope)).andReturn(
      Assessment.makeDisjoint(TestUtils.ONE_LONG_CONTENT));
    EasyMock.replay(mock);
    Map<Pow2LengthBitSetRange, NodeValue<LongContent>> rolledupMap = ImmutableMap.of(
      rootRange, NodeValue.of(new LongContent(20), true));
    MapRegionInspector<Object, LongContent> mapInspector = MapRegionInspector.create(
      rolledupMap, mock, false, TestUtils.ZERO_LONG_CONTENT, TestUtils.ONE_LONG_CONTENT);
    Assert.assertEquals(
      Assessment.makeOverlaps(TestUtils.ZERO_LONG_CONTENT), mapInspector.assess(rootRange, rootOrthotope));
    Assert.assertEquals(
      Assessment.makeDisjoint(new LongContent(Math.min(4 * 20 / 32, 5))),
      mapInspector.assess(leftRange, leftOrthotope));
    Assert.assertEquals(
      Assessment.makeDisjoint(new LongContent(Math.min(4 * 20 / 32, 1))),
      mapInspector.assess(rightRange, rightOrthotope));
    Assert.assertTrue(mapInspector.getDisguisedCacheHits().isEmpty());
  }

  @Test
  public void oneDisguisedCacheHit() {
    Pow2LengthBitSetRange rootRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 1), 0);
    List<Pow2LengthBitSetRange> rootOrthotope = ImmutableList.of(rootRange);
    EasyMock.expect(mock.assess(rootRange, rootOrthotope)).andReturn(
      Assessment.makeCovered(new Object(), false, TestUtils.ZERO_LONG_CONTENT));
    EasyMock.replay(mock);
    Map<Pow2LengthBitSetRange, NodeValue<LongContent>> rolledupMap = ImmutableMap.of(
      rootRange, NodeValue.of(TestUtils.ONE_LONG_CONTENT, true));
    MapRegionInspector<Object, LongContent> mapInspector = MapRegionInspector.create(
      rolledupMap, mock, true, TestUtils.ZERO_LONG_CONTENT, TestUtils.ONE_LONG_CONTENT);
    Assert.assertEquals(Assessment.makeDisjoint(TestUtils.ONE_LONG_CONTENT), mapInspector.assess(rootRange, rootOrthotope));
    Assert.assertEquals(
      ImmutableMap.of(rootRange.getStart(), TestUtils.ONE_LONG_CONTENT),
      mapInspector.getDisguisedCacheHits());
  }

  private interface ObjectRegionInspector extends RegionInspector<Object, LongContent> {
  }
}
