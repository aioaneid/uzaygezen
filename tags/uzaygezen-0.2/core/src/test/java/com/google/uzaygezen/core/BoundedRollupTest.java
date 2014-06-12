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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.uzaygezen.core.TestUtils.IntArrayCallback;
import com.google.uzaygezen.core.TestUtils.IntArrayComparator;

/**
 * @author Daniel Aioanei
 */
public class BoundedRollupTest {

  @Test
  public void empty() {
    BoundedRollup<Integer, LongContent> rollup =
        BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
    Assert.assertNull(rollup.finish());
  }

  @Test
  public void rootOnly() {
    BoundedRollup<Integer, LongContent> rollup =
        BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
    rollup.feedRow(Iterators.<Integer>emptyIterator(), TestUtils.ONE_LONG_CONTENT);
    MapNode<Integer, LongContent> actual = rollup.finish();
    MapNode<Integer, LongContent> expected = leaf();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void rootOnlyAddition() {
    BoundedRollup<Integer, LongContent> rollup =
        BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
    int n = 10;
    for (int i = 0; i < n; ++i) {
      rollup.feedRow(Iterators.<Integer>emptyIterator(), TestUtils.ONE_LONG_CONTENT);
    }
    MapNode<Integer, LongContent> actual = rollup.finish();
    MapNode<Integer, LongContent> expected = leaf(n);
    Assert.assertEquals(expected, actual);
  }

  private MapNode<Integer, LongContent> leaf() {
    MapNode<Integer, LongContent> expected = MapNode.create(
        TestUtils.ONE_LONG_CONTENT, ImmutableMap.<Integer, MapNode<Integer, LongContent>>of());
    return expected;
  }

  private MapNode<Integer, LongContent> leaf(int count) {
    MapNode<Integer, LongContent> expected = MapNode.create(
        newCountingArray(count), ImmutableMap.<Integer, MapNode<Integer, LongContent>>of());
    return expected;
  }

  @Test
  public void oneRealNode() {
    BoundedRollup<Integer, LongContent> rollup =
        BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
    rollup.feedRow(ImmutableList.of(4).iterator(), TestUtils.ONE_LONG_CONTENT);
    MapNode<Integer, LongContent> actual = rollup.finish();
    MapNode<Integer, LongContent> expected = MapNode.create(TestUtils.ONE_LONG_CONTENT,
        ImmutableMap.<Integer, MapNode<Integer, LongContent>>of(4, leaf()));
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void twinPairAtLevel1() {
    BoundedRollup<Integer, LongContent> rollup =
        BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
    rollup.feedRow(ImmutableList.of(5).iterator(), TestUtils.ONE_LONG_CONTENT);
    rollup.feedRow(ImmutableList.of(10).iterator(), TestUtils.ONE_LONG_CONTENT);
    MapNode<Integer, LongContent> actual = rollup.finish();
    MapNode<Integer, LongContent> expected = MapNode.create(newCountingArray(2),
        ImmutableMap.<Integer, MapNode<Integer, LongContent>>of(10, leaf(), 5, leaf()));
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void twinPairAtLevel1RepeatLastNode() {
    BoundedRollup<Integer, LongContent> rollup =
        BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
    rollup.feedRow(ImmutableList.of(5).iterator(), TestUtils.ONE_LONG_CONTENT);
    rollup.feedRow(ImmutableList.of(10).iterator(), TestUtils.ONE_LONG_CONTENT);
    rollup.feedRow(ImmutableList.of(10).iterator(), TestUtils.ONE_LONG_CONTENT);
    MapNode<Integer, LongContent> actual = rollup.finish();
    MapNode<Integer, LongContent> expected = MapNode.create(newCountingArray(3),
        ImmutableMap.<Integer, MapNode<Integer, LongContent>>of(10, leaf(2), 5, leaf()));
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void twinPairAtLevel1ButGoingBackToFirstChildFails() {
    BoundedRollup<Integer, LongContent> rollup =
        BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
    rollup.feedRow(ImmutableList.of(5).iterator(), TestUtils.ONE_LONG_CONTENT);
    rollup.feedRow(ImmutableList.of(10).iterator(), TestUtils.ONE_LONG_CONTENT);
    try {
      rollup.feedRow(ImmutableList.of(5).iterator(), TestUtils.ONE_LONG_CONTENT);
      Assert.fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException ex) {
      MoreAsserts.assertContainsRegex("Node already there.", ex.getMessage());
    }
  }

  @Test
  public void twinPairAtLevel2() {
    BoundedRollup<Integer, LongContent> rollup =
        BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
    rollup.feedRow(ImmutableList.of(100, 5).iterator(), TestUtils.ONE_LONG_CONTENT);
    rollup.feedRow(ImmutableList.of(100, 10).iterator(), TestUtils.ONE_LONG_CONTENT);
    MapNode<Integer, LongContent> actual = rollup.finish();
    MapNode<Integer, LongContent> expected = MapNode.create(newCountingArray(2),
        ImmutableMap.<Integer, MapNode<Integer, LongContent>>of(
            100, MapNode.create(newCountingArray(2), ImmutableMap
                .<Integer, MapNode<Integer, LongContent>>of(10, leaf(), 5, leaf()))));
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void nodeAndLeafCount() {
    final List<int[]> list = Lists.newArrayList();
    IntArrayCallback callback = new ListCollector(list);
    for (int n = 0; n < 8; ++n) {
      list.clear();
      // Exact sum means that no array will be a prefix of another one.
      TestUtils.generateSpecWithExactSum(n, 2 * n, callback);
      Collections.sort(list, IntArrayComparator.INSTANCE);
      BoundedRollup<Integer, LongContent> rollup =
          BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
      MapNode<Integer, LongContent> actual = createTree(list, rollup);
      checkTreeIsComplete(list, n, actual);
    }
  }

  private void checkTreeIsComplete(final List<int[]> list, int n,
      MapNode<Integer, LongContent> actual) {
    Assert.assertEquals(list.size(), actual.getValue().value());
    int[] subtreeSizeAndLeafCount = actual.subtreeSizeAndLeafCount();
    Assert.assertEquals(n == 0 ? 1 : 2 * list.size(), subtreeSizeAndLeafCount[0]);
    Assert.assertEquals(list.size(), subtreeSizeAndLeafCount[1]);
  }

  @Test
  public void oneNodeAtMost() {
    final List<int[]> list = Lists.newArrayList();
    IntArrayCallback callback = new ListCollector(list);
    for (int n = 0; n < 8; ++n) {
      list.clear();
      // Exact sum means that no array will be a prefix of another one.
      TestUtils.generateSpecWithExactSum(n, 2 * n, callback);
      Collections.sort(list, IntArrayComparator.INSTANCE);
      BoundedRollup<Integer, LongContent> rollup =
          BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, 1);
      MapNode<Integer, LongContent> actual = createTree(list, rollup);
      Assert.assertEquals(MapNode.create(new LongContent(list.size()),
          ImmutableMap.<Integer, MapNode<Integer, LongContent>>of()), actual);
    }
  }

  @Test
  public void subtreeIsOptimalWithinConstraints() {
    final List<int[]> list = Lists.newArrayList();
    IntArrayCallback callback = new ListCollector(list);
    for (int n = 0; n < 3; ++n) {
      list.clear();
      // Exact sum means that no array will be a prefix of another one.
      TestUtils.generateSpecWithExactSum(n, 2 * n, callback);
      Collections.sort(list, IntArrayComparator.INSTANCE);
      BoundedRollup<Integer, LongContent> rollup =
          BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, Integer.MAX_VALUE);
      MapNode<Integer, LongContent> fullTreeRoot = createTree(list, rollup);
      checkTreeIsComplete(list, n, fullTreeRoot);
      int fullTreeSize = fullTreeRoot.subtreeSizeAndLeafCount()[0];
      for (int i = 1; i <= fullTreeSize + 5; ++i) {
        List<List<MapNode<Integer, LongContent>>> allPossibleExpansions =
            allPossibleExpansions(fullTreeRoot, i);
        BoundedRollup<Integer, LongContent> constrainedRollup =
            BoundedRollup.create(TestUtils.ZERO_LONG_CONTENT, i);
        MapNode<Integer, LongContent> constrainedTreeRoot =
            createTree(list, constrainedRollup);
        List<MapNode<Integer, LongContent>> actual = constrainedTreeRoot.preorder();
        List<List<MapNode<Integer, LongContent>>> all =
            toSortedValueListList(allPossibleExpansions);
        List<MapNode<Integer, LongContent>> constrained = toSortedValueList(actual);
        List<List<MapNode<Integer, LongContent>>> allSameSize = Lists.newArrayList();
        for (List<MapNode<Integer, LongContent>> row : all) {
          Assert.assertTrue(row.size() <= constrained.size());
          if (row.size() == constrained.size()) {
            allSameSize.add(row);
          }
        }
        Assert.assertEquals(Collections.max(allSameSize,
            new ListComparator<MapNode<Integer, LongContent>>(
                new MapNodeValueComparator<Integer, LongContent>())), constrained);
      }
    }
  }
  
  private MapNode<Integer, LongContent> createTree(
      final List<int[]> list, BoundedRollup<Integer, LongContent> rollup) {
    for (int[] array : list) {
      rollup.feedRow(Ints.asList(array).iterator(), TestUtils.ONE_LONG_CONTENT);
    }
    MapNode<Integer, LongContent> root = rollup.finish();
    return root;
  }
  
  /**
   * Test the test helper method {@link #allPossibleExpansions}.
   */
  @Test
  public void allPossibleExpansionsForRootOnly() {
    MapNode<Integer, String> root =
        MapNode.create("x", ImmutableMap.<Integer, MapNode<Integer, String>>of());
    Assert.assertTrue(allPossibleExpansions(root, 0).isEmpty());
    for (int i = 1; i < 3; ++i) {
      MoreAsserts.assertContentsAnyOrder(allPossibleExpansions(root, i), ImmutableList.of(root));
    }
  }
  
  /**
   * Test the test helper method {@link #allPossibleExpansions}.
   */
  @Test
  public void allPossibleExpansionsForThreeNodes() {
    MapNode<Integer, String> rightGrandchild =
        MapNode.create("d", ImmutableMap.<Integer, MapNode<Integer, String>>of());
    MapNode<Integer, String> rightChild =
        MapNode.create("c", ImmutableMap.<Integer, MapNode<Integer, String>>of(2, rightGrandchild));
    MapNode<Integer, String> leftChild =
        MapNode.create("b", ImmutableMap.<Integer, MapNode<Integer, String>>of());
    MapNode<Integer, String> root = MapNode.create(
        "a", ImmutableMap.<Integer, MapNode<Integer, String>>of(0, leftChild, 1, rightChild));
    Assert.assertTrue(allPossibleExpansions(root, 0).isEmpty());
    MoreAsserts.assertContentsAnyOrder(allPossibleExpansions(root, 1), ImmutableList.of(root));
    MoreAsserts.assertContentsAnyOrder(allPossibleExpansions(root, 2), ImmutableList.of(root));
    MoreAsserts.assertContentsAnyOrder(toIdentitySetList(allPossibleExpansions(root, 3)),
        toIdentitySet(ImmutableList.of(root)),
        toIdentitySet(ImmutableList.of(root, leftChild, rightChild)));
    List<Set<MapNode<Integer, String>>> actual = toIdentitySetList(allPossibleExpansions(root, 4));
    MoreAsserts.assertContentsAnyOrder(actual, toIdentitySet(ImmutableList.of(root)),
        toIdentitySet(ImmutableList.of(root, leftChild, rightChild)),
        toIdentitySet(ImmutableList.of(root, leftChild, rightChild, rightGrandchild)));
  }

  private static <K, V> List<Set<MapNode<K, V>>> toIdentitySetList(
      List<List<MapNode<K, V>>> listList) {
    List<Set<MapNode<K, V>>> setList = new ArrayList<Set<MapNode<K, V>>>(listList.size());
    for (List<MapNode<K, V>> list : listList) {
      Set<MapNode<K, V>> set = toIdentitySet(list);
      setList.add(set);
    }
    return setList;
  }

  private static <V, K> Set<MapNode<K, V>> toIdentitySet(List<MapNode<K, V>> list) {
    Set<MapNode<K, V>> set =
        Collections.newSetFromMap(new IdentityHashMap<MapNode<K, V>, Boolean>());
    set.addAll(list);
    Assert.assertEquals(list.size(), set.size());
    return set;
  }

  private static <K, V extends Comparable<V>> List<List<MapNode<K, V>>> toSortedValueListList(
      List<List<MapNode<K, V>>> listList) {
    List<List<MapNode<K, V>>> result = new ArrayList<List<MapNode<K, V>>>(listList.size());
    for (List<MapNode<K, V>> list : listList) {
      List<MapNode<K, V>> set = toSortedValueList(list);
      result.add(set);
    }
    return result;
  }

  private static <K, V extends Comparable<V>>
      List<MapNode<K, V>> toSortedValueList(List<MapNode<K, V>> list) {
    List<MapNode<K, V>> result = new ArrayList<MapNode<K, V>>(list.size());
    for (MapNode<K, V> node : list) {
      result.add(MapNode.create(node.getValue(), ImmutableMap.<K, MapNode<K, V>>of()));
    }
    Collections.sort(result, new MapNodeValueComparator<K, V>());
    return result;
  }
  
  private static class MapNodeValueComparator<K, V extends Comparable<V>>
      implements Comparator<MapNode<K, V>> {
    @Override
    public int compare(MapNode<K, V> o1, MapNode<K, V> o2) {
      return o1.getValue().compareTo(o2.getValue());
    }
  }

  private static <K, V> List<List<MapNode<K, V>>> allPossibleExpansions(
      final MapNode<K, V> node, int maxNodes) {
    List<List<MapNode<K, V>>> result = Lists.newArrayList();
    for (int i = 1; i <= maxNodes; ++i) {
      List<List<MapNode<K, V>>> expansions = allExpansionsWithExactNodeCount(node, i);
      result.addAll(expansions);
    }
    return result;
  }

  private static <K, V> List<List<MapNode<K, V>>> allExpansionsWithExactNodeCount(
      final MapNode<K, V> node, final int exactNodes) {
    if (exactNodes == 1) {
      return ImmutableList.of((List<MapNode<K, V>>) ImmutableList.of(node));
    }
    List<List<MapNode<K, V>>> result = Lists.newArrayList();
    assert exactNodes > 1;
    if (exactNodes > node.getChildren().size()) {
      final List<MapNode<K, V>> children = ImmutableList.copyOf(node.getChildren().values());
      final List<List<List<List<MapNode<K, V>>>>> childrenExpansions = Lists.newArrayList();
      IntArrayCallback callback = new IntArrayCallback() {
        @Override
        public void call(int[] m) {
          assert m.length <= children.size();
          if (m.length == children.size()) {
            List<List<List<MapNode<K, V>>>> expansionSet = Lists.newArrayList();
            for (int i = 0; i < m.length; ++i) {
              List<List<MapNode<K, V>>> childExpansions =
                  allExpansionsWithExactNodeCount(children.get(i), m[i] + 1);
              for (List<MapNode<K, V>> childExpansion : childExpansions) {
                Assert.assertEquals(m[i] + 1, childExpansion.size());
              }
              if (childExpansions.isEmpty()) {
                return;
              } else {
                expansionSet.add(childExpansions);
              }
            }
            Assert.assertEquals(m.length, expansionSet.size());
            childrenExpansions.add(expansionSet);
          }
        }
      };
      TestUtils.generateSpecWithExactSum(
          node.getChildren().size(), exactNodes - 1 - children.size(), callback);
      int[] childOffset = new int[node.getChildren().size()];
      for (List<List<List<MapNode<K, V>>>> childrenExpansion : childrenExpansions) {
        assert childrenExpansion.size() == children.size();
        Arrays.fill(childOffset, -1);
        int k = 0;
        while (k >= 0) {
          if (k == childrenExpansion.size()) {
            List<MapNode<K, V>> expansion = Lists.newArrayList();
            for (int i = 0; i < k; ++i) {
              expansion.addAll(childrenExpansion.get(i).get(childOffset[i]));
            }
            assert expansion.size() == exactNodes - 1;
            expansion.add(node);
            result.add(expansion);
            k--;
          } else if (childOffset[k] < childrenExpansion.get(k).size() - 1) {
            childOffset[k++]++;
          } else {
            childOffset[k--] = -1;
          }
        }
      }
    }
    for (List<MapNode<K, V>> list : result) {
      Assert.assertEquals(exactNodes, list.size());
    }
    return result;
  }

  private LongContent newCountingArray(long count) {
    return new LongContent(count);
  }
  
  /**
   * Will put all array callbacks which don't have zeroes into the supplied
   * list.
   */
  private static class ListCollector implements IntArrayCallback {
    
    private final List<int[]> list;
    
    public ListCollector(List<int[]> list) {
      this.list = list;
    }
    
    @Override
    public void call(int[] m) {
      if (!ArrayUtils.contains(m, 0)) {
        list.add(Arrays.copyOf(m, m.length));
      }
    }
  }

  private static class ListComparator<T> implements Comparator<Iterable<T>> {

    private final Comparator<T> delegate;
    
    public ListComparator(Comparator<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public int compare(Iterable<T> o1, Iterable<T> o2) {
      Iterator<T> it1 = o1.iterator();
      Iterator<T> it2 = o2.iterator();
      int result = 0;
      while (it1.hasNext()) {
        if (!it2.hasNext()) {
          result = +1;
          break;
        }
        T t1 = it1.next();
        T t2 = it2.next();
        int cmp = delegate.compare(t1, t2);
        if (cmp != 0) {
          result = cmp;
          break;
        }
      }
      if (result == 0) {
        result = it2.hasNext() ? -1 : 0;
      }
      return result;
    }
  }
}
