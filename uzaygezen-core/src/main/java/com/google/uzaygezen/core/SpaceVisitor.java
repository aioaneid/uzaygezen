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
 * Provides the ability to visit a multidimensional space by visiting one
 * contiguous chunk of a space filling curve, and having the visitor itself
 * specify if the framework should zoom into the region.
 * <p>
 * Only index ranges that fill an orthotope (nothing less, nothing more) are
 * supported. For (compact) Hilbert curves, each quadrant of any order has this
 * property. For the trivial curve, any range has the same property. Once the
 * visitor has said that it doesn't want to zoom into an index range, the space
 * filling framework is guaranteed not to zoom into that region.
 * </p>
 * <p>
 * While this interface is more user friendly than {@link ZoomingNavigator}, we
 * still don't expect it to be used directly by most users. Instead navigation
 * can be more readily customised through the {@link RegionInspector}
 * abstraction. 
 * </p>
 * 
 * @see ZoomingNavigator
 * @author Daniel Aioanei
 */
public interface SpaceVisitor {
  /*
   * We could provide much stronger guarantees about the iteration order, the
   * values of the index and not only, but for now we keep the semantics simple.
   */
  
  /**
   * Visits an orthotope that is filled by the space filling curve part starting
   * from {@code 0} for the first call, or the latest {@code indexEnd} seen that
   * either returned {@literal false}, or it had a range length of {@code 1}.
   * The first call during a complete navigation is guaranteed to be for the
   * whole multidimensional space, that is, {@code indexEnd} equal to {@code
   * 1 << mSum}.
   * 
   * @param indexRange When its length is {@code 1}, the return value will be
   * ignored since there are no more children to visit anyway on the current
   * path. Its value is guaranteed to be greater than its value in all previous
   * calls that either returned {@literal false}, or they had an index range
   * length of {@code 1}.
   * 
   * @return Whether the visitor wants to zoom in.
   */
  boolean visit(Pow2LengthBitSetRange indexRange, List<Pow2LengthBitSetRange> ranges);
}
