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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Represents a computed query which can be applied, perhaps after further
 * transformations, into an external system such as a storage server.
 * 
 * @author Daniel Aioanei
 *
 * @param <T> filter type
 */
public class Query<T> {
  
  private final List<FilteredIndexRange<T>> filteredIndexRanges;
  
  private static final Query<Object> EMPTY_QUERY =
      of(ImmutableList.<FilteredIndexRange<Object>>of());
  
  private Query(List<FilteredIndexRange<T>> filteredIndexRanges) {
    this.filteredIndexRanges = ImmutableList.copyOf(filteredIndexRanges);
  }

  public static <T> Query<T> of(List<FilteredIndexRange<T>> filteredIndexRanges) {
    return new Query<T>(filteredIndexRanges); 
  }
  
  public List<FilteredIndexRange<T>> getFilteredIndexRanges() {
    return filteredIndexRanges;
  }

  public boolean isPotentialOverSelectivity() {
    boolean potentialOverSelectivity = Iterables.any(
        filteredIndexRanges, FilteredIndexRange.<T>potentialOverSelectivityExtractor());
    return potentialOverSelectivity;
  }
  
  @Override
  public int hashCode() {
    return 31 * filteredIndexRanges.hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Query)) {
      return false;
    }
    Query<?> other = (Query<?>) o;
    return filteredIndexRanges.equals(other.filteredIndexRanges);
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
  
  @SuppressWarnings("unchecked")
  public static <T> Query<T> emptyQuery() {
    return (Query<T>) EMPTY_QUERY;
  }
}
