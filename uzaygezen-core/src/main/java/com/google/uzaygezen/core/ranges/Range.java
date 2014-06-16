/*
 * Copyright (C) 2012 Daniel Aioanei.
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
package com.google.uzaygezen.core.ranges;

/**
 * Non-empty interval with non-negative {@code long} inclusive start and
 * exclusive end.
 * 
 * @author Daniel Aioanei
 */
public interface Range<T, V> extends Measurable<V> {

  /**
   * @return the inclusive start of the interval
   */
  T getStart();

  /**
   * @return the exclusive end of the interval
   */
  T getEnd();

  boolean contains(T point);
}
