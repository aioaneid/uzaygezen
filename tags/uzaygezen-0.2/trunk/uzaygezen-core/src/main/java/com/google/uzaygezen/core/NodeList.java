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
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * {@code List} that provides access to the nodes in its internal
 * representation. It includes methods, such as {@link #getNode}, that return a
 * {@link Node} instance. That {@code Node} remains valid, even after structural
 * changes the list. A subsequent {@link Node#remove} call, for example, would
 * remove the node's element, even if the element's position within the list had
 * changed.
 * 
 * <p>The {@code NodeList} has several methods that correspond to {@link List}
 * methods, except that they return a node: {@link #getNode} is similar to
 * {@link List#get}, {@link #nodeWith} is similar to {@link List#indexOf}, and
 * so on.
 * 
 * <p>A node supports many operations, such as retrieving the element value,
 * finding the previous or next node, removing the node, or adding a new
 * element before or after the node. Some {@code Node} methods remain valid
 * after the corresponding element has been removed, while others are
 * unsupported. See the {@link Node} documentation for more information.
 * 
 * <p>A node's element changes only when the {@link List#set},
 * {@link ListIterator#set}, or {@link Node#set} methods are called. Note that
 * the {@link java.util.Collections#sort(List)} and
 * {@link java.util.Collections#sort(List, java.util.Comparator)} methods call
 * {@link ListIterator#set} internally, meaning that sorting will update the
 * elements of existing nodes but won't change the node positions. Instead, use
 * {@link NodeLists#sort(NodeList) or
 * NodeLists#sort(NodeList, java.util.Comparator)} to update the node
 * ordering.
 *
 * <p>Two successive calls to {@code getNode(i)}, without any list changes
 * in between, may or may not return the same {@code Node} instance, depending on
 * the implementation. If the returned nodes are distinct, they are equal to
 * each other and have the same hash code.
 * 
 * <p>All mutator methods of {@code NodeList} and {@code Node} are optional.
 * 
 * @author Jared Levy
 */
interface NodeList<E> extends List<E> {
  
  /**
   * Returns the node at the specified position within the list.
   * 
   * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
   */
  Node<E> getNode(int index);
  
  /**
   * Returns the first node in the list whose element equals the provided
   * object, or {@code null} if the list does not contain the element.  
   */
  Node<E> nodeWith(Object o);
  
  /**
   * Returns the last node in the list whose element equals the provided
   * object, or {@code null} if the list does not contain the element.  
   */
  Node<E> lastNodeWith(Object o);
  
  /**
   * Inserts the provided object at the end of the list.
   * 
   * @return the node holding the newly added element
   */
  Node<E> addAndGetNode(E e);
  
  /**
   * Inserts the provided object at the specified position of the list. Any
   * existing elements at position {@code index} or higher are shifted to the
   * position that's one greater. 
   * 
   * @return the node holding the newly added element
   * @throws IndexOutOfBoundsException if {@code index < 0 || index > size()}
   */
  Node<E> addAndGetNode(int index, E e);  
  
  /**
   * Concatenates the supplied list to the end of this list. After calling this
   * method, this list consists of the elements that it previously contained,
   * followed by the elements in {@code nodeList}, while {@code nodeList} is
   * empty. Any {@link Node} instances that previously referred to nodes in
   * either list remain valid and refer to nodes in this list.
   * 
   * <p>To copy another list's elements onto the end of this list without
   * changing the other list, call {@link List#addAll(java.util.Collection)}
   * instead.  
   * 
   * @param nodeList the list containing the elements to transfer
   * @return {@code true} if the lists changed, or {@code false} if
   *     {@code nodeList} was already empty
   * @throws IllegalArgumentException if {@code this == nodeList}
   * @throws ClassCastException if {@code nodeList} and this list have different
   *     implementation classes
   */
  boolean concat(NodeList<E> nodeList);
  
  /**
   * A node that corresponds to an element of a {@code NodeList}. The node
   * continues to hold the same element after any list updates besides
   * {@code set} methods. {@code Node} methods let you traverse or update the
   * underlying list.
   * 
   * <p>The {@link #equals} and {@link #hashCode} methods are defined in a way
   * that makes a {@code Node} an acceptable hash key.
   * 
   * <p>After a node is removed from the list, some {@code Node} methods have
   * the same behavior as before, some have different behavior, and some are no
   * longer supported. See the method descriptions for details. 
   */
  public interface Node<E> {

    /**
     * Retrieves the element that the node contains. This method's behavior
     * doesn't change after the node is removed from the list.
     */
    E get();

    /**
     * Returns the position of the node within the list.
     * 
     * @throws IllegalStateException if the node is no longer in the list
     */
    int index();
    
    /**
     * Replaces the node's current element. If the node is currently part of a
     * list, this change will be visible in the list. Calling this method is
     * still valid if the node has been removed from the list, but in that case
     * the list will not be affected in any way.
     */    
    E set(E e);

    /**
     * Removes the node from the underlying list.
     * 
     * @throws IllegalStateException if the node has already been removed from
     *    the list
     */
    void remove();

    /**
     * Returns the node that comes after this node in the list, or {@code null}
     * if this is the last node in the list.
     * 
     * <p>If this node has been removed from the list, the method returns the
     * node that followed this node at the time it was removed from the list, or
     * {@code null} if this was the last element of the list.
     */
    Node<E> next();

    /**
     * Returns the node that comes before this node in the list, or {@code null}
     * if this is the first node in the list.
     * 
     * <p>If this node has been removed from the list, the method returns the
     * node that preceded this node at the time it was removed from the list, or
     * {@code null} if this was the first element of the list.
     */
    Node<E> previous();

    /**
     * Adds the provided element at the list position before this node.
     * 
     * @return the node containing the newly added element
     * @throws IllegalStateException if the node is no longer in the list
     */
    Node<E> addBefore(E e);

    /**
     * Adds the provided element at the list position after this node.
     * 
     * @return the node containing the newly added element
     * @throws IllegalStateException if the node is no longer in the list
     */
    Node<E> addAfter(E e);

    /**
     * Returns the list containing this node, or {@code null} if this node is no
     * longer in a list.
     */
    NodeList<E> list();

    /**
     * Returns a {@code ListIterator} whose cursor position is just before this
     * node. Calling {@link ListIterator#next} on the iterator will return this
     * node's element, while calling {@link ListIterator#previous} will return
     * the previous element or throw a {@link NoSuchElementException}.
     * 
     * @throws IllegalStateException if the node is no longer in the list
     */
    ListIterator<E> listIterator();
    
    /**
     * Moves this node to the start of the list. Each node that previously
     * preceded this node is shifted forward one position.
     * 
     * @return {@code} true if the list changed, or {@code false} if this node
     *     was already at the start of the list.
     * @throws IllegalStateException if the node is no longer in the list
     */
    boolean moveToStart();
    
    /**
     * Moves this node to the end of the list. Each node that previously came
     * after this node is shifted back one position.
     * 
     * @return {@code} true if the list changed, or {@code false} if this node
     *     was already at the end of the list.
     * @throws IllegalStateException if the node is no longer in the list
     */
    boolean moveToEnd();
    
    /**
     * Switches the list position of this node and the provided node. The
     * other nodes remain at the same positions.
     * 
     * <p>This method may also swap two nodes in different lists, with each
     * node being transferred to the other list. Or, it may swap a previously
     * removed node with a node that's still in a list.
     * 
     * @return {@code} true if the list changed, or {@code false} if the
     *     supplied node equals this node or if both nodes have been removed
     *     from the list
     * @throws ClassCastException if the two nodes are from different
     *     {@code NodeList} implementations, preventing them from being swapped  
     */
    boolean swap(Node<E> node);
    
    /**
     * Splits the list containing this node into two lists, separating the lists
     * just before this node. Afterwards, the current list is truncated to
     * contain the nodes before this node. The newly created list includes this
     * node and all subsequent nodes. Any existing {@code Node} references
     * remain valid, regardless of which list the nodes end up belonging to.
     * 
     * @return the newly created list, consisting of this node and later nodes
     * @throws IllegalStateException if this node is no longer in a list
     */
    NodeList<E> splitBefore();
    
    /**
     * Splits the list containing this node into two lists, separating the lists
     * just after this node. Afterwards, the current list is truncated to
     * contain this node and all previous nodes. The newly created list includes
     * the subsequent nodes. Any existing {@code Node} references remain valid,
     * regardless of which list the nodes end up belonging to.
     * 
     * @return the newly created list, consisting of the nodes that follow this
     *     node
     * @throws IllegalStateException if this node is no longer in a list
     */
    NodeList<E> splitAfter();
    
    /**
     * Determines whether the specified object equals this node. Assuming this
     * node is still in the list, this method returns true if {@code obj} is a
     * {@code Node} that's in the same list at the same position. (Two nodes are
     * in the same list if they were produced by starting with the same
     * {@code NodeList} instance and calling successive {@code NodeList} or
     * {@code Node} methods). 
     * 
     * <p>The value of {@code node.equals(obj)} doesn't change over time; two
     * equal nodes remain equal and two non-equal nodes remain non-equal. Two
     * removed nodes are equal if, before being removed, they occupied the same
     * position in the same list. 
     */
    boolean equals(Object obj);
    
    /**
     * Returns the hash code of this node. A node's hash code never changes, 
     * even if its element changes, its position within the list changes, or the
     * node is removed from the list.
     */
    int hashCode();
    
    /**
     * Returns the string representation of the node's element, or {@code null}
     * if the element is null.
     * 
     * @return the string value {@code String.valueOf(get())}
     */
    String toString();
  }
}
