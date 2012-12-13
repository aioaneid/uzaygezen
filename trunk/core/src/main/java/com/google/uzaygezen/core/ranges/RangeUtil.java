package com.google.uzaygezen.core.ranges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.uzaygezen.core.AdditiveValue;
import com.google.uzaygezen.core.Pow2LengthBitSetRange;

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
}
