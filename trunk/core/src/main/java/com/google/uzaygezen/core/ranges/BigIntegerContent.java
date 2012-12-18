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

import com.google.common.base.Preconditions;
import com.google.uzaygezen.core.Content;

/**
 * @author Richard Garris
 * @author Daniel Aioanei
 */
public class BigIntegerContent implements Content<BigIntegerContent> {

  private BigInteger value;

  public BigIntegerContent(BigInteger v) {
    Preconditions.checkArgument(v.signum() >= 0);
    this.value = v;
  }

  @Override
  public void add(BigIntegerContent other) {
    value = value.add(other.value);
  }

  @Override
  public boolean isZero() {
    return value.signum() == 0;
  }

  @Override
  public int compareTo(BigIntegerContent o) {
    return value.compareTo(o.value);
  }

  @Override
  public void shiftRight(int n) {
    value = value.shiftRight(n);
  }

  @Override
  public boolean isOne() {
    return value.equals(BigInteger.ONE);
  }

  @Override
  public int hashCode() {
    return ~value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BigIntegerContent)) {
      return false;
    }
    BigIntegerContent other = (BigIntegerContent) obj;
    return value.equals(other.value);
  }

  @Override
  public BigIntegerContent clone() {
    return new BigIntegerContent(value);
  }
}
