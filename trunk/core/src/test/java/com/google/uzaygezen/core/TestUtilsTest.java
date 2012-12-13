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

import java.util.BitSet;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.uzaygezen.core.TestUtils.IntArrayCallback;

/**
 * @author Daniel Aioanei
 */
public class TestUtilsTest {

  @Test
  public void unsignedIntToLittleEndianBitSet() {
    for (int i = -1024; i < 1024; ++i) {
      BitSet bs = TestUtils.unsignedIntToLittleEndianBitSet(i);
      for (int j = 0; j < 32; ++j) {
        boolean expected = ((i >>> j) & 0x1) != 0;
        Assert.assertEquals(expected, bs.get(j));
      }
    }
  }

  @Test
  public void generate() {
    IntArrayCallback mock = EasyMock.createMock(IntArrayCallback.class);
    final int maxSum = 10;
    mock.call(EasyMock.aryEq(new int[0]));
    for (int i = 0; i <= maxSum; ++i) {
      mock.call(EasyMock.aryEq(new int[] {i}));
      for (int j = 0; j <= maxSum - i; ++j) {
        mock.call(EasyMock.aryEq(new int[] {i, j}));
        for (int k = 0; k <= maxSum - i - j; ++k) {
          mock.call(EasyMock.aryEq(new int[] {i, j, k}));
        }
      }
    }
    EasyMock.replay(mock);
    TestUtils.generateSpec(3, maxSum, mock);
    EasyMock.verify(mock);
  }
  
  @Test
  public void intArrayComparator() {
    Assert.assertEquals(0, TestUtils.IntArrayComparator.INSTANCE.compare(new int[0], new int[0]));
    Assert.assertTrue(TestUtils.IntArrayComparator.INSTANCE.compare(new int[0], new int[1]) < 0);
    Assert.assertTrue(TestUtils.IntArrayComparator.INSTANCE.compare(new int[1], new int[0]) > 0);
    Assert.assertTrue(
        TestUtils.IntArrayComparator.INSTANCE.compare(new int[] {1, 2}, new int[] {2, 2}) < 0);
    Assert.assertEquals(
        0, TestUtils.IntArrayComparator.INSTANCE.compare(new int[] {2, 2}, new int[] {2, 2}));
    Assert.assertTrue(
        TestUtils.IntArrayComparator.INSTANCE.compare(new int[] {1}, new int[] {1, 2}) < 0);
    Assert.assertTrue(
        TestUtils.IntArrayComparator.INSTANCE.compare(new int[] {1, 2}, new int[] {1}) > 0);
  }
}
