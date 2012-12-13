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

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * @author Daniel Aioanei
 */
public class MultiDimensionalSpecTest {

  private static final List<Integer> list = ImmutableList.of(4, 0, 2);
  private static final MultiDimensionalSpec multiDimensionalSpec = new MultiDimensionalSpec(list);

  @Test
  public void multiDimensionalSpecDissalowsNegativeBits() {
    try {
      new MultiDimensionalSpec(ImmutableList.of(-1));
      Assert.fail("Negative bits shouldn't be allowed.");
    } catch (IllegalArgumentException ex) {
      Assert.assertNotNull(ex.getMessage());
    }
  }

  /**
   * Conventionally called "m" in comments.
   */
  @Test
  public void getBitsPerDimension() {
    Assert.assertEquals(list, multiDimensionalSpec.getBitsPerDimension());
  }

  /**
   * Conventionally called "mSum" in comments.
   */
  @Test
  public void sumBitsPerDimension() {
    Assert.assertEquals(6, multiDimensionalSpec.sumBitsPerDimension());
  }

  /**
   * Conventionally called "mMax" in comments.
   */
  @Test
  public void maxBitsPerDimension() {
    Assert.assertEquals(4, multiDimensionalSpec.maxBitsPerDimension());
  }
}
