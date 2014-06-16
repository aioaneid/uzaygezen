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

/**
 * Compact Hilbert index mappings and backtracking query building support for
 * persistence systems.
 * <p>
 * Throughout the comments we use the following terminology:
 * <ul>
 *  <li><a href="http://mathworld.wolfram.com/Orthotope.html">Orthotope</a></li>
 *  <li>a href="http://mathworld.wolfram.com/Content.html">Content</a></li>
 * </ul>
 * </p>
 * <p>
 * Bibliography:
 * <ul>
 *  <li>
 *   <a href="http://ieeexplore.ieee.org/xpl/freeabs_all.jsp?arnumber=4159726">
 *    Compact Hilbert Indices for Multi-Dimensional Data</a>
 *    Hamilton, Chris H.; Rau-Chaplin, Andrew
 *    Complex, Intelligent and Software Intensive Systems, 2007. CISIS 2007.
 *    First International Conference on
 *    Volume , Issue , 10-12 April 2007 Page(s):139 - 146
 *    Digital Object Identifier   10.1109/CISIS.2007.16
 *  </li>
 *  <li>
 *   <a href="http://www.cs.dal.ca/research/techreports/2006/CS-2006-07.pdf">
 *   Compact Hilbert indices</a>
 *   C. Hamilton, Dalhousie University, Faculty of Computer Science,
 *   Technical Report CS-2006-07, July 2006
 *  </li>
 *  <li>
 *   <a href="http://citeseer.ist.psu.edu/lawder01querying.html"
 *   Querying Multi-dimensional Data Indexed Using the Hilbert Space-Filling
 *   Curve</a>
 *   J.K. Lawder, P.J.H. King
 *   SIGMOD Record, Volume 30, Issue 1 (March 2001), Pages: 19-24,
 *   ISSN:0163-5808
 *  </li>
 * </ul>
 * </p>
 */
package com.google.uzaygezen.core;
