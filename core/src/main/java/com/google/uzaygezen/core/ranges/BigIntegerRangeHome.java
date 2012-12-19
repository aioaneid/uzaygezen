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

import com.google.uzaygezen.core.BigIntegerContent;
import com.google.uzaygezen.core.Pow2LengthBitSetRange;

/**
 * @author Richard Garris
 * @author Daniel Aioanei
 */
public enum BigIntegerRangeHome implements
  RangeHome<BigInteger, BigIntegerContent, BigIntegerRange> {

  INSTANCE;

  @Override
  public BigIntegerRange of(BigInteger start, BigInteger end) {
    return BigIntegerRange.of(start, end);
  }

  @Override
  public BigIntegerRange toRange(Pow2LengthBitSetRange bitSetRange) {
    BigInteger inclusiveStart = bitSetRange.getStart().toBigInteger();
    BigInteger delta = BigInteger.ONE.shiftLeft(bitSetRange.getLevel());
    BigInteger exclusiveEnd = inclusiveStart.add(delta);
    return BigIntegerRange.of(inclusiveStart, exclusiveEnd);
  }

  @Override
  public BigIntegerContent overlap(List<BigIntegerRange> x, List<BigIntegerRange> y) {
    BigInteger overlap = BigIntegerRange.overlap(x, y);
    return new BigIntegerContent(overlap);
  }
}
