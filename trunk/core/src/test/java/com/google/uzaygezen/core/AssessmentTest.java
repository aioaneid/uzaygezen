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



import junit.framework.TestCase;

/**
 * @author Daniel Aioanei
 */
public class AssessmentTest extends TestCase {

  public void testMakeDisjoint() {
    Assessment<Integer> assessment = Assessment.makeDisjoint(10);
    assertEquals(10, assessment.getEstimate());
    assertSame(SpatialRelation.DISJOINT, assessment.getOutcome());
  }

  public void testMakeOverlaps() {
    Assessment<Integer> assessment = Assessment.makeOverlaps();
    assertSame(SpatialRelation.OVERLAPS, assessment.getOutcome());
  }

  public void testMakeCovered() {
    final Integer ten = new Integer(10);
    Assessment<Integer> assessment = Assessment.makeCovered(ten, true);
    assertSame(SpatialRelation.COVERED, assessment.getOutcome());
    assertSame(ten, assessment.getFilter());
    assertTrue(assessment.isPotentialOverSelectivity());
  }
}
