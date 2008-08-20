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



import junit.framework.TestCase;

import java.math.BigInteger;
import java.util.Random;

/**
 * @author Daniel Aioanei
 */
public class MathUtilsTest extends TestCase {
  
  public void testGcd() {
    for (int i = -64; i < 64; ++i) {
      for (int j = -64; j < 64; ++j) {
        checkGcd(0, i, j);
        checkGcd(Integer.MAX_VALUE, i, j);
        checkGcd(Integer.MIN_VALUE, i, j);
      }
    }
  }

  public void testToFixedSizeByteArray() {
    // Testing up to 5 bytes.
    for (int i = 0; i < 256 * 5 * 8; ++i) {
      for (int byteCount = (i + 7) / 8; byteCount < 9; ++byteCount) {
        assertEquals(i, MathUtils.unsignedBigEndianBytesToNonNegativeLong(
            MathUtils.nonNegativeLongToBigEndianBytes(byteCount, i)));
      }
    }
  }

  public void testBitCountToByteCount() {
    assertEquals(0, MathUtils.bitCountToByteCount(0));
    assertEquals(1, MathUtils.bitCountToByteCount(1));
    assertEquals(1, MathUtils.bitCountToByteCount(8));
    assertEquals(2, MathUtils.bitCountToByteCount(9));
    assertEquals(2, MathUtils.bitCountToByteCount(16));
    assertEquals(3, MathUtils.bitCountToByteCount(17));
  }

  public void testDoubleBytesConversion() {
    Random random = new Random(TestUtils.SEED);
    for (int i = 0; i < 10; ++i) {
      double x = random.nextDouble();
      byte[] xBytes = MathUtils.doubleToBytes(x);
      double actual = MathUtils.bytesToDouble(xBytes);
      assertEquals(x, actual);
    }
  }
  
  private void checkGcd(int offset, int i, int j) {
    BigInteger iAsBigInt = BigInteger.valueOf(offset + i);
    BigInteger jAsBigInt = BigInteger.valueOf(offset + j);
    BigInteger gcd = iAsBigInt.gcd(jAsBigInt);
    assertEquals(MathUtils.gcd(offset + i, offset + j), gcd.intValue());
  }
}
