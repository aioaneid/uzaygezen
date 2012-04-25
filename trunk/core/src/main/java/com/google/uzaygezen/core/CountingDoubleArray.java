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

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * The natural ordering of this class is <em>inconsistent with equals</em>. This
 * is not a problem since {@link BoundedRollup} never uses {@code
 * equals/hashCode} to compare values.
 * 
 * TODO: Add some unit tests.
 * 
 * @author Daniel Aioanei
 */
public class CountingDoubleArray implements AdditiveValue<CountingDoubleArray> {

  private long count;
  private final double[] measures;
  
  public CountingDoubleArray(double[] stats) {
    this(1, stats);
  }
  
  /**
   * Visible for testing.
   */
  CountingDoubleArray(long count, double[] measures) {
    assert count >= 0;
    this.count = count;
    this.measures = Arrays.copyOf(measures, measures.length);
  }
  
  public static CountingDoubleArray newEmptyValue(int measureCount) {
    return new CountingDoubleArray(
        0, measureCount == 0 ? ArrayUtils.EMPTY_DOUBLE_ARRAY : new double[measureCount]);
  }
  
  @Override
  public void add(CountingDoubleArray other) {
    int measureCount = measures.length;
    Preconditions.checkArgument(measureCount == other.measures.length, "different lengths");
    count += other.count;
    for (int i = 0; i < measureCount; ++i) {
      measures[i] += other.measures[i];
    }
  }

  @Override
  public int compareTo(CountingDoubleArray o) {
    long x = count;
    long y = o.count;
    return x < y ? -1 : (x == y ? 0 : 1);
  }
  
  @Override
  public CountingDoubleArray clone() {
    // The measures are copied by the constructor.
    return new CountingDoubleArray(count, measures);
  }
  
  public long getCount() {
    return count;
  }
  
  public double[] getMeasures() {
    return Arrays.copyOf(measures, measures.length);
  }
  
  /**
   * For testing purposes only.
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(count, Arrays.hashCode(measures));
  }
  
  /**
   * For testing purposes only.
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CountingDoubleArray)) {
      return false;
    }
    CountingDoubleArray other = (CountingDoubleArray) o;
    return count == other.count && Arrays.equals(measures, other.measures);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
