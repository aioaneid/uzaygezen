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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;

/**
 * Streaming rollup implementation that supports an upper bound on the number of
 * nodes, upper bound which is achieved by rolling up nodes are required. The
 * resulting tree has the property that if one were to order the nodes of a tree
 * by the additive value inside, and do that for all possible root-sharing
 * subtrees of size exactly {@code min(maxNodes, nodeCount)}, and then order
 * lexicographically all such trees, the one created by this class will be
 * maximal, with ties broken arbitrarily.
 * <p>
 * Implementation notes: At every step only leaves are rolled up, and more
 * precisely, those leaves which share a common parent with the property that it
 * has the smallest value amongst all finished parent nodes which have only
 * leaf children.
 * </p>
 * 
 * @author Daniel Aioanei
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class BoundedRollup<K, V extends AdditiveValue<V>> implements StreamingRollup<K, V> {

  /**
   * An empty value prototype.
   */
  private final V emptyValue;
  private int nodeCount;
  
  /**
   * The maximum number of nodes allowed.
   */
  private final int maxNodes;

  /**
   * Has all finished nodes that are not leaves, and have all children as
   * leaves.
   */
  private final PriorityQueue<ComparableTreeNode> minHeap;
  
  /**
   * Initially it is {@literal null}.
   */
  private TreeNode root;
  
  /**
   * Contains the path most recently visited.
   */
  private final Deque<TreeNode> rightmostPath = new ArrayDeque<TreeNode>();
  
  private boolean finished;
  
  public static <K, V extends AdditiveValue<V>> BoundedRollup<K, V> create(
      V emptyValue, int maxNodes) {
    return new BoundedRollup<K, V>(emptyValue, maxNodes);
  }
  
  private BoundedRollup(V emptyValue, int maxNodes) {
    Preconditions.checkArgument(maxNodes > 0, "maxNodes must be positive");
    this.maxNodes = maxNodes;
    this.emptyValue = emptyValue.clone();
    this.minHeap = new PriorityQueue<ComparableTreeNode>();
  }

  @Override
  public void feedRow(Iterator<K> leafPath, V v) {
    checkNotFinished();
    Preconditions.checkArgument(emptyValue.compareTo(v) < 0, "v must not be zero.");
    if (root == null) {
      firstTime(leafPath, v);
    } else {
      assert rightmostPath.peek() == root;
      Iterator<TreeNode> rightmostPathIterator = rightmostPath.iterator();
      TreeNode node = root;
      root.value.add(v);
      TreeNode rightmostNode = rightmostPathIterator.next();
      K key = null;
      while (leafPath.hasNext()) {
        key = leafPath.next();
        if (node.children == null) {
          // Sealed node.
          node.value.add(v);
          rightmostNode = node;
          break;
        } else {
          TreeNode child = node.children.get(key);
          rightmostNode = rightmostPathIterator.next();
          if (child != rightmostNode) {
            break;
          }
          child.value.add(v);
          node = child;
        }
      }
      if (node != rightmostNode) {
        assert node == rightmostNode.parent && key != null;
        removeTail(rightmostPathIterator, rightmostNode);
        TreeNode child =
            new TreeNode(key, node, v.clone(), leafPath.hasNext() ? createChildrenMap() : null);
        rightmostPath.addLast(child);
        node.children.put(key, child);
        node = child;
        completeNewPath(leafPath, node, v);
      } else {
        Preconditions.checkState(!rightmostPathIterator.hasNext());
        // nothing else to do
      }
    }
    fixSizeIfNeededAndPossible();
  }

  /**
   * As long as we have too many nodes and there is at least one finished parent
   * node whose children are all leaves, it keeps rolling up.
   */
  private void fixSizeIfNeededAndPossible() {
    while (maxNodes < nodeCount) {
      ComparableTreeNode minNode = minHeap.poll();
      if (minNode == null) {
        // min-heap exhausted
        break;
      }
      minNode.node.removeChildrenAndSeal();
      /*
       * If we were this was the only non-leaf child of the parent, and the
       * parent has been finished processing, then add the parent to minHeap.
       */
      if (minNode.node.parent != null
          && minNode.node.parent.allChildrenOfThisInternalNodeAreLeaves()
          && !(rightmostPath.contains(minNode.node.parent))) {
        minHeap.add(new ComparableTreeNode(minNode.node.parent));
      }
    }
  }

  /**
   * Removes all the remaining elements of the iterator, including the current
   * element.
   */
  private void removeTail(Iterator<TreeNode> iterator, TreeNode currentNode) {
    TreeNode previousToLast = iterator.hasNext() ? currentNode : null;
    iterator.remove();
    while (iterator.hasNext()) {
      TreeNode node = iterator.next();
      if (iterator.hasNext()) {
        previousToLast = node;
      }
      iterator.remove();
    }
    if (previousToLast != null && previousToLast.allChildrenOfThisInternalNodeAreLeaves()) {
      Preconditions.checkState(minHeap.offer(new ComparableTreeNode(previousToLast)));
    }
  }

  private void firstTime(Iterator<K> leafPath, V v) {
    assert rightmostPath.isEmpty();
    root = new TreeNode(v.clone(), leafPath.hasNext() ? createChildrenMap() : null);
    rightmostPath.addLast(root);
    TreeNode node = root;
    while (leafPath.hasNext()) {
      K key = leafPath.next();
      final TreeNode child;
      if (leafPath.hasNext()) {
        child = new TreeNode(key, node, v.clone(), createChildrenMap());
      } else {
        child = new TreeNode(key, node, v.clone(), null);
      }
      rightmostPath.addLast(child);
      node = child;
    }
    Preconditions.checkState(nodeCount == rightmostPath.size());
  }

  /**
   * Creates one new node for each remaining element in {@code leafPath}.
   * @param leafPath
   * @param node the parent of the first new node to be created
   * @param v the value being added
   */
  private void completeNewPath(Iterator<K> leafPath, TreeNode node, V v) {
    K key;
    while (leafPath.hasNext()) {
      key = leafPath.next();
      TreeNode freshChild =
          new TreeNode(key, node, v.clone(), leafPath.hasNext() ? createChildrenMap() : null);
      rightmostPath.addLast(freshChild);
      node.children.put(key, freshChild);
      node = freshChild;
    }
  }

  private void checkNotFinished() {
    Preconditions.checkState(!finished, "finished() has been called already");
  }

  private HashMap<K, TreeNode> createChildrenMap() {
    return Maps.<K, TreeNode>newHashMap();
  }

  @Override
  public MapNode<K, V> finish() {
    checkNotFinished();
    if (root == null) {
      return null;
    }
    Iterator<TreeNode> rightmostPathIterator = rightmostPath.iterator();
    // Consume the root.
    Preconditions.checkState(rightmostPathIterator.next() == root);
    removeTail(rightmostPathIterator, root);
    Preconditions.checkState(rightmostPath.isEmpty());
    // Now that rightmostPath has been fully cleared we can fix the size.
    fixSizeIfNeededAndPossible();
    // Clear some memory.
    minHeap.clear();
    /*
     * Since there are no more pending nodes, we are guaranteed to be able to
     * achieve the maximum node count criterion.
     */
    Preconditions.checkState(nodeCount <= maxNodes,
        "nodeCount=%s should be less than or equal to maxNodes=%s", nodeCount, maxNodes);
    MapNode<K, V> mapNode = moveNode(root);
    // The rest of the tree is already clear so clear the root for consistency.
    root = null;
    finished = true;
    return mapNode;
  }
  
  /**
   * Creates a {@link MapNode} from a {@link TreeNode}. It handles the
   * discrepancy in leaf representation between the two kinds of nodes: while
   * {@link MapNode} represents leaves as nodes with an empty {@link
   * MapNode#getChildren()} collection, {@link TreeNode} represents them as
   * nodes with a {@literal null} children collection. As a side effect it also
   * clears the tree so that the total amount of memory at any time is reduced.
   */
  private MapNode<K, V> moveNode(TreeNode subtree) {
    assert subtree != null;
    final MapNode<K, V> mapNode;
    if (subtree.children == null) {
      mapNode = MapNode.create(subtree.value, ImmutableMap.<K, MapNode<K, V>>of());
    } else {
      Map<K, MapNode<K, V>> children = Maps.newHashMapWithExpectedSize(subtree.children.size());
      for (Iterator<Entry<K, TreeNode>> iterator = subtree.children.entrySet().iterator();
          iterator.hasNext(); ) {
        Entry<K, TreeNode> entry = iterator.next();
        MapNode<K, V> old = children.put(entry.getKey(), moveNode(entry.getValue()));
        Preconditions.checkState(old == null);
        iterator.remove();
      }
      mapNode = MapNode.create(subtree.value, children);
    }
    return mapNode;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  /**
   * Internal tree node data structure. 
   */
  class TreeNode {
    
    final V value;
    
    /**
     * Null for children when the node is a leaf. The reason is that we want
     * to fail early with NPE in case because of a programming error resulting
     * in a node being inserted under a leaf.
     */
    Map<K, TreeNode> children;
    
    /**
     * Node only for the root.
     */
    final TreeNode parent;
    
    /**
     * Creates a root node.
     * @param value the data
     * @param children the children collection
     */
    public TreeNode(V value, Map<K, TreeNode> children) {
      parent = null;
      this.value = Preconditions.checkNotNull(value);
      this.children = children;
      nodeCount++;
    }

    public boolean allChildrenOfThisInternalNodeAreLeaves() {
      Preconditions.checkState(children != null & !children.isEmpty());
      for (TreeNode child : children.values()) {
        if (child.children != null) {
          return false;
        }
      }
      return true;
    }

    /**
     * @param key the key under which to hang the new node under its parent
     * @param parent the parent node; it is null for the root
     * @param value the data
     * @param children collection of children. Leaves have a null collection.
     */
    public TreeNode(K key, TreeNode parent, V value, Map<K, TreeNode> children) {
      this.parent = Preconditions.checkNotNull(parent);
      this.value = Preconditions.checkNotNull(value);
      this.children = children;
      TreeNode old = parent.children.put(key, this);
      Preconditions.checkArgument(old == null, "Node already there.");
      nodeCount++;
    }
    
    /**
     * Makes this node a leaf if it was an internal node before. Otherwise fail.
     */
    public void removeChildrenAndSeal() {
      Preconditions.checkState(!children.isEmpty());
      nodeCount -= children.size();
      children = null;
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      appendStringInto(sb, 0);
      return sb.toString();
    }

    private void appendStringInto(StringBuilder sb, int level) {
      sb.append('\n');
      for (int i = 0; i < level; ++i) {
        sb.append(' ');
      }
      sb.append(value);
      if (children != null) {
        for (Entry<K, TreeNode> entry : children.entrySet()) {
          sb.append('\n');
          for (int i = 0; i < level; ++i) {
            sb.append(' ');
          }
          sb.append("key=");
          sb.append(entry.getKey());
          sb.append('\n');
          for (int i = 0; i < level; ++i) {
            sb.append(' ');
          }
          sb.append("child=");
          entry.getValue().appendStringInto(sb, level + 1);
        }
      }
    }
  }
  
  /**
   * This comparator is <em>inconsistent with equals</em>. It only compares the
   * value inside each node.
   */
  class ComparableTreeNode implements Comparable<ComparableTreeNode> {

    private TreeNode node;
    
    public ComparableTreeNode(TreeNode node) {
      Preconditions.checkArgument(
          !node.children.isEmpty(), "Leaves are not to be put in the heap.");
      this.node = Preconditions.checkNotNull(node);
    }

    @Override
    public int compareTo(ComparableTreeNode o) {
      return node.value.compareTo(o.node.value);
    }
    
    public void swap(ComparableTreeNode other) {
      if (other != this) {
        // Values can be different, but they must compare equal.
        Preconditions.checkArgument(node.value.compareTo(other.node.value) == 0);
        TreeNode tmp = node;
        node = other.node;
        other.node = tmp;
      }
    }
  }
}
