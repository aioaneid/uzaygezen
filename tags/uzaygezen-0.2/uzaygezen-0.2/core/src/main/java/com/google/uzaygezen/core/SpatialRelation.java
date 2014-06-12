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
 * The interesting spatial relationships between two regions A and B.
 *
 * @author Daniel Aioanei
 */
public enum SpatialRelation {

  /**
   * A and B have nothing in common.
   */
  DISJOINT,
  
  /**
   * A and B orverlap, but A has at least one point that is not in B.
   */
  OVERLAPS,
  
  /**
   * A is included in B. The inclusion doesn't have to be strict.
   */
  COVERED;
}
