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
 * Abstraction for the most common operations involving a space filling curve in
 * a multi-dimensional space.
 *
 * @author Daniel Aioanei
 */
public interface SpaceFillingCurve extends IndexCalculator {

  /**
   * Given the index of a point, computes the coordinates of the point. The
   * output array must have length exactly equal to the dimensionality of the
   * space, and all its elements must be non-null.
   * 
   * @param index the index of a to-be-located point. Implementations must not
   * modify this parameter in any way.
   * @param p output
   */
  void indexInverse(BitVector index, BitVector[] p);
  
  /**
   * Shows the space to the visitor, and the process is guided by the visitor
   * through its {@code boolean} returning method. Thus the visitor has a double
   * role as a driver of the navigation since at each call it decides if it
   * wants to zoom in or not. The visitor is guaranteed to see the space in
   * index order in-depth traversal.
   * 
   * @param visitor driver-visitor
   */
  void accept(ZoomingNavigator visitor);
}
