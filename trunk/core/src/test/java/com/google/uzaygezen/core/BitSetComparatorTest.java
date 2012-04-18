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

import java.util.BitSet;

/**
 * @author Daniel Aioanei
 */
public class BitSetComparatorTest extends TestCase {
  
  public void testOrderIsSameAsNormalNumberOrdering() {
    BitSet iAsBitSet = new BitSet();
    BitSet jAsBitSet = new BitSet();
    for (int i = 0; i < 64; ++i) {
      jAsBitSet.clear();
      for (int j = 0; j < 64; ++j) {
        int cmp = BitSetComparator.INSTANCE.compare(iAsBitSet, jAsBitSet);
        assertEquals(Math.signum(i - j), Math.signum(cmp));
        BitSetMath.increment(jAsBitSet);
      }
      BitSetMath.increment(iAsBitSet);
    }
  }
}