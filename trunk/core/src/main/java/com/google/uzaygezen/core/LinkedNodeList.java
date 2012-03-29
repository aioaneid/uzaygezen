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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Nullable;
import com.google.common.collect.ForwardingList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Linked list implementation of {@code NodeList} and {@code Queue}. All
 * optional {@link List}, {@link NodeList}, and {@link NodeList.Node} operations
 * are supported. However, this list does not support null elements.
 * 
 * <p>Its methods perform as expected for a doubly-link list. Methods taking
 * an index parameter traverse the list from the start or end of the list.
 * Adding or removing elements from the start or end of the list is fast.
 * 
 * <p>Two successive calls to {@code getNode(i)}, without any list changes
 * in between, return the same {@code Node} instance.
 * 
 * <p>The list is serializable, assuming its elements are, but the individual
 * {@code Node} instances are not.
 *
 * @author Jared Levy
 */
final class LinkedNodeList<E> extends AbstractSequentialList<E>
    implements NodeList<E>, Queue<E>, Serializable {
  transient private int size;
  /** First node in list, or null if the list is empty. */
  transient private NodeImpl<E> head;
  /** Last node in list, or null if the list is empty. */
  transient private NodeImpl<E> tail;

  private LinkedNodeList() {}
  
  private LinkedNodeList(Collection<? extends E> collection) {
    addAll(collection);
  }
  
  /** Constructs an empty list. */
  public static <E> LinkedNodeList<E> create() {
    return new LinkedNodeList<E>();
  }
  
  /** Constructs a list with the contents of the provided collection. */
  public static <E> LinkedNodeList<E> create(
      Collection<? extends E> collection) {
    return new LinkedNodeList<E>(collection);
  }
  
  @Override public void add(int index, E e) {
    addAndGetNode(index, e);
  }
  
  @Override public ListIterator<E> listIterator(int index) {
    return new IteratorImpl(index);
  }

  @Override public E remove(int index) {
    Node<E> node = getNode(index);
    node.remove();
    return node.get();
  }
  
  @Override public boolean removeAll(Collection<?> c) {
    return super.removeAll(checkNotNull(c));
  }
  
  @Override public boolean retainAll(Collection<?> c) {
    return super.retainAll(checkNotNull(c));
  }
  
  @Override public int size() {
    return size;
  }

  @Override public List<E> subList(int fromIndex, int toIndex) {
    // Using ForwardingList so removeAll(null) and retainAll(null) throw a NPE
    final List<E> delegate = super.subList(fromIndex, toIndex);
    return new ForwardingList<E>() {
      @Override protected List<E> delegate() {
        return delegate;
      }
    };
  }  
  
  public Node<E> addAndGetNode(E e) {    
    return addAndGetNode(size, e);
  }

  public Node<E> addAndGetNode(int index, E e) {
    if ((index < 0) || (index > size)) {
      throw new IndexOutOfBoundsException("Invalid index " + index);
    }
    if (head == null) {
      NodeImpl<E> node = new NodeImpl<E>(e, null, null, this);
      head = node;
      tail = node;
      return node;
    } else if (index < size) {
      return getNode(index).addBefore(e);
    } else {
      return tail.addAfter(e);
    }
  }

  public NodeImpl<E> getNode(int index) {
    if ((index < 0) || (index >= size)) {
      throw new IndexOutOfBoundsException("Invalid index " + index);
    }
    NodeImpl<E> node;
    
    if (index < size / 2) {
      node = head;
      for (int i = 0; i < index; i++) {
        node = node.next;
      }
    } else {
      node = tail;
      for (int i = size - 1; i > index; i--) {
        node = node.previous;
      }
    }
    
    return node;
  }

  public Node<E> nodeWith(@Nullable Object o) {
    for (Node<E> node = head; node != null; node = node.next()) {
      if (node.get().equals(o)) {
        return node;
      }
    }
    return null;
  }

  public Node<E> lastNodeWith(@Nullable Object o) {
    for (Node<E> node = tail; node != null; node = node.previous()) {
      if (node.get().equals(o)) {
        return node;
      }
    }
    return null;
  }


  public boolean concat(NodeList<E> nodeList) {
    checkArgument(nodeList != this);
    LinkedNodeList<E> other = (LinkedNodeList<E>) nodeList;
    if (other.isEmpty()) {
      return false;
    }
    
    if (isEmpty()) {
      head = other.head;
    } else {
      tail.next = other.head;
      other.head.previous = tail;
    }
    tail = other.tail;
    size += other.size;
    modCount++;
    
    for (NodeImpl<E> node = other.head; node != null; node = node.next) {
      node.list = this;
    }
    
    other.head = null;
    other.tail = null;
    other.size = 0;
    other.modCount++;
    return true;
  }

  // Queue methods
  
  public E element() {
    if (head == null) {
      throw new NoSuchElementException();
    }
    return head.get();
  }

  public boolean offer(E e) {
    return add(e);
  }

  public E peek() {
    return (head == null) ? null : head.get();
  }

  public E poll() {
    if (head == null) {
      return null;
    }
    E element = head.get();
    head.remove();
    return element;
  }

  public E remove() {
    if (head == null) {
      throw new NoSuchElementException();
    }
    E element = head.get();
    head.remove();
    return element;
  }
  
  /*
   * TODO: Possibly override removeRange so the next() and previous() values of
   * the removed nodes don't change. Currently, calling clear() on the list or
   * a sublist modifies the previous values of all removed nodes past the first
   * one.
   */
  
  /**
   * The node implementation. It's used for storing the list structure
   * internally, and it's returned by the methods with node output.
   */
  private static class NodeImpl<E> implements Node<E> {
    E element;
    NodeImpl<E> previous;
    NodeImpl<E> next;
    LinkedNodeList<E> list;
    
    NodeImpl(E element, NodeImpl<E> previous, NodeImpl<E> next,
        LinkedNodeList<E> list) {
      this.element = checkNotNull(element);
      this.previous = previous;
      this.next = next;
      this.list = list;
      // All new nodes will be added to the list.
      list.size++;
      list.modCount++;
    }

    public Node<E> addAfter(E e) {
      checkState(list != null);
      NodeImpl<E> node;
      if (this == list.tail) {
        node = new NodeImpl<E>(e, list.tail, null, list);
        list.tail.next = node;
        list.tail = node;
      } else {
        node = new NodeImpl<E>(e, this, next, list);
        next.previous = node;
        next = node;
      }
      return node;
    }

    public Node<E> addBefore(E e) {
      checkState(list != null);
      NodeImpl<E> node;
      if (this == list.head) {
        node = new NodeImpl<E>(e, null, list.head, list);
        list.head.previous = node;
        list.head = node;
      } else {
        node = new NodeImpl<E>(e, previous, this, list);
        previous.next = node;
        previous = node;
      }
      return node;
    }

    public E get() {
      return element;
    }

    public int index() {
      checkState(list != null);
      int index = 0;
      Node<E> node = list.head;
      while (node != this) {
        index++;
        node = node.next();
      }
      return index;
    }
    
    public NodeList<E> list() {
      return list;
    }

    public ListIterator<E> listIterator() {
      checkState(list != null);
      return list.new IteratorImpl(this);
    }

    public Node<E> next() {
      return next;
    }

    public Node<E> previous() {
      return previous;
    }

    public void remove() {
      checkState(list != null);
      removeNode();      
      list.size--;
      list.modCount++;
      list = null;
    }

    /** Remove the node from the list, but don't adjust size/inList/modCount. */
    private void removeNode() {
      if (this == list.head) {
        list.head = next;
      } else {
        previous.next = next;
      }
      
      if (this == list.tail) {
        list.tail = previous; 
      } else {
        next.previous = previous;
      }
    }

    public E set(E e) {
      E old = element;
      element = checkNotNull(e);
      return old;
    }

    public boolean moveToStart() {
      checkState(list != null);
      if (this == list.head) {
        return false;
      }      
      
      removeNode();
      next = list.head;
      previous = null;
      list.head.previous = this;
      list.head = this;
      
      list.modCount++;
      return true;
    }

    public boolean moveToEnd() {
      checkState(list != null);
      if (this == list.tail) {
        return false;
      } 
      
      removeNode();
      next = null;
      previous = list.tail;
      list.tail.next = this;
      list.tail = this;
      
      list.modCount++;
      return true;
    }

    public boolean swap(Node<E> node) {
      if (this == node) {
        return false;
      }
      NodeImpl<E> other = (NodeImpl<E>) node;
      if ((list == null) && (other.list == null)) {
        return false;        
      }
      
      if (list == null) {
        swapOneRemoved(other, this);
      } else if (other.list == null) {
        swapOneRemoved(this, other);
      } else if (other == next) {
        swapWithNext();
      } else if (other == previous) {
        other.swapWithNext();
      } else {
        swapNonAdjacent(this, other);        
      }

      updateAfterSwap();
      other.updateAfterSwap();
      return true;
    }

    /** Swap a present node with a removed node. */ 
    private static <E> void swapOneRemoved(
        NodeImpl<E> present, NodeImpl<E> removed) {
      removed.previous = present.previous;
      removed.next = present.next;
      removed.list = present.list;
      present.list = null;
    }

    /** Swap this node with the node after it. */
    private void swapWithNext() {
      NodeImpl<E> originalNext = next;      
      originalNext.previous = previous;
      next = originalNext.next;
      originalNext.next = this;
      previous = originalNext;
    }
    
    /** Swap this node with another, non-adjacent, node. */
    private static <E> void swapNonAdjacent(
        NodeImpl<E> first, NodeImpl<E> second) {
      NodeImpl<E> oldPrevious = first.previous;
      first.previous = second.previous;
      second.previous = oldPrevious;
      
      NodeImpl<E> oldNext = first.next;
      first.next = second.next;
      second.next = oldNext;
      
      LinkedNodeList<E> oldList = first.list;
      first.list = second.list;
      second.list = oldList;
    }    

    /** 
     * After a node has been swapped, updates the head, tail, previous node's
     * next, next node's previous, and modCount as appropriate.
     */
    private void updateAfterSwap() {
      if (list != null) {
        if (previous == null) {
          list.head = this;
        } else {
          previous.next = this;
        }
        
        if (next == null) {
          list.tail = this;
        } else {
          next.previous = this;
        }
        
        list.modCount++;
      }
    }

    public NodeList<E> splitBefore() {
      checkState(list != null);
      LinkedNodeList<E> first = list;
      LinkedNodeList<E> second = new LinkedNodeList<E>();
      
      for (NodeImpl<E> node = this; node != null; node = node.next) {
        node.list = second;
        second.size++;
      }
      first.size -= second.size;      
      first.modCount++;
      
      second.head = this;
      second.tail = first.tail;
      first.tail = previous;
      if (this == first.head) {
        first.head = null;
      } else {
        previous.next = null;
        previous = null;
      }      
      
      return second;
    }
    
    public NodeList<E> splitAfter() {
      checkState(list != null);
      LinkedNodeList<E> first = list;
      LinkedNodeList<E> second = new LinkedNodeList<E>();      
      if (this == first.tail) {
        return second;
      }
      
      for (NodeImpl<E> node = next; node != null; node = node.next) {
        node.list = second;
        second.size++;
      }
      first.size -= second.size;      
      first.modCount++;      
      
      second.head = next;
      second.tail = first.tail;
      first.tail = this;      
      next.previous = null;
      next = null;
      
      return second;
    }
    
    @Override public String toString() {
      return element.toString();  // this implementation doesn't support nulls
    }
  }
  
  /** {@link ListIterator} implementation. */
  private class IteratorImpl implements ListIterator<E> {
    /** Position of element returned by next(). */
    int nextIndex;
    /** Node returned by next(), if not null. */
    NodeImpl<E> nextNode;
    /** Node that set() and remove() act on. */
    NodeImpl<E> toUpdate;
    int iteratorModCount = modCount;
    
    IteratorImpl(int index) {
      if ((index < 0) || (index > size)) {
        throw new IndexOutOfBoundsException("Invalid index " + index);
      }
      this.nextIndex = index;
    }

    IteratorImpl(NodeImpl<E> node) {
      this.nextIndex = node.index();
      this.nextNode = node;
    }

    public void add(E e) {
      checkConcurrentModification();
      if (nextNode == null) {
        LinkedNodeList.this.add(nextIndex, e);
      } else {
        nextNode.addBefore(e);
      }
      toUpdate = null;
      nextIndex++;
      iteratorModCount = modCount;
    }

    public boolean hasNext() {
      checkConcurrentModification();
      return nextIndex < size;
    }

    public boolean hasPrevious() {
      checkConcurrentModification();
      return nextIndex > 0;
    }

    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      toUpdate = (nextNode == null) ? getNode(nextIndex) : nextNode;
      nextNode = toUpdate.next;
      nextIndex++;
      return toUpdate.get();      
    }

    public int nextIndex() {
      checkConcurrentModification();
      return nextIndex;
    }

    public E previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      toUpdate
          = (nextNode == null) ? getNode(nextIndex - 1) : nextNode.previous;
      nextNode = toUpdate;
      nextIndex--;
      return toUpdate.get();
    }

    public int previousIndex() {
      checkConcurrentModification();
      return nextIndex - 1;
    }

    public void remove() {
      checkConcurrentModification();
      checkState(toUpdate != null);
      checkState(toUpdate.list != null);
      toUpdate.remove();
      if (toUpdate == nextNode) {
        nextNode = nextNode.next;
      } else {
        nextIndex--;        
      }
      toUpdate = null;
      iteratorModCount = modCount;
    }

    public void set(E e) {
      checkConcurrentModification();
      checkState(toUpdate != null);
      toUpdate.set(checkNotNull(e));
    }
    
    void checkConcurrentModification() {
      if (iteratorModCount != modCount) {
        throw new ConcurrentModificationException();
      }
    }
  }
  
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(toArray());
  }

  private void readObject(ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    @SuppressWarnings("unchecked")
    E[] array = (E[]) stream.readObject();
    for (E element : array) {
      add(element);
    }
  }
  
  private static final long serialVersionUID = 0;
}
