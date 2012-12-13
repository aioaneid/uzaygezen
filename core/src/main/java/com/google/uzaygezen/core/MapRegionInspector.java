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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.uzaygezen.core.ranges.Content;

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
public class MapRegionInspector<T, V extends Content<V>> implements RegionInspector<T, V> {

  private static final Logger logger = Logger.getLogger(MapRegionInspector.class.getName());
  
  private final RegionInspector<T, V> delegate;
  private final Map<Pow2LengthBitSetRange, NodeValue<V>> rolledupMap;
  private final Map<BitVector, V> cacheHits;
  
  /**
   * The maximum size of the stack is at most the depth of the tree from which
   * the map was created, which is at most 1 + mMax.
   */
  private Deque<StackElement<V>> stack = new ArrayDeque<>();

  private final V zero, one;
  
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
      Map<Pow2LengthBitSetRange, NodeValue<V>> rolledupMap,
      RegionInspector<T, V> delegate, boolean disguiseCacheHits, V zero, V one) {
    this.rolledupMap = Preconditions.checkNotNull(rolledupMap, "rolledupMap");
    this.delegate = Preconditions.checkNotNull(delegate, "delegate");
    this.cacheHits =
        disguiseCacheHits ? new HashMap<BitVector, V>() : null;
    logger.info("disguiseCacheHits=" + disguiseCacheHits);
    this.zero = zero;
    this.one = one;
  }

  public static <T, V extends Content<V>> MapRegionInspector<T, V> create(
      Map<Pow2LengthBitSetRange, NodeValue<V>> rolledupMap,
      RegionInspector<T, V> delegate, boolean disguiseCacheHits, V zero, V one) {
    return new MapRegionInspector<T, V>(rolledupMap, delegate, disguiseCacheHits, zero, one);
  }

  @Override
  public int getNumberOfDimensions() {
    return delegate.getNumberOfDimensions();
  }
  
  @Override
  public Assessment<T, V> assess(
      Pow2LengthBitSetRange indexRange, List<Pow2LengthBitSetRange> orthotope) {
    Preconditions.checkState(!stack.isEmpty() || indexRange.getStart().isEmpty());
    StackElement<V> top;
    while ((top = stack.peek()) != null) {
      if (top.range.encloses(indexRange)) {
        break;
      } else {
        stack.pop();
      }
    }
    NodeValue<V> value = rolledupMap.get(indexRange);
    final Assessment<T, V> result;
    if (value != null) {
      assert stack.isEmpty()
          || (stack.peek().range.encloses(indexRange) & !stack.peek().leaf);
      Preconditions.checkState(!value.getValue().isZero());
      stack.push(new StackElement<V>(
          indexRange.clone(), value.getValue(), value.isLeaf()));
      Assessment<T, V> localResult = delegateAssessment(indexRange, orthotope);
      // TODO: use an equivalence relation instead of getLevel() == 0 for "group by"
      if (cacheHits != null && localResult.getOutcome() == SpatialRelation.COVERED
          && !localResult.isPotentialOverSelectivity()) {
        if (indexRange.getLevel() != 0) {
          /*
           * Go deeper until we're outside the cache, or until the last significant
           * level has been reached.
           */
          result = Assessment.makeOverlaps(zero);
        } else {
          // Here we drop the filter.
          Content<V> old =
              cacheHits.put(indexRange.getStart().clone(), value.getValue().clone());
          Preconditions.checkState(old == null);
          Preconditions.checkState(value.getValue().isOne());
          result = Assessment.makeDisjoint(value.getValue());
        }
      } else {
        result = localResult;
      }
    } else {
      if (rolledupMap.isEmpty()) {
        result = Assessment.makeDisjoint(zero);
      } else {
        /*
         * latestPow2Range cannot be null, since the root is always in the map
         * when not empty.
         */
        assert stack.peek().range.encloses(indexRange);
        if (stack.peek().leaf) {
          result = delegateAssessment(indexRange, orthotope);
        } else {
          result = Assessment.makeDisjoint(zero);
        }
      }
    }
    return result;
  }

  private Assessment<T, V> delegateAssessment(
      Pow2LengthBitSetRange indexRange, List<Pow2LengthBitSetRange> orthotope) {
    assert indexRange.getLevel() <= stack.peek().range.getLevel();
    Assessment<T, V> delegateAssessment = delegate.assess(indexRange, orthotope);
    if (delegateAssessment.getOutcome() == SpatialRelation.DISJOINT
        && (!delegateAssessment.getEstimate().isZero() & !delegateAssessment.getEstimate().isOne())) {
      V quotient = stack.peek().value.clone();
      quotient.shiftRight(
        stack.peek().range.getLevel() - indexRange.getLevel());
      if (!quotient.isZero()) {
        if (delegateAssessment.getEstimate().compareTo(quotient) > 0) {
          return Assessment.makeDisjoint(quotient);
        }
      } else {
        return Assessment.makeDisjoint(one);
      }
    }
    return delegateAssessment;
  }
  
  public Map<BitVector, V> getDisguisedCacheHits() {
    return cacheHits == null ? ImmutableMap.<BitVector, V>of() : cacheHits;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
  
  private static class StackElement<V extends Content<V>> {
    
    private final Pow2LengthBitSetRange range;
    private final V value;
    private final boolean leaf;
    
    public StackElement(Pow2LengthBitSetRange range, V value, boolean leaf) {
      this.range = range;
      this.value = value;
      this.leaf = leaf;
    }
  }
}
