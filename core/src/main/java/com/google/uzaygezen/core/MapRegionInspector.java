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
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Assessor of spatial relationships that first check the input into an internal
 * rollup map, and keeps track of whether the current zooming level is still
 * covered by the map or not. It only works if no {@link #assess} calls are
 * skipped. It is designed to improve region inspection in a couple of ways:
 * <ul>
 *  <li>If we receive as input a query region that hangs from an internal node
 *  in the map, then we know for sure that that particular region is all vacuum
 *  and no further region assessment is required, but even more importantly, the
 *  backtracking query builder will join the vacuum to adjacent ranges thus
 *  reducing the number of index intervals generated.</li>
 *  <li>If the current region is found in the map, but it is evaluated as
 *  disjoint by the delegate, it provides as the estimate the smaller of the
 *  latest node's count from the map proportional to the index range length, and
 *  the one provided by the delegate, improving the estimate over the one
 *  provided by the delegate.</li>
 *  <li>When {@code disguiseCacheHits} is set, if the current region is found in
 *  the map, and it is evaluated as covered, with no potential over selectivity
 *  by the delegate, it produces a disjoint assessment and it remembers in an
 *  internal cache the skipped values. Caveat: when using this option, since the
 *  internal cache doesn't remember the filter produced by the delegate
 *  inspector, the delegate inspector must never return a meaningful filter for
 *  covered regions with no potential over-selectivity.</li>
 * </ul>
 * 
 * @author Daniel Aioanei
 *
 * @param <T> filter type
 */
public class MapRegionInspector<T> implements RegionInspector<T> {

  private static final Logger logger = Logger.getLogger(MapRegionInspector.class.getName());
  
  private final RegionInspector<T> delegate;
  private final Map<Pow2LengthBitSetRange, NodeValue<CountingDoubleArray>> rolledupMap;
  private final Map<BitVector, CountingDoubleArray> cacheHits;
  
  /**
   * The maximum size of the stack is at most the depth of the tree from which
   * the map was created, which is at most 1 + mMax.
   */
  private Deque<StackElement> stack = new ArrayDeque<StackElement>();
  
  /**
   * 
   * @param rolledupMap
   * @param delegate
   * @param disguiseCacheHits When true, the delegate inspector must always
   * return filters with selectivity 100% for COVERED regions, when {@link
   * Assessment#isPotentialOverSelectivity} is false. Unfortunately we
   * cannot check that condition in here.
   * TODO: Somehow remove the filter altogether from Assessment.
   */
  private MapRegionInspector(
      Map<Pow2LengthBitSetRange, NodeValue<CountingDoubleArray>> rolledupMap,
      RegionInspector<T> delegate, boolean disguiseCacheHits) {
    this.rolledupMap = Preconditions.checkNotNull(rolledupMap, "rolledupMap");
    this.delegate = Preconditions.checkNotNull(delegate, "delegate");
    this.cacheHits =
        disguiseCacheHits ? Maps.<BitVector, CountingDoubleArray>newHashMap() : null;
    logger.info("disguiseCacheHits=" + disguiseCacheHits);
  }

  public static <T> MapRegionInspector<T> create(
      Map<Pow2LengthBitSetRange, NodeValue<CountingDoubleArray>> rolledupMap,
      RegionInspector<T> delegate, boolean disguiseCacheHits) {
    return new MapRegionInspector<T>(rolledupMap, delegate, disguiseCacheHits);
  }

  @Override
  public int getNumberOfDimensions() {
    return delegate.getNumberOfDimensions();
  }
  
  @Override
  public Assessment<T> assess(
      Pow2LengthBitSetRange indexRange, List<Pow2LengthBitSetRange> orthotope) {
    Preconditions.checkState(!stack.isEmpty() || indexRange.getStart().isEmpty());
    StackElement top;
    while ((top = stack.peek()) != null) {
      if (top.range.encloses(indexRange)) {
        break;
      } else {
        stack.pop();
      }
    }
    NodeValue<CountingDoubleArray> value = rolledupMap.get(indexRange);
    final Assessment<T> result;
    if (value != null) {
      assert stack.isEmpty()
          || (stack.peek().range.encloses(indexRange) & !stack.peek().leaf);
      Preconditions.checkState(value.getValue().getCount() > 0);
      stack.push(new StackElement(
          indexRange.clone(), value.getValue().getCount(), value.isLeaf()));
      Assessment<T> localResult = delegateAssessment(indexRange, orthotope);
      // TODO: use an equivalence relation instead of getLevel() == 0 for "group by"
      if (cacheHits != null && localResult.getOutcome() == SpatialRelation.COVERED
          && !localResult.isPotentialOverSelectivity()) {
        if (indexRange.getLevel() != 0) {
          /*
           * Go deeper until we're outside the cache, or until the last significant
           * level has been reached.
           */
          result = Assessment.makeOverlaps();
        } else {
          // Here we drop the filter.
          CountingDoubleArray old =
              cacheHits.put(indexRange.getStart().clone(), value.getValue().clone());
          Preconditions.checkState(old == null);
          Preconditions.checkState(value.getValue().getCount() == 1);
          result = Assessment.makeDisjoint(value.getValue().getCount());
        }
      } else {
        result = localResult;
      }
    } else {
      if (rolledupMap.isEmpty()) {
        result = Assessment.makeDisjoint(0);
      } else {
        /*
         * latestPow2Range cannot be null, since the root is always in the map
         * when not empty.
         */
        assert stack.peek().range.encloses(indexRange);
        if (stack.peek().leaf) {
          result = delegateAssessment(indexRange, orthotope);
        } else {
          result = Assessment.makeDisjoint(0);
        }
      }
    }
    return result;
  }

  private Assessment<T> delegateAssessment(
      Pow2LengthBitSetRange indexRange, List<Pow2LengthBitSetRange> orthotope) {
    assert indexRange.length() <= stack.peek().range.length();
    Assessment<T> delegateAssessment = delegate.assess(indexRange, orthotope);
    if (delegateAssessment.getOutcome() == SpatialRelation.DISJOINT
        && delegateAssessment.getEstimate() > 1) {
      long quotient = indexRange.length() * stack.peek().value
          / stack.peek().range.length();
      if (quotient != 0) {
        if (delegateAssessment.getEstimate() > quotient) {
          return Assessment.makeDisjoint(quotient);
        }
      } else {
        return Assessment.makeDisjoint(1);
      }
    }
    return delegateAssessment;
  }
  
  public Map<BitVector, CountingDoubleArray> getDisguisedCacheHits() {
    return cacheHits == null ? ImmutableMap.<BitVector, CountingDoubleArray>of() : cacheHits;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
  
  private static class StackElement {
    
    private final Pow2LengthBitSetRange range;
    private final Long value;
    private final boolean leaf;
    
    public StackElement(Pow2LengthBitSetRange range, long value, boolean leaf) {
      this.range = range;
      this.value = value;
      this.leaf = leaf;
    }
  }
}
