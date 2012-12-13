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
import com.google.uzaygezen.core.ranges.Content;
import com.google.uzaygezen.core.ranges.Range;

/**
 * Very simple filter combiner that always uses the field {@link #fixedFilter} as
 * the combined filter.
 * 
 * @author Daniel Aioanei
 */
public class PlainFilterCombiner<F, T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> implements FilterCombiner<F, V, R> {

  private final F fixedFilter;

  public PlainFilterCombiner(F filter) {
    this.fixedFilter = filter;
  }
  
  /**
   * Produces a marker of potential selectivity iff at least one of the
   * following holds:
   * <ul>
   *   <li>{@code gapEstimate != 0}</li>
   *   <li>{@code firstFilteredRange.isPotentialOverSelectivity()} holds</li>
   *   <li>{@code secondFilteredRange.isPotentialOverSelectivity()} holds</li>
   * </ul>
   */
  @Override
  public SelectiveFilter<F> combine(
      FilteredIndexRange<F, R> firstFilteredRange,
      FilteredIndexRange<F, R> secondFilteredRange, V gapEstimate) {
    int cmp = firstFilteredRange.getIndexRange().getEnd().compareTo(
        secondFilteredRange.getIndexRange().getStart());
    int gapSignum = gapEstimate.isZero() ? 0 : 1;
    Preconditions.checkArgument((cmp < 0 & gapSignum >= 0) || (cmp == 0 & gapSignum == 0));
    return SelectiveFilter.of(fixedFilter, !gapEstimate.isZero()
        || firstFilteredRange.isPotentialOverSelectivity()
        || secondFilteredRange.isPotentialOverSelectivity());
  }
}
