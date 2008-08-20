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

import com.google.common.base.Function;

/**
 * Commonly used bit vector factories.
 * 
 * @author Daniel Aioanei
 */
public enum BitVectorFactories implements Function<Integer, BitVector> {

  OPTIMAL {
    @Override
    public BitVector apply(Integer from) {
      int size = from.intValue();
      if (size <= 64) {
        return new LongBitVector(size);
      } else {
        return new BitSetBackedBitVector(size);
      }
    }
  }, SLOW {
    @Override
    public BitVector apply(Integer from) {
      return new BitSetBackedBitVector(from);
    }
  };
  
  /**
   * TODO: Remove when http://bugs.sun.com/view_bug.do?bug_id=6330385 is fixed.
   */
  public abstract BitVector apply(Integer input);
}
