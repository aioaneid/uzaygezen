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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Aioanei
 */
public class AssessmentTest {

  @Test
  public void makeDisjoint() {
    Assessment<Void, Integer> assessment = Assessment.makeDisjoint(10);
    Assert.assertEquals(10, assessment.getEstimate().intValue());
    Assert.assertSame(SpatialRelation.DISJOINT, assessment.getOutcome());
  }

  @Test
  public void makeOverlaps() {
    Assessment<Void, Integer> assessment = Assessment.makeOverlaps(0);
    Assert.assertSame(SpatialRelation.OVERLAPS, assessment.getOutcome());
  }

  @Test
  public void makeCovered() {
    Integer ten = new Integer(10);
    Assessment<Integer, Integer> assessment = Assessment.makeCovered(ten, true, 0);
    Assert.assertSame(SpatialRelation.COVERED, assessment.getOutcome());
    Assert.assertSame(ten, assessment.getFilter());
    Assert.assertTrue(assessment.isPotentialOverSelectivity());
  }
}
