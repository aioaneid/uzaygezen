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
 * Encapsulates a value and a flag indicating if the value's node is a leaf.
 * 
 * TODO: Add some unit tests.
 * 
 * @author Daniel Aioanei
 * 
 * @param <T> node value type
 */
public class NodeValue<T> {
  
  private final T value;

  private final boolean leaf;
  
  private NodeValue(T filter, boolean leaf) {
    this.value = Preconditions.checkNotNull(filter);
    this.leaf = leaf;
  }
  
  public static <T> NodeValue<T> of(T filter, boolean leaf) {
    return new NodeValue<T>(filter, leaf);
  }
  
  public T getValue() {
    return value;
  }

  public boolean isLeaf() {
    return leaf;
  }
  
  @Override
  public int hashCode() {
    return 31 * value.hashCode() + Boolean.valueOf(leaf).hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NodeValue<?>)) {
      return false;
    }
    NodeValue<?> other = (NodeValue<?>) o;
    return leaf == other.leaf && value.equals(other.value);
  }
  
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
