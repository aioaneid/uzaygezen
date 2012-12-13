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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.uzaygezen.core.ranges.Content;
import com.google.uzaygezen.core.ranges.Measurable;
import com.google.uzaygezen.core.ranges.Range;
import com.google.uzaygezen.core.ranges.RangeHome;
import com.google.uzaygezen.core.ranges.RangeUtil;

/**
 * Assessor of spatial relationships that works with a query region composed of
 * disjoint orthotopes and assumes uniform distribution of the data points in
 * the multidimensional space.
 * 
 * @author Daniel Aioanei
 *
 * @param <T> filter type
 */
public class SimpleRegionInspector<F, T, V extends AdditiveValue<V>, R extends Measurable<V>> implements RegionInspector<F, V> {
  
  /**
   * Set of hopefully disjoint orthotopes. Currently we don't check that they
   * are disjoint, but we might in the future.
   */
  private final List<? extends List<R>> queryRegion;
  
  /**
   * Threshold for the orthotope content under which OVERLAPS becomes COVERED.
   */
  private final V minOverlappingContent;
  
  /**
   * Factory of non-null filter objects.
   */
  private final Function<? super R, F> filterFactory;
  
  private final RangeHome<T, V, R> rangeHome;
  
  private final V zero;
  
  /**
   * @param <T> filter type
   * @param queryRegion set of disjoint orthotopes. If they are not disjoint the
   * behaviour is undefined.
   * @param minOverlappingContent Not null and not zero. If the result would
   * otherwise be OVERLAP, but the range length is less than this number, then
   * we return COVERED. That can lead to an artificial increase of the
   * selectivity of the created query, but it's necessary as a barrier against
   * query regions with a large measure of order 2.
   * @param filterFactory factory of non-null filter objects
   * @return a simple region inspector
   */
  public static <F, T, V extends Content<V>, R extends Range<T, V>> SimpleRegionInspector<F, T, V, R> create(
      List<? extends List<R>> queryRegion,
      V minOverlappingContent, Function<? super R, F> filterFactory,
      RangeHome<T, V, R> rangeHome, V zero) {
    return new SimpleRegionInspector<F, T, V, R>(
      queryRegion, minOverlappingContent, filterFactory, rangeHome, zero);
  }
  
  /**
   * @param queryRegion set of disjoint orthotopes. If they are not disjoint the
   * behaviour is undefined.
   * @param minOverlappingContent Not null and not zero. If the result would
   * otherwise be OVERLAP, but the range length is less than this number, then
   * we return COVERED. That can lead to an artificial increase of the
   * selectivity of the created query, but it's necessary as a barrier against
   * query regions with a large measure of order 2.
   * @param filterFactory factory of non-null filter objects
   */
  private SimpleRegionInspector(
      List<? extends List<R>> queryRegion,
      V minOverlappingContent, Function<? super R, F> filterFactory,
      RangeHome<T, V, R> rangeHome, V zero) {
    Iterator<? extends List<R>> queryRegionIterator = queryRegion.iterator();
    List<R> firstOrthotope = queryRegionIterator.next();
    while (queryRegionIterator.hasNext()) {
      List<R> orthotope = queryRegionIterator.next();
      Preconditions.checkArgument(firstOrthotope.size() == orthotope.size());
    }
    this.queryRegion = queryRegion;
    Preconditions.checkArgument(!minOverlappingContent.isZero(),
        "minOverlappingContent must be positive but it is %s.", minOverlappingContent);
    this.minOverlappingContent = minOverlappingContent;
    this.filterFactory = filterFactory;
    this.rangeHome = rangeHome;
    this.zero = zero;
  }

  /**
   * Currently it computes the overlapping content, and then it figures out if
   * the orthotope being checked and the query region are disjoint, if the
   * former is covered by the latter, or if they only overlap. In the future we
   * might be able to optimise the computation so that once we discover let's
   * say one point in common, and one point contained by the orthotope but not
   * in the query region, we could return OVERLAP early.
   */
  @Override
  public Assessment<F, V> assess(
      Pow2LengthBitSetRange indexBitSetRange, List<Pow2LengthBitSetRange> orthotope) {
    assert indexBitSetRange.getLevel() == Pow2LengthBitSetRange.levelSum(orthotope)
        : String.format("rangeLevel=%s but content=%s",
          indexBitSetRange.getLevel(), Pow2LengthBitSetRange.levelSum(orthotope));
    V commonContent = zero.clone();
    RangeUtil.overlapSum(RangeUtil.toOrthotope(orthotope, rangeHome), queryRegion, rangeHome, commonContent);
    R indexRange = rangeHome.toRange(indexBitSetRange);
    V rangeLength = indexRange.length();
    int cmp = commonContent.compareTo(rangeLength);
    if (cmp == 0) {
      return Assessment.makeCovered(filterFactory.apply(indexRange), false, zero);
    } else {
      if (commonContent.equals(zero)) {
        return Assessment.makeDisjoint(rangeLength);
      } else {
        if (rangeLength.compareTo(minOverlappingContent) >= 0) {
          return Assessment.makeOverlaps(zero);
        } else {
          return Assessment.makeCovered(filterFactory.apply(indexRange), true, zero);
        }
      }
    }
  }
  
  @Override
  public int getNumberOfDimensions() {
    return queryRegion.get(0).size();
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
