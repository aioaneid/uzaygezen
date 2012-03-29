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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.PrimitiveArrays;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Transforms a {@link MapNode} tree into a {@link java.util.Map} which contains
 * as keys all paths from the root node to all nodes. The path from root to root
 * is empty and is present in the produced map iff the tree is not empty(i.e.,
 * its root is not null). The values in the map are pairs of the original tree
 * node's value and a flag indicating if the node is a leaf node.
 * 
 * @author Daniel Aioanei
 * 
 * @param <V> value type
 */
public class Pow2LengthBitSetRangeFactory<V>
    implements Function<MapNode<BitVector, V>, Map<Pow2LengthBitSetRange, NodeValue<V>>> {

  private final int[] elementLengths;
  private final int[] elementLengthSums;
  
  /**
   * @param cardinality must have the bit cardinality for each iteration. The
   * size of the list must be {@code mMax}.
   */
  private Pow2LengthBitSetRangeFactory(List<Integer> cardinality) {
    this.elementLengths = PrimitiveArrays.toIntArray(cardinality);
    elementLengthSums = new int[elementLengths.length];
    for (int i = 0; i < elementLengths.length; ++i) {
      elementLengthSums[i] = (i == 0 ? 0 : elementLengthSums[i - 1]) + elementLengths[i];
    }
  }

  public static <V> Pow2LengthBitSetRangeFactory<V> create(List<Integer> elementLengths) {
    return new Pow2LengthBitSetRangeFactory<V>(elementLengths);
  }
  
  @Override
  public Map<Pow2LengthBitSetRange, NodeValue<V>> apply(MapNode<BitVector, V> from) {
    if (from == null) {
      return ImmutableMap.of();
    }
    Deque<MapNode<BitVector, V>> inputStack = new ArrayDeque<MapNode<BitVector, V>>();
    Deque<BitVectorWithIterationLevelAndValue> outputStack =
        new ArrayDeque<BitVectorWithIterationLevelAndValue>();
    inputStack.push(from);
    int n = elementLengthSums.length;
    int bitCount = n == 0 ? 0 : elementLengthSums[n - 1];
    outputStack.push(new BitVectorWithIterationLevelAndValue(
        BitVectorFactories.OPTIMAL.apply(bitCount), n, from.getValue()));
    MapNode<BitVector, V> inputNode;
    Map<Pow2LengthBitSetRange, NodeValue<V>> map = Maps.newHashMap(); 
    while ((inputNode = inputStack.poll()) != null) {
      BitVectorWithIterationLevelAndValue outputElement = outputStack.poll();
      map.put(new Pow2LengthBitSetRange(outputElement.bitVector,
          outputElement.level == 0 ? 0 : elementLengthSums[outputElement.level - 1]),
          NodeValue.of(outputElement.value, inputNode.getChildren().isEmpty()));
      Preconditions.checkArgument(outputElement.level > 0
          || (inputNode.getChildren().isEmpty() && outputElement.level >= 0));
      for (Entry<BitVector, MapNode<BitVector, V>> entry : inputNode.getChildren().entrySet()) {
        inputStack.push(entry.getValue());
        BitVector childBitSet = outputElement.bitVector.clone();
        BitVector key = entry.getKey();
        for (int i = key.size() == 0 ? -1 : key.nextSetBit(0); i != -1;
            i = i == key.size() - 1 ? -1 : key.nextSetBit(i + 1)) {
          int bitIndex =
              (outputElement.level == 1 ? 0 : elementLengthSums[outputElement.level - 2]) + i;
          Preconditions.checkState(bitIndex < bitCount, "bitIndex is too high");
          Preconditions.checkState(!childBitSet.get(bitIndex));
          childBitSet.set(bitIndex);
        }
        outputStack.push(new BitVectorWithIterationLevelAndValue(
            childBitSet, outputElement.level - 1, entry.getValue().getValue()));
      }
    }
    Preconditions.checkState(outputStack.isEmpty() & !map.isEmpty());
    return map;
  }
  
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
  
  private class BitVectorWithIterationLevelAndValue {
    
    private final BitVector bitVector;
    private final int level;
    private final V value;
    
    public BitVectorWithIterationLevelAndValue(BitVector bitSet, int level, V value) {
      this.bitVector = bitSet;
      this.level = level;
      this.value = value;
    }
    
    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
  }
}
