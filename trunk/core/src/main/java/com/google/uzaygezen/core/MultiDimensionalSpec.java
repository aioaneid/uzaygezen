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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Specification of the boundaries of a (compact) multidimensional space. It can
 * represents multidimensional spaces with any number of dimensions, including
 * no dimensions at all, and it allows degenerate dimensions with zero bits.
 * Particular algorithm implementations may impose more restrictions though.
 *
 * @author Daniel Aioanei
 */
public class MultiDimensionalSpec {
  
  private final List<Integer> bitsPerDimension;
  private final int sumBitsPerDimension;
  private final int maxBitsPerDimension;
  
  public MultiDimensionalSpec(List<Integer> bitsPerDimension) {
    int maxBits = 0;
    int sumBits = 0;
    for (int bitsPerDim : bitsPerDimension) {
      Preconditions.checkArgument(bitsPerDim >= 0,
          "The number of bits for each dimension must be non-negative.");
      if (maxBits < bitsPerDim) {
        maxBits = bitsPerDim;
      }
      sumBits += bitsPerDim;
      Preconditions.checkArgument(sumBits >= 0, "The sum of the all bits must fit in int");
    }
    this.bitsPerDimension = Collections.unmodifiableList(Lists.newArrayList(bitsPerDimension));
    this.maxBitsPerDimension = maxBits;
    this.sumBitsPerDimension = sumBits;
  }
  
  /**
   * Usually called {@code m} throughout this package.
   */
  public List<Integer> getBitsPerDimension() {
    return bitsPerDimension;
  }
  
  /**
   * Usually called {@code mSum} throughout this package.
   */
  public int sumBitsPerDimension() {
    return sumBitsPerDimension;
  }
  
  /**
   * Usually called {@code mMax} throughout this package.
   */
  public int maxBitsPerDimension() {
    return maxBitsPerDimension;
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
