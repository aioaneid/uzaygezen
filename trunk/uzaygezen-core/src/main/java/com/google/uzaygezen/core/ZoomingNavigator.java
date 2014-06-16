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

/**
 * Provides the ability to visit a multidimensional space by starting at the
 * root level, where no bits are set or meaningful in any of the dimensions.
 * Since each dimension can have a different number of bits, at the highest
 * levels only some of the dimensions will be meaningful, the other dimensions
 * being represented instead as all zeroes. Implementations will usually have a
 * way to remember the specification of the multidimensional spec in the form
 * of a {@link MultiDimensionalSpec} object, but they are not required to do so.
 * 
 * @author Daniel Aioanei
 */
public interface ZoomingNavigator {
  
  /**
   * @param level {@code 0 <= level <= mMax}. When {@code level==mMax} all
   * coordinates are zero.
   * @param index the index bits calculated from level {@code mMax - 1} down to
   * to {@code level}; the remaining low order bits are guaranteed to be clear.
   * This parameter is owned by the caller and must not be modified by
   * {@link ZoomingNavigator} implementations.
   * @param p only bits from {@code max(i, mMax - m[i])} (inclusive) to {@code
   * mMax} (exclusive) are meaningful, and the other bits are guaranteed to be
   * zero. This parameter is fully owned by the caller and it must not be either
   * modified, or expected to stay the same over time.
   * @return {@code true} if we want to see the children; {@code false}
   * otherwise. At the deepest level the return value is normally ignored since
   * there are no more children to show.
   */
  /*
   * This is mostly an internal interface and it won't normally be
   * implemented by end users which instead can use the more friendly, but
   * slower, {@code List<Long>} based navigation API instead.
   */
  boolean visit(int level, BitVector index, BitVector[] p);
}
