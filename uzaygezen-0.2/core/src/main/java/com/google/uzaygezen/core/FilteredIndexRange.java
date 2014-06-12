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

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.uzaygezen.core.ranges.Range;

/**
 * Index range with a filter that is valid for the points with a space filling
 * index inside the range.
 * 
 * @param <T> filter type
 * 
 * @author Daniel Aioanei
 */
public class FilteredIndexRange<F, R> {

  private final R indexRange;
  private final F filter;
  private final boolean potentialOverSelectivity;
  
  private static final Function<FilteredIndexRange<?, ?>, Object>
      FILTER_EXTRACTOR = new Function<FilteredIndexRange<?, ?>, Object>() {
        @Override
        public Object apply(FilteredIndexRange<?, ?> from) {
          return from.getFilter();
      }
    };
  
  private static final Predicate<FilteredIndexRange<?, ?>>
      IS_POTENTIAL_OVER_SELECTIVITY = new Predicate<FilteredIndexRange<?, ?>>() {
        @Override
        public boolean apply(FilteredIndexRange<?, ?> from) {
          return from.potentialOverSelectivity;
        }
  };
    
  public FilteredIndexRange(R indexRange, F filter, boolean potentialOverSelectivity) {
    this.indexRange = Preconditions.checkNotNull(indexRange, "range");
    this.filter = Preconditions.checkNotNull(filter, "filter");
    this.potentialOverSelectivity = potentialOverSelectivity;
  }

  public static <F, R> FilteredIndexRange<F, R> of(
      R indexRange, F filter, boolean potentialOverSelectivity) {
    return new FilteredIndexRange<F,R>(indexRange, filter, potentialOverSelectivity);
  }
  
  public R getIndexRange() {
    return indexRange;
  }

  public F getFilter() {
    return filter;
  }
  
  public boolean isPotentialOverSelectivity() {
    return potentialOverSelectivity;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(indexRange, filter, potentialOverSelectivity);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FilteredIndexRange<?, ?>)) {
      return false;
    }
    FilteredIndexRange<?, ?> other = (FilteredIndexRange<?, ?>) o;
    return indexRange.equals(other.indexRange) && filter.equals(other.filter)
        && potentialOverSelectivity == other.potentialOverSelectivity;
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <F, T, V, R extends Range<T, V>> Function<FilteredIndexRange<F, R>, F> filterExtractor() {
    return (Function) FILTER_EXTRACTOR;
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <F, R> Predicate<FilteredIndexRange<F, R>> potentialOverSelectivityExtractor() {
    return (Predicate) IS_POTENTIAL_OVER_SELECTIVITY;
  }
  
  public static <F, T, V extends AdditiveValue<V>, R extends Range<T, V>> V sumRangeLengths(
    Iterable<FilteredIndexRange<F, R>> iterable, V zero) {
    V sum = zero.clone();
    for (FilteredIndexRange<F, R> r : iterable) {
      sum.add(r.getIndexRange().length());
    }
    return sum;
  }
}
