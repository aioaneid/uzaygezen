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
 * Value that can be added to similar values.
 * 
 * @author Daniel Aioanei
 * 
 * @param <V> value type
 */
public interface AdditiveValue<V> extends Comparable<V> {
  
  /**
   * Adding two values must result in a value that compares greater than or
   * equal to both input values. There must be a (set of) special neutral values
   * for addition, and whenever two values are added and neither of them is a
   * neutral value, the result must compare greater than either operand.
   * @param other
   */
  void add(V other);
  
  boolean isZero();
  
  /**
   * Clones this value and produces another one that can be modified
   * independently.
   */
  V clone();
}
