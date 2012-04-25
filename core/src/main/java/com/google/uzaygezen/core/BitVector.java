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

/**
 * Represents a fixed-size bit set. Two bit vectors are equal iff they have the
 * same size and they represent the same bit pattern. In general a bit vector
 * can talk to any other bit vector as long as they have the same size; with
 * the exception of {@link #equals}, {@link #hashCode}, {@link
 * #copyFromSection(BitVector, int)}, {@link #copySectionFrom} and {@link
 * #grayCodeRank}, all other methods accepting a {@link BitVector} parameter
 * fail if the bit vector parameter has a different size.
 *
 * @author Mehmet Akin
 * @author Daniel Aioanei
 */
public interface BitVector extends Comparable<BitVector> {
  // TODO: For methods that take [from, to) also allow from=to=size
  // to represent the empty sub-sequence as in most APIs that deal with sequences.
  
  boolean isEmpty();
  
  void set(int bitIndex);
  
  void set(int bitIndex, boolean value);
  
  void set(int fromIndex, int toIndex);
  
  void set(int fromIndex, int toIndex, boolean value); 
  
  boolean get(int bitIndex);
  
  /**
   * Copies {@link #size()} bits from {@code src}, starting with {@code
   * fromIndex}.
   * 
   * @throws IllegalArgumentException if {@code src.size() < fromIndex + size()}
   */
  void copyFromSection(BitVector src, int fromIndex);

  /**
   * Copies all {@code src.size()} bits from src into this bit vector, starting
   * with position {@code offset}.
   * 
   * @throws IllegalArgumentException if {@code offset + src.size() > size()}
   */
  void copySectionFrom(int offset, BitVector src);
  
  int length();
  
  int size();
  
  /**
   * Makes BitVector contain all zeros.
   */
  void clear();
  
  void clear(int bitIndex);
  
  void clear(int fromIndex, int toIndex);
  
  /**
   * @return  number of 1's in BitVector.
   */
  int cardinality();
  
  void flip(int bitIndex);
  
  void flip(int fromIndex, int toIndex);
  
  boolean intersects(BitVector set);
  
  /**
   * Returns the index of the first bit that is set to <code>true</code>
   * that occurs on or after the specified starting index. If no such
   * bit exists then -1 is returned.
   */
  int nextSetBit(int fromIndex);
  
  /**
   * Returns the index of the first bit that is set to <code>true</code>
   * that occurs on or after the specified starting index. If no such
   * bit exists then -1 is returned. Note that the behaviour is different from
   * the one exhibited by {@link BitSet#nextClearBit(int)}.
   */
  int nextClearBit(int fromIndex);

  /**
   * If at least one bit is {@code false} then the value is incremented by one
   * and {@code true} is returned; otherwise {@code false} is returned.
   */
  boolean increment(); 
  
  void andNot(BitVector o);
  
  void and(BitVector o);
  
  /**
   * Bitwise or with given {@code BitVector}
   * @param o A BitVector, sizes must be equal. 
   */
  void or(BitVector o);
  
  void xor(BitVector o);
  
  /**
   * Rotates by {@code count} bits to the right.
   * 
   * @param count can be negative
   */
  void rotate(int count);

  /**
   * Encodes the content of bitVector as Gray code. 
   */
  void grayCode();
  
  /**
   * Decodes the content of bitVector from Gray code to natural binary code.
   */
  void grayCodeInverse();
  
  /**
   * If empty, the are no changes. If even but not empty, it subtracts two and
   * computes the gray code. Otherwise it subtracts one and computes the gray
   * code.
   */
  void smallerEvenAndGrayCode();

  /**
   * Computes the length of the contiguous range of equal bits, starting from
   * bit zero, with the mention that all zeroes and all ones produce zero
   * instead of {@link #size()};
   * 
   * @return number of same lowest bits; the value returned always satisfies the
   * condition {@code result == 0 || (0 < result & result < size)}
   */
  int lowestDifferentBit();
  
  /**
   * @return {@code true} if the lowest {@code bitCount} bits are clear;
   * {@code false} otherwise. If called with the parameter {@code bitCount = 0}
   * it will always return {@code true}.
   */
  boolean areAllLowestBitsClear(int bitCount);
  
  /**
   * If one puts all the numbers which have the gray code inverse fixed in all
   * bit positions which are zero in {@code mu}, and they are equal in
   * those bit positions to the corresponding bits in {@code w}, and sorts
   * all those numbers increasingly, then there will be exactly one such number
   * that has the gray code inverse exactly equal to {@code w}. This method
   * computes the rank of that number in the sorted list.
   * 
   * @param mu pattern of free bits
   * @param w some gray code inverse. Note that every non-negative number is
   * the gray code inverse of exactly one number, so {@code w} can be any
   * number.
   * @throws IllegalArgumentException if {@code mu.size() != w.size()
   * || size() != mu.cardinality()}
   */
  void grayCodeRank(BitVector mu, BitVector w);
  
  /**
   * Giving the name {@code i} to the bit set represented by {@code this}, this
   * method computes the inverse function for {@link #grayCodeRank}. By knowing
   * the non-free bits of {@code i}'s gray code {@code known} and the rank of
   * {@code this} (as a gray code inverse), both with respect to a pattern
   * {@code mu}, this method computes the complete number {@code i}. If the gray
   * code of {@code i} needs to be found out as well, then {@link
   * BitVector#grayCode} can be called with this method's output (i.e., {@code
   * this}) as input.
   * 
   * @param mu pattern of free bits
   * @param known the known bits from i's gray code
   * @param r gray code rank of {@code this} with respect to {@code mu}
   */
  void grayCodeRankInverse(BitVector mu, BitVector known, BitVector r);  
  
  /**
   * Makes this BitVector equal to the other bit vector. The two bit vectors
   * must have the same size.
   * 
   * @see #copyFromSection(BitVector, int) for copying bits between bit vectors of
   * different sizes
   */
  void copyFrom(BitVector from);
  
  /**
   * Makes this BitVector equal to the given bit set.
   * 
   * @throws IllegalArgumentException iff {@code from.length() > size()}
   */
  void copyFrom(BitSet from);
  
  BitVector clone();
  
  /**
   * Returns a non-shared bit set representing the same set of bits.
   */
  BitSet toBitSet();
  
  /**
   * @return lowest 64 bit of BitVector as a long. Higher bits are ignored. The
   * result is negative iff bit 63 is set.
   */
  long toLong();

  /**
   * Like {@link #toLong}, but fails if the number doesn't fit in 64 bits. Even
   * bit vectors of size greater than 64 can succeed, if they happen not to have
   * any bits set at position 64 and beyond.
   */
  long toExactLong();
 
  /**
   * Initialises this bit vector with the little endian representation of a
   * {@code long}.
   */
  void copyFrom(long data);
  
  /**
   * See {@code BitSet#toLongArray} in Java 7. The main difference is that we
   * do not stop at the highest set bit.
   */
  long[] toLongArray();
  
  byte[] toBigEndianByteArray();
  
  /**
   * Makes this bit vector have the same bit pattern as the little-endian
   * long array, size permitting.
   * 
   * @param array must have length {@code (size() + 63) / 64}
   */
  void copyFrom(long[] array);
  
  void copyFromBigEndian(byte[] array);
  
  /**
   * @return {@code size() + 31 * toBitSet().hashCode()}
   */
  @Override
  public int hashCode();
  
  /**
   * Two bit vectors are deemed iff they have the same size and they represent
   * the same bit pattern.
   */
  @Override
  public boolean equals(Object o);
  
  /**
   * Compares the unsigned numbers having the pattern in this and the other bit
   * vectors, respectively, as the little endian representation.
   * 
   * @throws IllegalArgumentException when {@code size() != o.size()}
   */
  @Override
  public int compareTo(BitVector o);
}
