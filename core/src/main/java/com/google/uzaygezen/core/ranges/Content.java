package com.google.uzaygezen.core.ranges;

import com.google.uzaygezen.core.AdditiveValue;

public interface Content<V> extends AdditiveValue<V> {

  void shiftRight(int n);
  
  boolean isOne();
}
