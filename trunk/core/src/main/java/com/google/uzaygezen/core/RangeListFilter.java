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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * Associates a list of ranges with a bool specifying if the list of ranges has
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
public class RangeListFilter {

  private static final Logger logger = Logger.getLogger(RangeListFilter.class.getName());
  
  private final List<LongRange> rangeList;
  private final boolean thresholdExceeded;
  private final Level thresholdExceededLogLevel;
  
  public static final Predicate<RangeListFilter> IS_THRESHOLD_EXCEEDED =
      new Predicate<RangeListFilter>() {
        @Override
        public boolean apply(RangeListFilter from) {
          return from.thresholdExceeded;
        }
  };
  
  public static final Function<RangeListFilter, List<LongRange>> RANGE_LIST_EXTRACTOR =
      new Function<RangeListFilter, List<LongRange>>() {
        @Override
        public List<LongRange> apply(RangeListFilter from) {
          return from.getRangeList();
        }
  };
  
  public static Function<LongRange, RangeListFilter> creator(
      final Level thresholdExceededLogLevel) {
      return new Function<LongRange, RangeListFilter>() {
        @Override
        public RangeListFilter apply(LongRange from) {
          return new RangeListFilter(ImmutableList.of(from), false, thresholdExceededLogLevel);
        }
      };
  }
  
  /**
   * @param rangeList should better be an immutable list
   * @param thresholdExceeded if the filter size threshold has been exceeded
   */
  public RangeListFilter(List<LongRange> rangeList, boolean thresholdExceeded,
      Level thresholdExceededLogLevel) {
    Preconditions.checkArgument(!rangeList.isEmpty(), "rangeList must not be empty");
    Preconditions.checkArgument(!thresholdExceeded || rangeList.size() == 1,
        "A list which has exceede the threshold will always have exactly one element");
    this.rangeList = rangeList;
    this.thresholdExceeded = thresholdExceeded;
    this.thresholdExceededLogLevel =
        Preconditions.checkNotNull(thresholdExceededLogLevel, "thresholdExceededLogLevel");
  }

  public RangeListFilter combine(RangeListFilter higher, int threshold, long gapEstimate) {
    Preconditions.checkArgument(rangeList.size() <= threshold);
    Preconditions.checkArgument(higher.rangeList.size() <= threshold);
    // This trick works for nonnegative numbers.
    int cmp = Long.signum(rangeList.get(rangeList.size() - 1).getEnd()
        - Long.valueOf(higher.rangeList.get(0).getStart()));
    int gapSignum = Long.signum(gapEstimate);
    Preconditions.checkArgument((cmp == -1 && gapSignum >= 0) || (cmp == 0 && gapSignum == 0));
    int concatenatedListSize = rangeList.size() + higher.rangeList.size() + gapSignum - 1;
    if (thresholdExceeded || higher.thresholdExceeded || concatenatedListSize > threshold) {
      if (concatenatedListSize > threshold & !thresholdExceeded & !higher.thresholdExceeded) {
        logger.log(thresholdExceededLogLevel, "Exceeded threshold {0} with input sizes"
            + " {1} and {2} and gapEstimate={3}.", new Object[] {
                threshold, rangeList.size(), higher.rangeList.size(), gapEstimate});
      }
      return new RangeListFilter(ImmutableList.of(LongRange.of(rangeList.get(0).getStart(),
          higher.rangeList.get(higher.rangeList.size() - 1).getEnd())), true,
          thresholdExceededLogLevel);
    }
    List<LongRange> list = new ArrayList<LongRange>(concatenatedListSize);
    if (gapSignum == 0) {
      list.addAll(rangeList);
      list.addAll(list.size() - 1, higher.rangeList);
      LongRange lastLowerFilter = list.remove(list.size() - 1);
      assert rangeList.get(rangeList.size() - 1) == lastLowerFilter;
      list.set(rangeList.size() - 1,
          LongRange.of(lastLowerFilter.getStart(), higher.rangeList.get(0).getEnd()));
    } else {
      list.addAll(rangeList);
      list.addAll(higher.rangeList);
    }
    return new RangeListFilter(
        Collections.unmodifiableList(list), false, thresholdExceededLogLevel);
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
    RangeListFilter other = (RangeListFilter) o;
    return rangeList.equals(other.rangeList) && thresholdExceeded == other.thresholdExceeded;
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public List<LongRange> getRangeList() {
    return rangeList;
  }

  /**
   * Convenience method to extract the range covered by this filter.
   */
  public LongRange getRange() {
    return LongRange.of(
        rangeList.get(0).getStart(), rangeList.get(rangeList.size() - 1).getEnd());
  }
  
  public boolean isThresholdExceeded() {
    return thresholdExceeded;
  }
}
