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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.uzaygezen.core.ranges.Range;
import com.google.uzaygezen.core.ranges.RangeHome;

/**
 * Associates a list of ranges with a boolean specifying if the list of ranges has
 * exceeded the maximum limit. If at least one of two such filters has exceeded
 * the threshold in the past, then the combined threshold will have just one
 * element in the list.
 * 
 * TODO: We should really use a min-heap to perform the optimal
 * concatenations instead. But for now this approach should work as
 * well, although it will lead to too much over-selectivity since
 * thresholdExceeded will quickly become true and thus inside-range
 * filtering won't get to be used that much. Although it might look weird that
 * we should use 2 min-heaps, one in {@link BacktrackingQueryBuilder} and one
 * here, the explanation is that getting the ranges right is much more important
 * than getting the filter inside each range as tight as possible. As a result
 * {@link BacktrackingQueryBuilder} uses one min-heap of its own which gets the
 * best possible ranges, and then the inside each range, we should also get the
 * best possible filter. But we consider the former to be of utmost importance. 
 * 
 * @author Daniel Aioanei
 */
public class RangeListFilter<T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> {

  private static final Logger logger = Logger.getLogger(RangeListFilter.class.getName());
  
  private final List<R> rangeList;
  private final boolean thresholdExceeded;
  private final Level thresholdExceededLogLevel;
  private final RangeHome<T, V, R> rangeHome;
  
  public static <T extends Comparable<T>, V extends Content<V>, R extends Range<T, V>> Function<R, RangeListFilter<T, V, R>> creator(
      final Level thresholdExceededLogLevel, final RangeHome<T, V, R> rangeHome) {
      return new Function<R, RangeListFilter<T, V, R>>() {
        @Override
        public RangeListFilter<T, V, R> apply(R from) {
          return new RangeListFilter<>(ImmutableList.of(from), false, thresholdExceededLogLevel, rangeHome);
        }
      };
  }
  
  /**
   * @param rangeList should better be an immutable list
   * @param thresholdExceeded if the filter size threshold has been exceeded
   */
  public RangeListFilter(List<R> rangeList, boolean thresholdExceeded,
      Level thresholdExceededLogLevel, RangeHome<T, V, R> rangeHome) {
    Preconditions.checkArgument(!rangeList.isEmpty(), "rangeList must not be empty");
    Preconditions.checkArgument(!thresholdExceeded || rangeList.size() == 1,
        "A list which has exceede the threshold will always have exactly one element");
    this.rangeList = rangeList;
    this.thresholdExceeded = thresholdExceeded;
    this.thresholdExceededLogLevel =
        Preconditions.checkNotNull(thresholdExceededLogLevel, "thresholdExceededLogLevel");
    this.rangeHome = rangeHome;
  }

  public RangeListFilter<T, V, R> combine(RangeListFilter<T, V, R> higher, int threshold, V gapEstimate) {
    Preconditions.checkArgument(rangeList.size() <= threshold);
    Preconditions.checkArgument(higher.rangeList.size() <= threshold);
    // This trick works for nonnegative numbers.
    int cmp = rangeList.get(rangeList.size() - 1).getEnd().compareTo(
      higher.rangeList.get(0).getStart());
    int gapSignum = gapEstimate.isZero() ? 0 : 1;
    Preconditions.checkArgument((cmp < 0 & gapSignum >= 0) || (cmp == 0 & gapSignum == 0));
    int concatenatedListSize = rangeList.size() + higher.rangeList.size() + gapSignum - 1;
    if (thresholdExceeded || higher.thresholdExceeded || concatenatedListSize > threshold) {
      if (concatenatedListSize > threshold & !thresholdExceeded & !higher.thresholdExceeded) {
        logger.log(thresholdExceededLogLevel, "Exceeded threshold {0} with input sizes"
            + " {1} and {2} and gapEstimate={3}.", new Object[] {
                threshold, rangeList.size(), higher.rangeList.size(), gapEstimate});
      }
      return new RangeListFilter<T, V, R>(ImmutableList.of(rangeHome.of(rangeList.get(0).getStart(),
          higher.rangeList.get(higher.rangeList.size() - 1).getEnd())), true,
          thresholdExceededLogLevel, rangeHome);
    }
    List<R> list = new ArrayList<>(concatenatedListSize);
    if (gapSignum == 0) {
      list.addAll(rangeList);
      list.addAll(list.size() - 1, higher.rangeList);
      R lastLowerFilter = list.remove(list.size() - 1);
      assert rangeList.get(rangeList.size() - 1) == lastLowerFilter;
      list.set(rangeList.size() - 1,
          rangeHome.of(lastLowerFilter.getStart(), higher.rangeList.get(0).getEnd()));
    } else {
      list.addAll(rangeList);
      list.addAll(higher.rangeList);
    }
    return new RangeListFilter<T, V, R>(
        Collections.unmodifiableList(list), false, thresholdExceededLogLevel, rangeHome);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(rangeList, thresholdExceeded);
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof RangeListFilter)) {
      return false;
    }
    RangeListFilter<?, ?, ?> other = (RangeListFilter<?, ?, ?>) o;
    return rangeList.equals(other.rangeList) && thresholdExceeded == other.thresholdExceeded;
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public List<R> getRangeList() {
    return rangeList;
  }

  /**
   * Convenience method to extract the range covered by this filter.
   */
  public R getRange() {
    return rangeHome.of(
        rangeList.get(0).getStart(), rangeList.get(rangeList.size() - 1).getEnd());
  }
  
  public boolean isThresholdExceeded() {
    return thresholdExceeded;
  }
}
