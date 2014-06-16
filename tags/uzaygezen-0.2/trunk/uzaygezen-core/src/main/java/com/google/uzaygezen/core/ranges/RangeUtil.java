/*
 * Copyright (C) 2012 Daniel Aioanei.
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
package com.google.uzaygezen.core.ranges;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.uzaygezen.core.AdditiveValue;
import com.google.uzaygezen.core.Pow2LengthBitSetRange;

/**
 * @author Daniel Aioanei
 */
public class RangeUtil {
  /**
   * Convenience method which sums the overlap between one orthotope and a set
   * of target orthotopes. This is a plain summation, and the result is actually
   * higher than the intersection with the union of the target orthotope when
   * they are not disjoint.
   * 
   * @return the sum of the overlap of {@code x} with each element in {@code y}.
   */
  public static <T, V extends AdditiveValue<V>, R> void overlapSum(
    List<R> x, List<? extends List<R>> y, RangeHome<T, V, R> rangeHome, V sum) {
    for (List<R> yElement : y) {
      sum.add(rangeHome.overlap(x, yElement));
    }
  }

  public static <T, V, R> List<R> toOrthotope(
    List<Pow2LengthBitSetRange> pow2LengthOrthotope, RangeHome<T, V, R> rangeHome) {
    List<R> result = new ArrayList<>(pow2LengthOrthotope.size());
    for (Pow2LengthBitSetRange bitSetRange : pow2LengthOrthotope) {
      result.add(rangeHome.toRange(bitSetRange));
    }
    return Collections.unmodifiableList(result);
  }

  public static <T, V, R extends Range<T, V>> boolean contains(List<R> orthotope, List<T> point) {
    final int n = point.size();
    Preconditions.checkArgument(orthotope.size() == n, "dimensionality mismatch");
    for (int i = 0; i < n; ++i) {
      if (!orthotope.get(i).contains(point.get(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean containsBigInteger(List<BigIntegerRange> orthotope, List<BigInteger> point) {
    final int n = point.size();
    Preconditions.checkArgument(orthotope.size() == n, "dimensionality mismatch");
    for (int i = 0; i < n; ++i) {
      if (orthotope.get(i).getStart().compareTo(point.get(i)) > 0
        || orthotope.get(i).getEnd().compareTo(point.get(i)) <= 0) {
        return false;
      }
    }
    return true;
  }

  public static boolean contains(int[][] orthotope, int[] point) {
    final int n = point.length;
    Preconditions.checkArgument(orthotope.length == n, "dimensionality mismatch");
    for (int i = 0; i < n; ++i) {
      Preconditions.checkArgument(orthotope[i].length == 2);
      if (orthotope[i][0] > point[i] | orthotope[i][1] <= point[i]) {
        return false;
      }
    }
    return true;
  }
}
