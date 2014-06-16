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

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.uzaygezen.core.LongContent;
import com.google.uzaygezen.core.Pow2LengthBitSetRange;
import com.google.uzaygezen.core.TestUtils;

/**
 * @author Daniel Aioanei
 */
public class RangeUtilTest {

  @Test
  public void toLongOrthotope() {
    Assert.assertEquals(ImmutableList.of(TestUtils.TWO_FOUR, TestUtils.FOUR_EIGHT), RangeUtil.toOrthotope(
      ImmutableList.of(
        new Pow2LengthBitSetRange(TestUtils.createBitVector(2, 3), 1), new Pow2LengthBitSetRange(
          TestUtils.createBitVector(4, 3), 2)), LongRangeHome.INSTANCE));
  }
  
  @Test
  public void overlapSum() {
    LongRange x0 = LongRange.of(100, 105);
    LongRange x1 = LongRange.of(103, 200);
    LongRange y0 = LongRange.of(1, 10);
    LongRange y1 = LongRange.of(0, 5);
    List<List<LongRange>> list = Lists.newArrayList();
    List<LongRange> x0y0 = ImmutableList.of(x0, y0);
    list.add(x0y0);
    list.add(x0y0);
    List<LongRange> x1y1 = ImmutableList.of(x1, y1);
    list.add(x1y1);
    LongContent actual = new LongContent(0);
    RangeUtil.overlapSum(x0y0, list, LongRangeHome.INSTANCE, actual);
    Assert.assertEquals(8 + ((x0.getEnd() - x0.getStart()) * (y0.getEnd() - y0.getStart()) << 1), actual.value());
  }
}
