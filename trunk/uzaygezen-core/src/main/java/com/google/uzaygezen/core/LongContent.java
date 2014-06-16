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
package com.google.uzaygezen.core;

import java.math.BigInteger;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * @author Daniel Aioanei
 */
public class LongContent implements Content<LongContent> {

  private long v;

  public LongContent(long v) {
    Preconditions.checkArgument(v >= 0);
    this.v = v;
  }
  
  public long value() {
    return v;
  }
  
  @Override
  public int compareTo(LongContent o) {
    return Long.compare(v, o.v);
  }

  @Override
  public void add(LongContent other) {
    v += other.v;
  }
  
  public LongContent clone() {
    return new LongContent(v);
  }

  @Override
  public int hashCode() {
    return ~Objects.hashCode(v);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LongContent)) {
      return false;
    }
    LongContent other = (LongContent) o;
    return v == other.v;
  }

  /**
   * If n is larger than 62 then the value becomes zero. This behaviour is
   * different from that of Java's shifting operators, but is similar to
   * that of {#link {@link BigInteger#shiftRight(int)}.
   */
  @Override
  public void shiftRight(int n) {
    Preconditions.checkArgument(n >= 0);
    // Since v is positive, it only has up to 63 useful bits.
    v >>>= Math.min(n, 63);
  }

  @Override
  public boolean isZero() {
    return v == 0;
  }

  @Override
  public boolean isOne() {
    return v == 1;
  }
  
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
