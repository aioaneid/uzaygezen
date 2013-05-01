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

import com.google.common.base.Preconditions;

/**
 * Math utilities.
 * 
 * @author Daniel Aioanei
 */
public class MathUtils {
  
  private MathUtils() {}
  
  /**
   * Computes the greatest common divisor of two integers. Semantically
   * equivalent to {@code BigInteger.valueOf(a).gcd(BigInteger.valueOf(b))
   * .intValue()}, but normally faster.
   */
  public static int gcd(int a, int b) {
    while (b != 0) {
      int oldB = b;
      b = a % b;
      a = oldB;
    }
    return Math.abs(a);
  }

  /**
   * Produces the big-endian byte array representation of {@code value}. The
   * number must be non-negative and less than {@code 1 << 8 * byteCount)}. In
   * other words, the big endian representation, without the bit sign, must fit
   * into {@code byteCount} bytes.
   * 
   * @param byteCount the fixed size wanted for the result
   * @param value the number
   * @return the fixed size big-endian byte array representation of {@code
   * value}. The original number can be recovered via {@code
   * unsignedBigEndianBytesToNonNegativeLong(1, result)}.
   */
  public static byte[] nonNegativeLongToBigEndianBytes(int byteCount, long value) {
    Preconditions.checkArgument(value >= 0, "Negative numbers are not allowed.");
    byte[] bytes = new byte[byteCount];
    for (int i = byteCount - 1; value != 0; --i) {
      bytes[i] = (byte) (value & 0xFF);
      value >>= 8;
    }
    return bytes;
  }
  
  public static long unsignedBigEndianBytesToNonNegativeLong(byte[] bytes) {
    Preconditions.checkArgument(bytes.length < 8 || (bytes.length == 8 & bytes[0] >= 0));
    int value = 0;
    for (int i = 0; i < bytes.length; ++i) {
      value <<= 8;
      value |= bytes[i] & 0xFF;
    }
    assert value >= 0;
    return value;
  }
  
  /**
   * Computes the minimum number of bytes needed to cover {@code bitCount} bits.
   */
  public static int bitCountToByteCount(int bitCount) {
    return (bitCount + 7) >>> 3;
  }
  
  /**
   * Computes the big-endian byte array representation of the IEEE-754 8 byte
   * encoding of {@code value}.
   */
  public static byte[] doubleToBytes(double value) {
    long asLong = Double.doubleToLongBits(value);
    // Cannot use the non-negative version.
    return new byte[] {
        (byte) ((asLong >>> 56) & 0xFF), (byte) ((asLong >>> 48) & 0xFF),
        (byte) ((asLong >>> 40) & 0xFF), (byte) ((asLong >>> 32) & 0xFF),
        (byte) ((asLong >>> 24) & 0xFF), (byte) ((asLong >>> 16) & 0xFF),
        (byte) ((asLong >>> 8) & 0xFF), (byte) ((asLong >>> 0) & 0xFF)};
  }
  
  /**
   * Inverse of {@link #doubleToBytes}.
   */
  public static double bytesToDouble(byte[] bytes) {
    Preconditions.checkState(bytes.length == 8);
    long asLong = ((bytes[0] & 0xFFL) << 56) | ((bytes[1] & 0xFFL) << 48)
        | ((bytes[2] & 0xFFL) << 40) | ((bytes[3] & 0xFFL) << 32)
        | ((bytes[4] & 0xFFL) << 24) | ((bytes[5] & 0xFFL) << 16)
        | ((bytes[6] & 0xFFL) << 8) | ((bytes[7] & 0xFFL) << 0);
    return Double.longBitsToDouble(asLong);
  }
  
  /**
   * Unfortunately there is no Byte.numberOfLeadingZeros method.
   * 
   * @return Number of leading zeroes (up to 8) of a byte.
   */
  public static int numberOfLeadingZeros(byte b) {
    return Integer.numberOfLeadingZeros(b & 0xFF) - 24;
  }
}
