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

import com.google.common.base.Preconditions;

/**
 * Math utilities.
 * 
 * @author Daniel Aioanei
 */
public class BigIntegerMath {

  private BigIntegerMath() {}

  public static byte[] nonnegativeBigIntegerToBigEndianByteArrayForBitSize(BigInteger s, int size) {
    // It will have at least one sign bit.
    byte[] b = s.toByteArray();
    Preconditions.checkArgument(b[0] >= 0, "%s is negative", s);
    int n = MathUtils.bitCountToByteCount(size);
    Preconditions.checkArgument(b.length <= n + 1, "%s has bits that are two high", s);
    int start;
    if (b.length == n + 1) {
      Preconditions.checkArgument(
        size == n << 3, "A BigInteger's big endian length of %s cannot be copied into size=%s.",
          b.length, size);
      Preconditions.checkArgument(b[0] == 0, "The first byte is a sign bit, and it must be zero.");
      start = 1;
    } else {
      start = 0;
    }
    final byte[] array;
    if (b.length != n) {
      array = new byte[n];
      int len = b.length - start;
      System.arraycopy(b, start, array, n - len, len);
    } else {
      // Optimisation, but this branch is not otherwise necessary. 
      array = b;
    }
    return array;
  }
}
