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

import java.util.BitSet;
import java.util.Comparator;

/**
 * Comparator of bit sets that establishes over all bit sets the same total
 * order as the normal one over non-negative numbers, considering a one to one
 * correspondence between non-negative numbers and their little-endian bit set
 * representation.
 * 
 * @author Daniel Aioanei
 */
public enum BitSetComparator implements Comparator<BitSet> {
  
  INSTANCE;
  
  public int compare(BitSet o1, BitSet o2) {
    int len1 = o1.length();
    int len2 = o2.length();
    if (len1 == len2) {
      o1.xor(o2);
      int xoredLength = o1.length();
      o1.xor(o2);
      if (xoredLength == 0) {
        return 0;
      } else {
        return o1.get(xoredLength - 1) ? +1 : -1;
      }
    } else {
      return len1 - len2;
    }
  }
}
