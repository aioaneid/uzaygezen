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

import java.util.List;

import com.google.uzaygezen.core.Pow2LengthBitSetRange;

/**
 * @author Daniel Aioanei
 */
public interface RangeHome<T, V, R> {

    R of(T start, T end);
    
    R toRange(Pow2LengthBitSetRange bitSetRange);
    
    V overlap(List<R> x, List<R> y);
}
