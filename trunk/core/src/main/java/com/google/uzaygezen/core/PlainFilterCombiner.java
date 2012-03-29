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

/**
 * Very simple filter combiner that always uses the object {@link #FILTER} as
 * the combined filter.
 * 
 * @author Daniel Aioanei
 */
public enum PlainFilterCombiner implements FilterCombiner<Object> {

  INSTANCE {
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
    public SelectiveFilter<Object> combine(
        FilteredIndexRange<Object> firstFilteredRange,
        FilteredIndexRange<Object> secondFilteredRange, long gapEstimate) {
      int cmp = Long.signum(firstFilteredRange.getIndexRange().getEnd()
          - Long.valueOf(secondFilteredRange.getIndexRange().getStart()));
      int gapSignum = Long.signum(gapEstimate);
      Preconditions.checkArgument((cmp == -1 && gapSignum >= 0) || (cmp == 0 & gapSignum == 0));
      return SelectiveFilter.of(FILTER, gapEstimate != 0
          || firstFilteredRange.isPotentialOverSelectivity()
          || secondFilteredRange.isPotentialOverSelectivity());
    }
  };
  
  public static final Object FILTER = new Object();
}
