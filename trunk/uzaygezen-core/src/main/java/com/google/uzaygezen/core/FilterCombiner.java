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


/**
 * Combiner of two space filling curve index ranges with attached filters.
 * Because the number of index ranges that a query expression must have is
 * limited, this abstraction allows one to customise the operation of joining
 * together two consecutive ranges, while still preserving some filtering of the
 * extraneous data inside. Thus while the index ranges are an inclusive
 * mechanism, this is the exclusive mechanism that works inside an index range.
 * <p>
 * Implementations are free to filter out all irrelevant data, or only some of
 * it, if at all. If the former happens for each call, and in addition to that,
 * the {@link SpaceVisitor} never returns {@link SpatialRelation#COVERED} when
 * there is a chance for extraneous points in that particular orthotope, the
 * framework guarantees that that no irrelevant data points will be selected by
 * the query. On the other hand, if the filter object stats getting too big, an
 * implementation is free to create a filter that doesn't filter anything at
 * all. But in that case extraneous points might (and probably will) be selected
 * by the constructed query.
 * </p>
 * 
 * @author Daniel Aioanei
 *
 * @param <T> filter type
 */
public interface FilterCombiner<F, V, R> {
  
  /**
   * Combines two filtered index ranges and produces a combined filter that must
   * be valid in the context of the range {@code
   * [firstFilteredRange.getIndexRange().getStart(),
   * secondFilteredRange.getIndexRange().getEnd())}. When the framework calls
   * this method it is guaranteed that {@code firstRange} is disjoint and sits
   * before {@code secondIndexRange} on the space filling curve.
   * <p>
   * The parameter {@code gapEstimate} is provided to help implementations trade
   * the size of the filter in some representation format for selectivity of
   * irrelevant points, and to enabled heavy optimisations free of any tradeoffs
   * when the gap is zero.
   * </p>
   *  
   * @param firstFilteredRange the filtered index range coming before on the
   * space filling curve
   * @param secondFilteredRange the filtered index range coming after on the
   * space filling curve
   * @param gapEstimate the estimate of the number of points in the gap between
   * the index ranges. If zero, then it is guaranteed that there are no data
   * points in between. If non zero, it is just an estimate and its accuracy
   * depends on the accuracy provided by the {@link RegionInspector} in use.
   * If the two ranges are adjacent, i.e. {@code
   * firstFilteredRange.getIndexRange().getEnd() ==
   * secondFilteredRange.getIndexRange().getStart()}, then the gap estimate is
   * guaranteed to be zero.
   * @return the combined filter and a bool value specifying if potential over
   * selectivity might result by replacing the two filtered ranges with one
   * range with the combined filter attached
   */
  SelectiveFilter<F> combine(FilteredIndexRange<F, R> firstFilteredRange,
      FilteredIndexRange<F, R> secondFilteredRange, V gapEstimate);
}
