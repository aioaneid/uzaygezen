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
 * @author Daniel Aioanei
 */
public class BitVectorMath {
  
  private BitVectorMath() {}
  
  /**
   * Splits the bits from {@code bs} into {@code result} by putting the
   * lowest bits at the end of {@code result} and so on until the highest bits
   * are put at the beginning of {@code result}. This is similar to the big
   * endian representation of numbers, only that each digit has a potentially
   * different size. The bit set length must be equal to {@code
   * sum(elementLengths)}.
   * 
   * @param bs little endian bit set
   * @param result output
   */
  public static void split(BitVector bs, BitVector[] result) {
    int sum = 0;
    for (BitVector bv : result) {
      sum += bv.size();
    }
    Preconditions.checkArgument(sum == bs.size(), "size sum does not match");
    int startIndex = 0;
    for (int i = result.length; --i >= 0; ) {
      result[i].copyFromSection(bs, startIndex);
      startIndex += result[i].size();
    }
    Preconditions.checkArgument(startIndex >= bs.length(), "bit length is too high");
  }
}
