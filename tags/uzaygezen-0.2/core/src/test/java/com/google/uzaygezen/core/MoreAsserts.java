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

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;

import com.google.common.collect.ImmutableMultiset;

/**
 * Contains additional assertion methods not found in JUnit.
 */
public final class MoreAsserts {

  private MoreAsserts() { }

  /**
   * Asserts that {@code expectedRegex} matches any substring of {@code actual}
   * and fails with {@code message} if it does not.  The Matcher is returned in
   * case the test needs access to any captured groups.  Note that you can also
   * use this for a literal string, by wrapping your expected string in
   * {@link Pattern#quote}.
   */
  public static MatchResult assertContainsRegex(
      String message, String expectedRegex, String actual) {
    if (actual == null) {
      failNotContains(message, expectedRegex, null);
    }
    Matcher matcher = getMatcher(expectedRegex, actual);
    if (!matcher.find()) {
      failNotContains(message, expectedRegex, actual);
    }
    return matcher;
  }

  /**
   * Variant of {@link #assertContainsRegex(String,String,String)} using a
   * generic message.
   */
  public static MatchResult assertContainsRegex(
      String expectedRegex, String actual) {
    return assertContainsRegex(null, expectedRegex, actual);
  }

  /**
   * Asserts that {@code actual} contains precisely the elements
   * {@code expected}, in any order.  Both collections may contain
   * duplicates, and this method will only pass if the quantities are
   * exactly the same.
   */
  public static void assertContentsAnyOrder(
      String message, Iterable<?> actual, Object... expected) {
    Assert.assertEquals(message,
        ImmutableMultiset.copyOf(expected), ImmutableMultiset.copyOf(actual));
  }

  /**
   * Variant of {@link #assertContentsAnyOrder(String,Iterable,Object...)}
   * using a generic message.
   */
  public static void assertContentsAnyOrder(
      Iterable<?> actual, Object... expected) {
    assertContentsAnyOrder((String) null, actual, expected);
  }

  /**
   * Utility for testing equals() and hashCode() results at once.
   * Tests that lhs.equals(rhs) matches expectedResult, as well as
   * rhs.equals(lhs).  Also tests that hashCode() return values are
   * equal if expectedResult is true.  (hashCode() is not tested if
   * expectedResult is false, as unequal objects can have equal hashCodes.)
   *
   * @param lhs An Object for which equals() and hashCode() are to be tested.
   * @param rhs As lhs.
   * @param expectedResult True if the objects should compare equal,
   *   false if not.
   */
  public static void checkEqualsAndHashCodeMethods(
      String message, Object lhs, Object rhs, boolean expectedResult) {

    if ((lhs == null) && (rhs == null)) {
      Assert.assertTrue(
          "Your check is dubious...why would you expect null != null?",
          expectedResult);
      return;
    }

    if ((lhs == null) || (rhs == null)) {
      Assert.assertFalse(
          "Your check is dubious...why would you expect an object "
          + "to be equal to null?", expectedResult);
    }

    if (lhs != null) {
      Assert.assertEquals(message, expectedResult, lhs.equals(rhs));
    }
    if (rhs != null) {
      Assert.assertEquals(message, expectedResult, rhs.equals(lhs));
    }

    if (expectedResult) {
      String hashMessage =
          "hashCode() values for equal objects should be the same";
      if (message != null) {
        hashMessage += ": " + message;
      }
      Assert.assertTrue(hashMessage, lhs.hashCode() == rhs.hashCode());
    }
  }

  /**
   * Variant of
   * {@link #checkEqualsAndHashCodeMethods(String, Object, Object, boolean)}
   * using a generic message.
   */
  public static void checkEqualsAndHashCodeMethods(Object lhs, Object rhs,
                                             boolean expectedResult) {
    checkEqualsAndHashCodeMethods((String) null, lhs, rhs, expectedResult);
  }

  /**
   * Fails a test with the given message and includes as its cause the given
   * {@link Throwable}, complete with its stack trace. This method is not
   * normally necessary; typically it is best to declare that your test method
   * throws any possible exceptions and to let JUnit handle them and mark the
   * test as an error. This method exists for cases in which it is necessary to
   * provide some context to the exception beyond that offered by its stack
   * trace -- perhaps the current element of a loop or the value of a random
   * seed.
   *
   * @param cause the cause of the failure
   * @param errorMessageFormat the {@link java.util.Formatter format string} for
   *        the desired error message
   * @param errorMessageArgs the arguments referenced by the format specifiers
   *        in {@code errorMessageFormat}.
   */
  public static void failWithThrowable(Throwable cause,
      String errorMessageFormat, Object... errorMessageArgs) {
    failWithThrowable(cause,
        String.format(errorMessageFormat, errorMessageArgs));
  }

  private static Matcher getMatcher(String expectedRegex, String actual) {
    Pattern pattern = Pattern.compile(expectedRegex);
    return pattern.matcher(actual);
  }

  private static void failNotContains(
      String message, String expectedRegex, String actual) {
    String actualDesc = (actual == null) ? "null" : ('<' + actual + '>');
    failWithMessage(message, "expected to contain regex:<" + expectedRegex
        + "> but was:" + actualDesc);
  }

  private static void failWithMessage(String userMessage, String ourMessage) {
    Assert.fail((userMessage == null)
        ? ourMessage
        : userMessage + ' ' + ourMessage);
  }
}
