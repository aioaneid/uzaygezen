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

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Preconditions;

/**
 * Filter combiner that works with filters consisting in a list of ranges, and
 * concatenates them. It only works with non-empty lists, and it never produces
 * empty lists. And the input filters must always start and end at the
 * boundaries of the filtered index; all filters produced by this class have
 * that property as well.
 * 
 * @author Daniel Aioanei
 */
public class ListConcatCombiner implements FilterCombiner<RangeListFilter> {

  public static final ListConcatCombiner UNBOUNDED_INSTANCE =
      new ListConcatCombiner(Integer.MAX_VALUE);
  
  private final int threshold;
  
  public ListConcatCombiner(int threshold) {
    Preconditions.checkArgument(threshold > 0, "threshold must be positive");
    this.threshold = threshold;
  }
  
  /**
   * If the gap is zero, joins the last index range and the first index range
   * into one range that covers both, and keeps the other ranges intact.
   * Otherwise it concatenates the two lists of ranges. Note that we don't use
   * the full information available in {@code gapEstimate}. Instead we extract
   * its signum and use that to optimise the situation when the gap is zero.
   * 
   * @param lower must not be empty
   * @param higher must not be empty
   * @return the computed list and whether the threshold was exceeded or one of
   * the two filters already had potential over-selectivity.
   */
  @Override
  public SelectiveFilter<RangeListFilter> combine(FilteredIndexRange<RangeListFilter> lower,
      FilteredIndexRange<RangeListFilter> higher, long gapEstimate) {
    checkInputFilteredIndexRange(lower);
    checkInputFilteredIndexRange(higher);
    RangeListFilter combinedFilter =
        lower.getFilter().combine(higher.getFilter(), threshold, gapEstimate);
    /*
     * Now we'll have the threshold exceeded information in both the filter
     * and as the second element of the pair. The reason is that the caller of
     * this method does not know about RangeListFilter, and instead it only
     * knows that there is a type T representing the filter.
     */
    return SelectiveFilter.of(combinedFilter, combinedFilter.isThresholdExceeded()
        || lower.isPotentialOverSelectivity() || higher.isPotentialOverSelectivity());
  }
  
  /**
   * Checks some sanity properties of {@code filteredIndexRange}.
   */
  private static void checkInputFilteredIndexRange(
      FilteredIndexRange<RangeListFilter> filteredIndexRange) {
    List<LongRange> filter = filteredIndexRange.getFilter().getRangeList();
    Preconditions.checkArgument(filteredIndexRange.getIndexRange().getStart() ==
        filter.get(0).getStart(), "invalid start");
    Preconditions.checkArgument(filteredIndexRange.getIndexRange().getEnd() ==
        filter.get(filter.size() - 1).getEnd(), "invalid end");
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
