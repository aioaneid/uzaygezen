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

/**
 * Assessor of spatial relationships that works with a query region composed of
 * disjoint orthotopes and assumes uniform distribution of the data points in
 * the multidimensional space.
 * 
 * @author Daniel Aioanei
 *
 * @param <T> filter type
 */
public class SimpleRegionInspector<T> implements RegionInspector<T> {
  
  /**
   * Set of hopefully disjoint orthotopes. Currently we don't check that they
   * are disjoint, but we might in the future.
   */
  private final List<? extends List<LongRange>> queryRegion;
  
  /**
   * Threshold for the orthotope content under which OVERLAPS becomes COVERED.
   */
  private final long minOverlappingContent;
  
  /**
   * Factory of non-null filter objects.
   */
  private final Function<? super LongRange, T> filterFactory;
  
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
  public static <T> SimpleRegionInspector<T> create(
      List<? extends List<LongRange>> queryRegion,
      long minOverlappingContent, Function<? super LongRange, T> filterFactory) {
    return new SimpleRegionInspector<T>(queryRegion, minOverlappingContent, filterFactory);
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
      List<? extends List<LongRange>> queryRegion,
      long minOverlappingContent, Function<? super LongRange, T> filterFactory) {
    Iterator<? extends List<LongRange>> queryRegionIterator = queryRegion.iterator();
    List<LongRange> firstOrthotope = queryRegionIterator.next();
    while (queryRegionIterator.hasNext()) {
      List<LongRange> orthotope = queryRegionIterator.next();
      Preconditions.checkArgument(firstOrthotope.size() == orthotope.size());
    }
    this.queryRegion = queryRegion;
    Preconditions.checkArgument(minOverlappingContent > 0,
        "minOverlappingContent must be positive but it is %s.", minOverlappingContent);
    this.minOverlappingContent = minOverlappingContent;
    this.filterFactory = filterFactory;
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
  public Assessment<T> assess(
      Pow2LengthBitSetRange indexRange, List<Pow2LengthBitSetRange> orthotope) {
    final long rangeLength = indexRange.length();
    assert rangeLength == Pow2LengthBitSetRange.content(orthotope)
        : String.format("rangeLength=%s but content=%s",
            rangeLength, Pow2LengthBitSetRange.content(orthotope));
    long commonContent = LongRange.overlapSum(
        Pow2LengthBitSetRange.toLongOrthotope(orthotope), queryRegion);
    int cmp = Long.valueOf(commonContent).compareTo(Long.valueOf(rangeLength));
    if (cmp == 0) {
      return Assessment.makeCovered(filterFactory.apply(indexRange.toLongRange()), false);
    } else {
      assert cmp == -1;
      if (commonContent == 0) {
        return Assessment.makeDisjoint(rangeLength);
      } else {
        if (rangeLength >= minOverlappingContent) {
          return Assessment.makeOverlaps();
        } else {
          return Assessment.makeCovered(filterFactory.apply(indexRange.toLongRange()), true);
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
