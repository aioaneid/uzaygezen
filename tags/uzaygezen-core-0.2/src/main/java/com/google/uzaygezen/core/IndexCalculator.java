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
 * Abstraction for space filling curve index calculators.
 *
 * @author Daniel Aioanei
 */
public interface IndexCalculator {

  /**
   * Provides the specification of the multidimensional space on which this
   * space filling curve operates.
   * 
   * @return information about the dimensionality of the space
   */
  MultiDimensionalSpec getSpec();

  /**
   * Computes the index on the curve of the multidimensional point {@code p}.
   * Only the highest sum(i=mMax..minLevel, max(0, m[i] - minLevel)) bits
   * are computed, and the lowest remaining bits are cleared.
   * 
   * @param p point in the multidimensional space. Implementations are not
   * allowed to modify anything in this parameter.
   * @param minLevel the level between 0 and mMax inclusive, inclusive, up to
   * which the index bits should be calculated. If equal to mMax, then the
   * result is always zero since there are no dimensions that have the mMax bit.
   * To get the full compact index specify {@code minLevel = 0}.
   * @param index output
   */
  void index(BitVector[] p, int minLevel, BitVector index);
}
