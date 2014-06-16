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
import java.util.PriorityQueue;
import java.util.Queue;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.NodeList.Node;
import com.google.uzaygezen.core.ranges.Range;
import com.google.uzaygezen.core.ranges.RangeHome;

/**
 * Query builder that can be configured with a maximum number of filtered index
 * ranges. That is useful for data storage systems that are somewhat limited in
 * the number of ranges they can accept in a query. But the builder can also be
 * configured to never try to join together two filtered ranges, by specifying a
 * very high maximum number of ranges and that it should never try to combine
 * two ranges with vacuum in between. In the latter case the filters are
 * guaranteed not to be used at all, unless they are created by the region
 * inspector itself.
 * 
 * @author Daniel Aioanei
 * 
 * @param <T>
 *          filter type
 */
public class BacktrackingQueryBuilder<F, T, V extends Content<V>, R extends Range<T, V>> implements
  QueryBuilder<F, R> {

  private final RegionInspector<F, V> regionInspector;
  private final FilterCombiner<F, V, R> filterCombiner;
  private final int maxFilteredIndexRanges;
  private final boolean alwaysRemoveVacuum;

  private final RangeHome<T, V, R> rangeHome;
  private final V zero;

  /**
   * The gap between the last 2 ranges, if any; otherwise zero.
   */
  private V currentGap;

  /**
   * The last node sits only here and it doesn't have a correspondent in the
   * min-heap.
   */
  private final NodeList<FilteredIndexRange<F, R>> nodeList = LinkedNodeList.create();

  /**
   * Never exceeds {@link #maxFilteredIndexRanges} in size.
   */
  private final Queue<ComparableNode> minHeap = new PriorityQueue<ComparableNode>();

  /**
   * Used solely for safety checking that the finished orthotopes are passed in
   * increasing order and that none are skipped.
   */
  private Pow2LengthBitSetRange lastFinishedIndexRange = null;

  /**
   * There are two types of sources for over-selectivity: the filter combiner
   * and the region inspector. This field keeps track of whether we have come
   * across over-selectivity at least once.
   */
  private boolean potentialOverSelectivity;

  /**
   * Checks the relationship of the input orthotope against the query region and
   * if it is:
   * <ul>
   * <li>{@link SpatialRelation#DISJOINT}: Augments the current gap with the
   * othotope's estimated number of points.</li>
   * <li>{@link SpatialRelation#OVERLAPS}: Asks to see the children.</li>
   * <li>{@link SpatialRelation#COVERED}:
   * <ul>
   * <li>If the current gap is zero and {@link #alwaysRemoveVacuum} is
   * {@literal true}, then it joins the new index range to the previous one.</li>
   * <li>Otherwise it adds the current range to the internal min-heap of ranges,
   * with the current gap as the key. Iff the number of filtered ranges exceeds
   * as a result {@link #maxFilteredIndexRanges}, does it then combine together
   * the two ranges with the minimum gap between them. If multiple such
   * consecutive ranges exists, one of them will be picked.</li>
   * </ul>
   * </li>
   * </ul>
   * Implementation note: {@link PriorityQueue#remove} does not specify if the
   * removal of the least value is deterministic or not. Instead it only says
   * that ties are broken arbitrarily. If {@link PriorityQueue} is deterministic
   * on a specific platform, then this class is also deterministic on that
   * particular platform.
   */
  @Override
  public boolean visit(Pow2LengthBitSetRange indexRange, List<Pow2LengthBitSetRange> orthotope) {
    Preconditions.checkArgument(lastFinishedIndexRange == null
      || indexRange.getStart().compareTo(lastFinishedIndexRange.getStart()) > 0);
    /*
     * Even more than the previous check, we make sure that the new range starts
     * exactly where the previous finished one had its upper bound.
     */
    assert (lastFinishedIndexRange == null) == (indexRange.getStart().length() == 0);
    assert (lastFinishedIndexRange == null)
      || (rangeHome.toRange(lastFinishedIndexRange).getEnd().equals(rangeHome.toRange(indexRange).getStart())) : String.format(
      "lastFinishedIndexRange=%s indeRange=%s", lastFinishedIndexRange, indexRange);
    Assessment<F, V> assessment = regionInspector.assess(indexRange, orthotope);
    switch (assessment.getOutcome()) {
    case OVERLAPS:
      return true;
    case COVERED:
      processCoveredNode(
        indexRange, assessment.getFilter(), assessment.isPotentialOverSelectivity());
      potentialOverSelectivity |= assessment.isPotentialOverSelectivity();
      lastFinishedIndexRange = indexRange.clone();
      return false;
    case DISJOINT:
      processDisjointRegion(assessment.getEstimate());
      lastFinishedIndexRange = indexRange.clone();
      return false;
    default:
      throw new RuntimeException("Cannot be: " + assessment.getOutcome());
    }
  }

  private void processCoveredNode(
    Pow2LengthBitSetRange indexBitSetRange, F filter, boolean potentialOverSelectivityInRange) {
    R indexRange = rangeHome.toRange(indexBitSetRange);
    FilteredIndexRange<F, R> indexQueryRange = new FilteredIndexRange<F, R>(
      indexRange, filter, potentialOverSelectivityInRange);
    // Fetching the last node is a constant time operation.
    Node<FilteredIndexRange<F, R>> end = nodeList.isEmpty() ? null
      : nodeList.getNode(nodeList.size() - 1);
    if (alwaysRemoveVacuum & end != null & currentGap.isZero()) {
      SelectiveFilter<F> combinedFilter = filterCombiner.combine(end.get(), indexQueryRange, zero);
      end.set(new FilteredIndexRange<F, R>(
        rangeHome.of(end.get().getIndexRange().getStart(), indexRange.getEnd()),
        combinedFilter.getFilter(), combinedFilter.isPotentialOverSelectivity()
          | potentialOverSelectivityInRange));
      potentialOverSelectivity |= combinedFilter.isPotentialOverSelectivity();
    } else {
      Node<FilteredIndexRange<F, R>> node = nodeList.addAndGetNode(indexQueryRange);
      if (end != null) {
        end = node;
        minHeap.add(new ComparableNode(currentGap, node));
        // We also have one interval which is not in the heap.
        if (minHeap.size() >= maxFilteredIndexRanges) {
          ComparableNode removed = minHeap.remove();
          SelectiveFilter<F> combinedFilter = filterCombiner.combine(
            removed.node.previous().get(), removed.node.get(), removed.leftGapEstimate);
          potentialOverSelectivity |= combinedFilter.isPotentialOverSelectivity();
          removed.node.previous().set(
            new FilteredIndexRange<F, R>(
              rangeHome.of(
                removed.node.previous().get().getIndexRange().getStart(),
                removed.node.get().getIndexRange().getEnd()), combinedFilter.getFilter(),
              combinedFilter.isPotentialOverSelectivity() | potentialOverSelectivityInRange));
          removed.node.remove();
        }
      }
      currentGap = zero.clone();
    }
  }

  private void processDisjointRegion(V gapEstimate) {
    if (!nodeList.isEmpty()) {
      currentGap.add(gapEstimate);
    }
  }

  /**
   * Returns the query constructed so far as a random access list.
   */
  @Override
  public Query<F, R> get() {
    /*
     * Can't return nodeList directly since it will be modified at later steps
     * if any left, and also because we want to return a random access list.
     */
    return Query.of(ImmutableList.copyOf(nodeList));
  }

  public static <F, T, V extends Content<V>, R extends Range<T, V>> BacktrackingQueryBuilder<F, T, V, R> create(
    RegionInspector<F, V> regionInspector, FilterCombiner<F, V, R> intervalCombiner,
    int maxFilteredIndexRanges, boolean removeVacuum, RangeHome<T, V, R> rangeHome, V zero) {
    return new BacktrackingQueryBuilder<F, T, V, R>(
      regionInspector, intervalCombiner, maxFilteredIndexRanges, removeVacuum, rangeHome, zero);
  }

  public BacktrackingQueryBuilder(RegionInspector<F, V> regionInspector,
    FilterCombiner<F, V, R> intervalCombiner, int maxFilteredIndexRanges,
    boolean alwaysRemoveVacuum, RangeHome<T, V, R> rangeHome, V zero) {
    this.regionInspector = regionInspector;
    this.filterCombiner = intervalCombiner;
    Preconditions.checkArgument(
      maxFilteredIndexRanges > 0, "maxFilteredIndexRanges must be positive");
    this.maxFilteredIndexRanges = maxFilteredIndexRanges;
    this.alwaysRemoveVacuum = alwaysRemoveVacuum;
    this.rangeHome = rangeHome;
    this.zero = zero;
    this.currentGap = zero.clone();
  }

  private class ComparableNode implements Comparable<ComparableNode> {

    private final V leftGapEstimate;
    private final Node<FilteredIndexRange<F, R>> node;

    public ComparableNode(V leftGapEstimate, Node<FilteredIndexRange<F, R>> node) {
      // Preconditions.checkArgument(leftGapEstimate >= 0);
      this.leftGapEstimate = leftGapEstimate;
      this.node = Preconditions.checkNotNull(node, "node");
    }

    @Override
    public int compareTo(ComparableNode other) {
      return leftGapEstimate.compareTo(other.leftGapEstimate);
    }
  }
}
