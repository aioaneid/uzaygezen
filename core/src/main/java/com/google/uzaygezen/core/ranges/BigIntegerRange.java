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
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * @author Richard Garris
 * @author Daniel Aioanei
 */
public class BigIntegerRange implements Range<BigInteger, BigIntegerContent> {

  private final BigInteger start;
  private final BigInteger end;

  public BigIntegerRange(BigInteger start, BigInteger end) {
    Preconditions.checkArgument(
      end.compareTo(start) > 0 & start.signum() >= 0,
      "start must be nonnegative and less than end.");
    this.start = start;
    this.end = end;
  }

  @Override
  public BigInteger getStart() {
    return start;
  }

  @Override
  public BigInteger getEnd() {
    return end;
  }

  @Override
  public BigIntegerContent length() {
    return new BigIntegerContent(end.subtract(start));
  }

  public static BigIntegerRange of(long start, long end) {
    return new BigIntegerRange(BigInteger.valueOf(start), BigInteger.valueOf(end));
  }

  public static BigIntegerRange of(BigInteger start, BigInteger end) {
    return new BigIntegerRange(start, end);
  }

  static BigInteger overlap(List<BigIntegerRange> x, List<BigIntegerRange> y) {
    int n = x.size();
    Preconditions.checkArgument(y.size() == n, "x and y must have the same number of values");
    BigInteger overlap = BigInteger.ONE;
    // Stop early if overlap.signum() becomes zero.
    for (int i = 0; i < n & overlap.signum() != 0; ++i) {
      BigIntegerRange xRange = x.get(i);
      BigIntegerRange yRange = y.get(i);
      overlap = overlap.multiply(xRange.overlap(yRange));
    }
    return overlap;
  }

  BigInteger overlap(BigIntegerRange other) {
    if (start.compareTo(other.getEnd()) >= 0 || end.compareTo(other.getStart()) <= 0) {
      return BigInteger.ZERO;
    } else {
      if (start.compareTo(other.getStart()) < 0 || end.compareTo(other.getEnd()) > 0) {
        BigInteger x = getEnd().min(other.getEnd());
        BigInteger y = getStart().max(other.getStart());
        return x.subtract(y);
      } else {
        return end.subtract(start);
      }
    }
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(start, end);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BigIntegerRange)) {
      return false;
    }
    BigIntegerRange other = (BigIntegerRange) obj;
    return start.equals(other.start) && end.equals(other.end);
  }

  @Override
  public boolean contains(BigInteger point) {
    return start.compareTo(point) <= 0 && end.compareTo(point) > 0;
  }
}
