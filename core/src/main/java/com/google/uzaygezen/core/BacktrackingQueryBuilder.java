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
import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.NodeList.Node;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Query builder that can be configured with a maximum number of filtered index
 * ranges. That is useful for data storage systems that are somewhat limited in
 * the number of ranges they can accept in a query. But the builder can also be
 * configured to never try to join together two filtered ranges, by specifying
 * a very high maximum number of ranges and that it should never try to combine
 * two ranges with vacuum in between. In the latter case the filters a
 * guaranteed not to be used at all, unless they are created by the region
 * inspector itself.
 * 
 * @author Daniel Aioanei
 * 
 * @param <T> filter type
 */
public class BacktrackingQueryBuilder<T> implements QueryBuilder<T> {

  private final RegionInspector<T> regionInspector;
  private final FilterCombiner<T> filterCombiner;
  private final int maxFilteredIndexRanges;
  private final boolean alwaysRemoveVacuum;
  
  /**
   * The gap between the last 2 ranges, if any; otherwise zero.
   */
  private long currentGap = 0;
  
  /**
   * The last node sits only here and it doesn't have a correspondent in the
   * min-heap.
   */
  private final NodeList<FilteredIndexRange<T>> nodeList = LinkedNodeList.create();

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
   *  <li>{@link SpatialRelation#DISJOINT}: Augments the current gap with the
   *  othotope's estimated number of points.</li>
   *  <li>{@link SpatialRelation#OVERLAPS}: Asks to see the children.</li>
   *  <li>{@link SpatialRelation#COVERED}:
   *   <ul>
   *    <li>If the current gap is zero and {@link #alwaysRemoveVacuum} is
   *    {@literal true}, then it joins the new index range to the previous one.
   *    </li>
   *    <li>Otherwise it adds the current range to the internal min-heap of
   *    ranges, with the current gap as the key. Iff the number of filtered
   *    ranges exceeds as a result {@link #maxFilteredIndexRanges}, does it
   *    then combine together the two ranges with the minimum gap between them.
   *    If multiple such consecutive ranges exists, one of them will be picked.
   *    </li>
   *   </ul>
   *  </li>
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
    assert (lastFinishedIndexRange == null) || (lastFinishedIndexRange.toLongRange().getEnd() ==
        indexRange.toLongRange().getStart()) : String.format(
            "lastFinishedIndexRange=%s indeRange=%s", lastFinishedIndexRange, indexRange);
    Assessment<T> assessment = regionInspector.assess(indexRange, orthotope);
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
      Pow2LengthBitSetRange indexRange, T filter, boolean potentialOverSelectivityInRange) {
    LongRange indexLongRange = indexRange.toLongRange();
    FilteredIndexRange<T> indexQueryRange =
        new FilteredIndexRange<T>(indexLongRange, filter, potentialOverSelectivityInRange);
    // Fetching the last node is a constant time operation.
    Node<FilteredIndexRange<T>> end =
          nodeList.isEmpty() ? null : nodeList.getNode(nodeList.size() - 1);
    if (alwaysRemoveVacuum & end != null & currentGap == 0) {
      SelectiveFilter<T> combinedFilter = filterCombiner.combine(end.get(), indexQueryRange, 0);
      end.set(new FilteredIndexRange<T>(LongRange.of(end.get().getIndexRange().getStart(),
          indexLongRange.getEnd()), combinedFilter.getFilter(),
          combinedFilter.isPotentialOverSelectivity() | potentialOverSelectivityInRange));
      potentialOverSelectivity |= combinedFilter.isPotentialOverSelectivity();
    } else {
      Node<FilteredIndexRange<T>> node = nodeList.addAndGetNode(indexQueryRange);
      if (end != null) {
        end = node;
        minHeap.add(new ComparableNode(currentGap, node));
        // We also have one interval which is not in the heap.
        if (minHeap.size() >= maxFilteredIndexRanges) {
          ComparableNode removed = minHeap.remove();
          SelectiveFilter<T> combinedFilter = filterCombiner.combine(removed.node.previous().get(),
              removed.node.get(), removed.leftGapEstimate);
          potentialOverSelectivity |= combinedFilter.isPotentialOverSelectivity();
          removed.node.previous().set(new FilteredIndexRange<T>(
              LongRange.of(removed.node.previous().get().getIndexRange().getStart(),
                  removed.node.get().getIndexRange().getEnd()), combinedFilter.getFilter(),
                  combinedFilter.isPotentialOverSelectivity() | potentialOverSelectivityInRange));
          removed.node.remove();
        }
      }
      currentGap = 0;
    }
  }

  private void processDisjointRegion(long gapEstimate) {
    if (!nodeList.isEmpty()) {
      currentGap += gapEstimate;
    }
  }

  /**
   * Returns the query constructed so far as a random access list.
   */
  @Override
  public Query<T> get() {
    /*
     * Can't return nodeList directly since it will be modified at later steps
     * if any left, and also because we want to return a random access list.
     */
    return Query.of(ImmutableList.copyOf(nodeList));
  }

  public static <T> BacktrackingQueryBuilder<T> create(
      RegionInspector<T> regionInspector, FilterCombiner<T> intervalCombiner,
      int maxFilteredIndexRanges, boolean removeVacuum) {
    return new BacktrackingQueryBuilder<T>(
        regionInspector, intervalCombiner, maxFilteredIndexRanges, removeVacuum);
  }
  
  public BacktrackingQueryBuilder(RegionInspector<T> regionInspector,
      FilterCombiner<T> intervalCombiner, int maxFilteredIndexRanges, boolean alwaysRemoveVacuum) {
    this.regionInspector = regionInspector;
    this.filterCombiner = intervalCombiner;
    Preconditions.checkArgument(
        maxFilteredIndexRanges > 0, "maxFilteredIndexRanges must be positive");
    this.maxFilteredIndexRanges = maxFilteredIndexRanges;
    this.alwaysRemoveVacuum = alwaysRemoveVacuum;
  }
  
  private class ComparableNode implements Comparable<ComparableNode> {
    
    private final long leftGapEstimate;
    private final Node<FilteredIndexRange<T>> node;
    
    public ComparableNode(long leftGapEstimate, Node<FilteredIndexRange<T>> node) {
      Preconditions.checkArgument(leftGapEstimate >= 0);
      this.leftGapEstimate = leftGapEstimate;
      this.node = Preconditions.checkNotNull(node, "node");
    }

    @Override
    public int compareTo(ComparableNode other) {
      // Works since both numbers are non-negative.
      return Long.signum(leftGapEstimate - other.leftGapEstimate);
    }
  }
}
