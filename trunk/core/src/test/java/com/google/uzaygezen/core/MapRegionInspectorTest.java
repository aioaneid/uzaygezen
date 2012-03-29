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
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import org.apache.commons.lang.ArrayUtils;
import org.easymock.EasyMock;

import java.util.List;
import java.util.Map;

/**
 * TODO: Add some more unit tests!
 * 
 * @author Daniel Aioanei
 */
public class MapRegionInspectorTest extends TestCase {

  private RegionInspector<Object> mock;
  
  @Override
  protected void setUp() {
    mock = EasyMock.createStrictMock(ObjectRegionInspector.class);
  }

  @Override
  protected void tearDown() {
    EasyMock.verify(mock);
  }

  public void testDisjointWhenMapIsEmpty() {
    MapRegionInspector<Object> mapInspector = MapRegionInspector.create(
        ImmutableMap.<Pow2LengthBitSetRange, NodeValue<CountingDoubleArray>>of(), mock, false);
    List<Pow2LengthBitSetRange> orthotope = ImmutableList.of();
    EasyMock.replay(mock);
    Assessment<Object> actual = mapInspector.assess(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 9), 2), orthotope);
    assertEquals(Assessment.makeDisjoint(0), actual);
    assertTrue(mapInspector.getDisguisedCacheHits().isEmpty());
  }
  
  public void testMinimumEstimateWins() {
    Pow2LengthBitSetRange rootRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 5), 5);
    List<Pow2LengthBitSetRange> rootOrthotope = ImmutableList.of(rootRange);
    EasyMock.expect(mock.assess(rootRange, rootOrthotope)).andReturn(Assessment.makeOverlaps());
    Pow2LengthBitSetRange leftRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(4, 5), 2);
    List<Pow2LengthBitSetRange> leftOrthotope = ImmutableList.of(leftRange);
    EasyMock.expect(mock.assess(leftRange, leftOrthotope))
        .andReturn(Assessment.makeDisjoint(5));
    Pow2LengthBitSetRange rightRange =
        new Pow2LengthBitSetRange(TestUtils.createBitVector(8, 5), 2);
    List<Pow2LengthBitSetRange> rightOrthotope = ImmutableList.of(rightRange);
    EasyMock.expect(mock.assess(rightRange, rightOrthotope))
        .andReturn(Assessment.makeDisjoint(1));
    EasyMock.replay(mock);
    Map<Pow2LengthBitSetRange, NodeValue<CountingDoubleArray>> rolledupMap = ImmutableMap.of(
        rootRange, NodeValue.of(new CountingDoubleArray(20, ArrayUtils.EMPTY_DOUBLE_ARRAY), true));
    MapRegionInspector<Object> mapInspector = MapRegionInspector.create(rolledupMap, mock, false);
    assertEquals(Assessment.makeOverlaps(), mapInspector.assess(rootRange, rootOrthotope));
    assertEquals(Assessment.makeDisjoint(Math.min(4 * 20 / 32, 5)),
        mapInspector.assess(leftRange, leftOrthotope));
    assertEquals(Assessment.makeDisjoint(Math.min(4 * 20 / 32, 1)),
        mapInspector.assess(rightRange, rightOrthotope));
    assertTrue(mapInspector.getDisguisedCacheHits().isEmpty());
  }

  public void testOneDisguisedCacheHit() {
    Pow2LengthBitSetRange rootRange = new Pow2LengthBitSetRange(TestUtils.createBitVector(0, 1), 0);
    List<Pow2LengthBitSetRange> rootOrthotope = ImmutableList.of(rootRange);
    EasyMock.expect(mock.assess(rootRange, rootOrthotope)).andReturn(Assessment.makeCovered(
        new Object(), false));
    EasyMock.replay(mock);
    Map<Pow2LengthBitSetRange, NodeValue<CountingDoubleArray>> rolledupMap = ImmutableMap.of(
        rootRange, NodeValue.of(new CountingDoubleArray(ArrayUtils.EMPTY_DOUBLE_ARRAY), true));
    MapRegionInspector<Object> mapInspector = MapRegionInspector.create(rolledupMap, mock, true);
    assertEquals(Assessment.makeDisjoint(1),
        mapInspector.assess(rootRange, rootOrthotope));
    assertEquals(ImmutableMap.of(rootRange.getStart(), new CountingDoubleArray(
        ArrayUtils.EMPTY_DOUBLE_ARRAY)), mapInspector.getDisguisedCacheHits());
  }
  
  private interface ObjectRegionInspector extends RegionInspector<Object> {}
}
