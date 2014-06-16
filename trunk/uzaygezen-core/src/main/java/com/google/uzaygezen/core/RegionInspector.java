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

import java.util.List;

/**
 * Assessor of spatial relationships.
 * 
 * @author Daniel Aioanei
 *
 * @param <T> filter type
 */
public interface RegionInspector<F, V> {
  
  int getNumberOfDimensions();
  
  /**
   * Assesses the spatial relationship of an orthotope with respect to the query
   * region. The space filling query framework doesn't know any particulars of
   * the query region. Instead it will try and find out about it piece by piece
   * by calling this method. The method can approximate the query region by
   * making it look larger, to minimise the amount of work needed to build the
   * index query corresponding to the query region in question. For instance an
   * implementation could use a histogram to better estimate the size of the
   * gaps, or approximate the surface of the query region with hypercubes that
   * have on each dimension the lowest k bits going from all zeros to all ones.
   * <p>
   * When the framework calls this method it is guaranteed that the size of the
   * index range is equal to the content of the orthotope.
   * </p>
   * 
   * @param indexRange space filling curve index range
   * @param orthotope orthotope specification. The caller might reuse this
   * parameter so no reference should be kept to it by any concrete region
   * inspector.
   * @return the immutable assessment result
   */
  Assessment<F, V> assess(Pow2LengthBitSetRange indexRange, List<Pow2LengthBitSetRange> orthotope);
}
