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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Unmodifiable tree node. This is the output of the streaming rollup operation.
 * 
 * TODO: Add some unit tests for this class.
 * 
 * @author Daniel Aioanei
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MapNode<K, V> {
  
  private final V value;
  private final Map<K, MapNode<K, V>> children;
  
  public static <K, V> MapNode<K, V> create(V value, Map<K, MapNode<K, V>> children) {
    return new MapNode<K, V>(value, children);
  }
  
  private MapNode(V value, Map<K, MapNode<K, V>> children) {
    this.value = Preconditions.checkNotNull(value, "value");
    // Lame attempt to detect cycles.
    Preconditions.checkArgument(!children.values().contains(this), "I can't be my own child.");
    this.children = Collections.unmodifiableMap(children);
  }

  public V getValue() {
    return value;
  }
  
  public Map<K, MapNode<K, V>> getChildren() {
    return children;
  }

  /**
   * For testing purposes only.
   * 
   * @return the number of nodes in the subtree rooted in this node and the
   * number of leaves.
   */
  int[] subtreeSizeAndLeafCount() {
    Deque<MapNode<K, V>> stack = new ArrayDeque<MapNode<K, V>>();
    stack.push(this);
    MapNode<K, V> node;
    int size = 0;
    int leafCount = 0;
    while ((node = stack.poll()) != null) {
      ++size;
      if (node.children.isEmpty()) {
        ++leafCount;
      }
      for (MapNode<K, V> child : node.children.values()) {
        stack.push(child);
      }
    }
    return new int[] {size, leafCount};
  }
  
  /**
   * For testing purposes only.
   * 
   * @return the full preorder list of nodes of this subtree
   */
  public List<MapNode<K, V>> preorder() {
    List<MapNode<K, V>> list = Lists.newArrayList();
    Deque<MapNode<K, V>> stack = new ArrayDeque<MapNode<K, V>>();
    stack.push(this);
    MapNode<K, V> node;
    while ((node = stack.poll()) != null) {
      list.add(node);
      for (MapNode<K, V> child : node.children.values()) {
        stack.push(child);
      }
    }
    return list;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  /**
   * For testing purposes only.
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(value, children);
  }

  /**
   * For testing purposes only.
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MapNode<?, ?>)) {
      return false;
    }
    MapNode<?, ?> other = (MapNode<?, ?>) o;
    /*
     * This is the only place where we use equals for value comparison.
     * Everywhere else we use compareTo.
     */
    return value.equals(other.value) && Objects.equal(children, other.children);
  }
}
