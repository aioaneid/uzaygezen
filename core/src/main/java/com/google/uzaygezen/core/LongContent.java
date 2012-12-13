package com.google.uzaygezen.core;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.uzaygezen.core.ranges.Content;

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

  @Override
  public void shiftRight(int n) {
    Preconditions.checkArgument(n >= 0);
    v >>>= n;
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
