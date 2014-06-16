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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Preconditions;

/**
 * Encapsulates a filter and a flag indicating if the filter could result in
 * more points than needed being selected.
 * 
 * TODO: Add some unit tests.
 * 
 * @author Daniel Aioanei
 * 
 * @param <T> filter type
 */
public class SelectiveFilter<T> {
  
  private final T filter;

  private final boolean potentialOverSelectivity;
  
  private SelectiveFilter(T filter, boolean potentialOverSelectivity) {
    this.filter = Preconditions.checkNotNull(filter);
    this.potentialOverSelectivity = potentialOverSelectivity;
  }
  
  public static <T> SelectiveFilter<T> of(T filter, boolean potentialOverSelectivity) {
    return new SelectiveFilter<T>(filter, potentialOverSelectivity);
  }
  
  public T getFilter() {
    return filter;
  }

  public boolean isPotentialOverSelectivity() {
    return potentialOverSelectivity;
  }
  
  @Override
  public int hashCode() {
    return 31 * filter.hashCode() + Boolean.valueOf(potentialOverSelectivity).hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SelectiveFilter<?>)) {
      return false;
    }
    SelectiveFilter<?> other = (SelectiveFilter<?>) o;
    return potentialOverSelectivity == other.potentialOverSelectivity
        && filter.equals(other.filter);
  }
  
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
