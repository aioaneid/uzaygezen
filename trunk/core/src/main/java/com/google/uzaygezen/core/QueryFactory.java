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

import com.google.common.base.Supplier;

/**
 * Supplier of queries. Multiple calls to the {@link #get()} method can produce
 * different query objects if other mutating methods are called on the object in
 * between.
 * 
 * @author Daniel Aioanei
 *
 * @param <T> filter type
 */
public interface QueryFactory<F, R> extends Supplier<Query<F,R>> {}
