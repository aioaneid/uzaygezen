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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * TODO: Create an exhaustive space search test.
 * 
 * @author Daniel Aioanei
 */
public class Pow2LengthBitSetRangeFactoryTest {

  @Test
  public void noDimensions() {
    Pow2LengthBitSetRangeFactory<String> range =
        Pow2LengthBitSetRangeFactory.create(ImmutableList.<Integer>of());
    checkSingleElement(range, 0);
  }

  @Test
  public void oneDimensionAndRootNodeOnly() {
    for (int i = 0; i < 5; ++i) {
      Pow2LengthBitSetRangeFactory<String> range =
          Pow2LengthBitSetRangeFactory.create(ImmutableList.<Integer>of(i));
      checkSingleElement(range, i);
    }
  }

  @Test
  public void emptyMapProducedFromNull() {
    for (int i = 0; i < 5; ++i) {
      Pow2LengthBitSetRangeFactory<String> range =
          Pow2LengthBitSetRangeFactory.create(ImmutableList.<Integer>of(i));
      Assert.assertTrue(range.apply(null).isEmpty());
    }
  }

  @Test
  public void oneDimensionWithTwoNodes() {
    for (int bitCount = 1; bitCount < 5; ++bitCount) {
      Pow2LengthBitSetRangeFactory<String> range =
          Pow2LengthBitSetRangeFactory.create(ImmutableList.<Integer>of(bitCount));
      MapNode<BitVector, String> leftChild =
          MapNode.create("b", ImmutableMap.<BitVector, MapNode<BitVector, String>>of());
      for (int j = 0; j < 1 << bitCount; ++j) {
        BitVector leftLink = BitVectorFactories.OPTIMAL.apply(bitCount);
        leftLink.copyFrom(j);
        Map<Pow2LengthBitSetRange, NodeValue<String>> expected = ImmutableMap.of(
            new Pow2LengthBitSetRange(BitVectorFactories.OPTIMAL.apply(bitCount), bitCount),
            NodeValue.of("a", false),
            new Pow2LengthBitSetRange(leftLink, 0), NodeValue.of("b", true));
        Map<Pow2LengthBitSetRange, NodeValue<String>> actual = range.apply(MapNode.create(
            "a", ImmutableMap.<BitVector, MapNode<BitVector, String>>of(leftLink, leftChild)));
        Assert.assertEquals(expected, actual);
      }
    }
  }

  @Test
  public void oneDimensionWithThreeNodes() {
    for (int bitCount = 1; bitCount < 5; ++bitCount) {
      Pow2LengthBitSetRangeFactory<String> range =
          Pow2LengthBitSetRangeFactory.create(ImmutableList.<Integer>of(bitCount));
      MapNode<BitVector, String> leftChild =
          MapNode.create("b", ImmutableMap.<BitVector, MapNode<BitVector, String>>of());
      MapNode<BitVector, String> rightChild =
          MapNode.create("c", ImmutableMap.<BitVector, MapNode<BitVector, String>>of());
      for (int j = 0; j < 1 << bitCount; ++j) {
        BitVector leftLink = BitVectorFactories.OPTIMAL.apply(bitCount);
        leftLink.copyFrom(j);
        for (int k = 0; k < 1 << bitCount; k++) {
          if (k == j) {
            continue;
          }
          BitVector rightLink = BitVectorFactories.OPTIMAL.apply(bitCount);
          rightLink.copyFrom(k);
          for (int i = bitCount; i < 100; ++i) {
            Map<Pow2LengthBitSetRange, NodeValue<String>> expected = ImmutableMap.of(
                new Pow2LengthBitSetRange(BitVectorFactories.OPTIMAL.apply(bitCount), bitCount),
                    NodeValue.of("a", false),
                new Pow2LengthBitSetRange(leftLink, 0), NodeValue.of("b", true),
                new Pow2LengthBitSetRange(rightLink, 0), NodeValue.of("c", true));
            MapNode<BitVector, String> root = MapNode.create(
                "a", ImmutableMap.<BitVector, MapNode<BitVector, String>>of(
                    leftLink, leftChild, rightLink, rightChild));
            Map<Pow2LengthBitSetRange, NodeValue<String>> actual = range.apply(root);
            Assert.assertEquals(expected, actual);
          }
        }
      }
    }
  }
  
  @Test
  public void twoDimensionsWithFourNodes() {
    Pow2LengthBitSetRangeFactory<String> range =
        Pow2LengthBitSetRangeFactory.create(ImmutableList.<Integer>of(3, 1));
    MapNode<BitVector, String> leftChild =
        MapNode.create("b", ImmutableMap.<BitVector, MapNode<BitVector, String>>of());
    MapNode<BitVector, String> rightGrandchild =
        MapNode.create("d", ImmutableMap.<BitVector, MapNode<BitVector, String>>of());
    BitVector grandchildLink = BitVectorFactories.OPTIMAL.apply(3);
    grandchildLink.copyFrom(6);
    MapNode<BitVector, String> rightChild =
        MapNode.create("c", ImmutableMap.<BitVector, MapNode<BitVector, String>>of(
            grandchildLink, rightGrandchild));
    BitVector leftLink = BitVectorFactories.OPTIMAL.apply(1);
    BitVector rightLink = BitVectorFactories.OPTIMAL.apply(3);
    rightLink.copyFrom(1);
    MapNode<BitVector, String> root = MapNode.create(
        "a", ImmutableMap.<BitVector, MapNode<BitVector, String>>of(
            leftLink, leftChild, rightLink, rightChild));
    BitVector grandchildBitSet = BitVectorFactories.OPTIMAL.apply(4);
    grandchildBitSet.copyFrom((1 << 3) + 6);
    BitVector expectedForC = BitVectorFactories.OPTIMAL.apply(4);
    expectedForC.copyFrom(1 << 3);
    Map<Pow2LengthBitSetRange, NodeValue<String>> expected = ImmutableMap.of(
        new Pow2LengthBitSetRange(
            BitVectorFactories.OPTIMAL.apply(4), 1 + 3), NodeValue.of("a", false),
        new Pow2LengthBitSetRange(BitVectorFactories.OPTIMAL.apply(4), 3), NodeValue.of("b", true),
        new Pow2LengthBitSetRange(expectedForC, 3), NodeValue.of("c", false),
        new Pow2LengthBitSetRange(grandchildBitSet, 0), NodeValue.of("d", true));
    Map<Pow2LengthBitSetRange, NodeValue<String>> actual = range.apply(root);
    Assert.assertEquals(expected, actual);
  }

  private void checkSingleElement(Pow2LengthBitSetRangeFactory<String> range, int level) {
    Map<Pow2LengthBitSetRange, NodeValue<String>> actual =
        range.apply(MapNode.create("x", ImmutableMap.<BitVector, MapNode<BitVector, String>>of()));
    Map<Pow2LengthBitSetRange, NodeValue<String>> expected = ImmutableMap.of(
        new Pow2LengthBitSetRange(BitVectorFactories.OPTIMAL.apply(level), level),
        NodeValue.of("x", true));
    Assert.assertEquals(expected, actual);
  }
}
