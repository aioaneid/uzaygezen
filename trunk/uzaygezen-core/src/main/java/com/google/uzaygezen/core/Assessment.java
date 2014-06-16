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

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * The result of the user assessment of the spatial relationship between one
 * spatial object and the query region. The following states are representable:
 * <ul>
 *  <li>{@link SpatialRelation#DISJOINT} with a {@code 0} (zero) {@link
 *  Assessment#estimate} means that there is no way the spatial object could
 *  overlap with points in the data set. That implies that the user doesn't care
 *  if the spatial object is filtered out or not from the final query
 *  expression, since it will not contain any points anyway.</li>
 *  <li>{@link SpatialRelation#DISJOINT} with a positive {@link
 *  Assessment#estimate} means that the spatial object is disjoint with the
 *  query region, but the spatial object is estimated to contain {@code
 *  estimate} data set points. This gap size estimate is later used to decide
 *  which consecutive ranges should be joined into one contiguous range, trying
 *  to minimise the number of included points that are not useful to the user,
 *  when there is a constraint on the number of ranges of the query expression
 *  being created.</li>
 *  <li>{@link SpatialRelation#COVERED} with a non-null {@link
 *  Assessment#filter} means that the spatial object's sub-space identified by
 *  the filter is fully included in the query region, or that the user doesn't
 *  want to zoom any deeper in the multidimensional space and that filtering of
 *  unneeded points can be done later, outside the space filling framework.
 *  Although the filter object is not allowed to be null, it will commonly
 *  represent a no-op filtering.</li>
 *  <li>{@link SpatialRelation#OVERLAPS} means that the spatial object overlaps,
 *  but is neither disjoint nor included in the query region, or that the user
 *  can't figure out what's the exact relationship and wants to zoom into the
 *  region.</li>
 * </ul>
 * 
 * @author Daniel Aioanei
 * 
 * @param <T> filter type
 */
public class Assessment<T, V> {
  
  private final SpatialRelation outcome;
  
  /**
   * Non-null iff {@link #outcome} is {@link SpatialRelation#DISJOINT}.
   */
  private final V estimate;
  
  /**
   * Non-null iff {@link #outcome} is {@link SpatialRelation#COVERED}.
   */
  private final T filter;
  
  /**
   * Can only be {@literal true} for {@link SpatialRelation#COVERED}, and then
   * it means that there the region inspector would have liked to return {@link
   * SpatialRelation#OVERLAPS} instead, but to speed up the query building step
   * it does not want to see the children of the orthotope inspected.
   */
  private final boolean potentialOverSelectivity;
  
  private Assessment(V estimate, boolean potentialOverSelectivity,
      SpatialRelation outcome, T filter) {
    this.estimate = estimate;
    this.potentialOverSelectivity = potentialOverSelectivity;
    this.outcome = outcome;
    this.filter = filter;
  }
  
  public static <T, V> Assessment<T, V> makeDisjoint(V gapEstimate) {
//    Preconditions.checkArgument(gapEstimate >= 0);
    return new Assessment<T, V>(gapEstimate, false, SpatialRelation.DISJOINT, null);
  }
  
  public static <T, V> Assessment<T, V> makeOverlaps(V zero) {
    return new Assessment<>(zero, false, SpatialRelation.OVERLAPS, null);
  }
  
  public static <T, V> Assessment<T, V> makeCovered(
    T coverFilter, boolean overSelectivityPossible, V zero) {
    return new Assessment<T, V>(zero, overSelectivityPossible, SpatialRelation.COVERED,
        Preconditions.checkNotNull(coverFilter, "filter"));
  }
  
  public SpatialRelation getOutcome() {
    return outcome;
  }
  
  public V getEstimate() {
    return estimate;
  }
  
  public T getFilter() {
    return filter;
  }

  /**
   * @return is there any chance that we might have over-selected?
   */
  public boolean isPotentialOverSelectivity() {
    return potentialOverSelectivity;
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Assessment)) {
      return false;
    }
    Assessment<?, ?> other = (Assessment<?, ?>) o;
    return outcome == other.outcome
        && potentialOverSelectivity == other.potentialOverSelectivity
        && Objects.equal(estimate, other.estimate) && Objects.equal(filter, other.filter);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(outcome, potentialOverSelectivity, estimate, filter);
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
