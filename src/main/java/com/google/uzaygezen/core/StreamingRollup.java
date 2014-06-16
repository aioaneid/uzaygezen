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

import java.util.Iterator;

/**
 * Streaming aggregator that rolls up all the values into a tree.
 * 
 * @author Daniel Aioanei
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface StreamingRollup<K, V extends AdditiveValue<V>> {

  /**
   * The rows must come in nested groups. For instance sorting achieves that,
   * but it is not necessary.
   * 
   * @param leafPath identifies the location of the node onto which the
   * value is added, starting from the root. An empty path represents the root
   * itself. If the node doesn't exist, it will be created.
   * 
   * @param value
   */
  public void feedRow(Iterator<K> leafPath, V value);

  /**
   * Returns the tree representing the rolled-up, aggregated data. If there is
   * no data then {@literal null} is returned.
   */
  public MapNode<K, V> finish();
}
