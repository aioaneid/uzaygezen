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

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Index range with a filter that is valid for the points with a space filling
 * index inside the range.
 * 
 * @param <T> filter type
 * 
 * @author Daniel Aioanei
 */
public class FilteredIndexRange<T> {

  private final LongRange indexRange;
  private final T filter;
  private final boolean potentialOverSelectivity;
  
  private static final Function<FilteredIndexRange<?>, Object>
      FILTER_EXTRACTOR = new Function<FilteredIndexRange<?>, Object>() {
        @Override
        public Object apply(FilteredIndexRange<?> from) {
          return from.getFilter();
      }
    };
  
  private static final Predicate<FilteredIndexRange<?>>
      IS_POTENTIAL_OVER_SELECTIVITY = new Predicate<FilteredIndexRange<?>>() {
        @Override
        public boolean apply(FilteredIndexRange<?> from) {
          return from.potentialOverSelectivity;
        }
  };
    
  public FilteredIndexRange(LongRange indexRange, T filter, boolean potentialOverSelectivity) {
    this.indexRange = Preconditions.checkNotNull(indexRange, "range");
    this.filter = Preconditions.checkNotNull(filter, "filter");
    this.potentialOverSelectivity = potentialOverSelectivity;
  }

  public static <T> FilteredIndexRange<T> of(
      LongRange indexRange, T filter, boolean potentialOverSelectivity) {
    return new FilteredIndexRange<T>(indexRange, filter, potentialOverSelectivity);
  }
  
  public LongRange getIndexRange() {
    return indexRange;
  }

  public T getFilter() {
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
    if (!(o instanceof FilteredIndexRange<?>)) {
      return false;
    }
    FilteredIndexRange<?> other = (FilteredIndexRange<?>) o;
    return indexRange.equals(other.indexRange) && filter.equals(other.filter)
        && potentialOverSelectivity == other.potentialOverSelectivity;
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> Function<FilteredIndexRange<T>, T> filterExtractor() {
    return (Function) FILTER_EXTRACTOR;
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> Predicate<FilteredIndexRange<T>> potentialOverSelectivityExtractor() {
    return (Predicate) IS_POTENTIAL_OVER_SELECTIVITY;
  }
}
