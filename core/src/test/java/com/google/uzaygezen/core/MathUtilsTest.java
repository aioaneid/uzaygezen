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



import java.math.BigInteger;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Aioanei
 */
public class MathUtilsTest {
  
  @Test
  public void gcd() {
    for (int i = -64; i < 64; ++i) {
      for (int j = -64; j < 64; ++j) {
        checkGcd(0, i, j);
        checkGcd(Integer.MAX_VALUE, i, j);
        checkGcd(Integer.MIN_VALUE, i, j);
      }
    }
  }

  @Test
  public void toFixedSizeByteArray() {
    // Testing up to 5 bytes.
    for (int i = 0; i < 256 * 5 * 8; ++i) {
      for (int byteCount = (i + 7) / 8; byteCount < 9; ++byteCount) {
        Assert.assertEquals(i, MathUtils.unsignedBigEndianBytesToNonNegativeLong(
            MathUtils.nonNegativeLongToBigEndianBytes(byteCount, i)));
      }
    }
  }

  @Test
  public void bitCountToByteCount() {
    Assert.assertEquals(0, MathUtils.bitCountToByteCount(0));
    Assert.assertEquals(1, MathUtils.bitCountToByteCount(1));
    Assert.assertEquals(1, MathUtils.bitCountToByteCount(8));
    Assert.assertEquals(2, MathUtils.bitCountToByteCount(9));
    Assert.assertEquals(2, MathUtils.bitCountToByteCount(16));
    Assert.assertEquals(3, MathUtils.bitCountToByteCount(17));
  }

  @Test
  public void doubleBytesConversion() {
    Random random = new Random(TestUtils.SEED);
    for (int i = 0; i < 10; ++i) {
      double x = random.nextDouble();
      byte[] xBytes = MathUtils.doubleToBytes(x);
      double actual = MathUtils.bytesToDouble(xBytes);
      Assert.assertEquals(x, actual, 0);
    }
  }
  
  private void checkGcd(int offset, int i, int j) {
    BigInteger iAsBigInt = BigInteger.valueOf(offset + i);
    BigInteger jAsBigInt = BigInteger.valueOf(offset + j);
    BigInteger gcd = iAsBigInt.gcd(jAsBigInt);
    Assert.assertEquals(MathUtils.gcd(offset + i, offset + j), gcd.intValue());
  }
  
  @Test
  public void numberOfLeadingZerosOfByte() {
    for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
      byte b = (byte) i;
      int actual = MathUtils.numberOfLeadingZeros(b);
      int expected = b < 0 ? 0 : (b == 0 ? 8 : Integer.numberOfLeadingZeros(i) - 24);
      Assert.assertEquals(expected, actual);
    }
  }
}
