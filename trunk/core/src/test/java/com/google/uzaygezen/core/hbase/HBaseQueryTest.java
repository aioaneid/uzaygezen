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

package com.google.uzaygezen.core.hbase;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.primitives.Ints;
import com.google.uzaygezen.core.BacktrackingQueryBuilder;
import com.google.uzaygezen.core.BitVector;
import com.google.uzaygezen.core.BitVectorFactories;
import com.google.uzaygezen.core.BitVectorMath;
import com.google.uzaygezen.core.BoundedRollup;
import com.google.uzaygezen.core.CompactHilbertCurve;
import com.google.uzaygezen.core.FilteredIndexRange;
import com.google.uzaygezen.core.HilbertIndexMasks;
import com.google.uzaygezen.core.MapNode;
import com.google.uzaygezen.core.MapRegionInspector;
import com.google.uzaygezen.core.MultiDimensionalSpec;
import com.google.uzaygezen.core.NodeValue;
import com.google.uzaygezen.core.PlainFilterCombiner;
import com.google.uzaygezen.core.Pow2LengthBitSetRange;
import com.google.uzaygezen.core.Pow2LengthBitSetRangeFactory;
import com.google.uzaygezen.core.Query;
import com.google.uzaygezen.core.RegionInspector;
import com.google.uzaygezen.core.SimpleRegionInspector;
import com.google.uzaygezen.core.SpaceFillingCurve;
import com.google.uzaygezen.core.StreamingRollup;
import com.google.uzaygezen.core.TestUtils;
import com.google.uzaygezen.core.ZoomingSpaceVisitorAdapter;
import com.google.uzaygezen.core.ranges.BigIntegerContent;
import com.google.uzaygezen.core.ranges.BigIntegerRange;
import com.google.uzaygezen.core.ranges.BigIntegerRangeHome;
import com.google.uzaygezen.core.ranges.RangeUtil;

/**
 * Test case that also serves as an example of how to use the query
 * functionality. While this class relies on BigInteger, BigIntegerContent,
 * BigIntegerRange and BigIntegerRangeHome, it is recommended to use the
 * parallel Long, LongContent, LongRange and LogRangeHome classes when the total
 * precision of the Hilbert space is less than 63 bits.
 * 
 * @author Daniel Aioanei
 */
public class HBaseQueryTest {

  private static final Logger logger = Logger.getLogger(HBaseQueryTest.class.getSimpleName());

  /**
   * With more than 62 bits (using {@link BigInteger} rather than plain
   * {@link Long}) and without any caching rollup version of the data
   * {@link BoundedRollup}, this way of building the queries is likely to be
   * quite slow, but it shows off the capability of perform queries of
   * non-cached arbitrary-precision data.
   */
  @Test
  public void queryHBase() throws IOException, InterruptedException {
    MockHTable table = MockHTable.create();
    final byte[] family = "FAMILY".getBytes(Charsets.ISO_8859_1);
    /*
     * We choose not to store the coordinates themselves, since storing the
     * Hilbert index is sufficient to recover the coordinate values. So let's
     * use a dummy column.
     */
    final byte[][] qualifiers = {"NICE".getBytes(Charsets.ISO_8859_1),};
    MultiDimensionalSpec spec = new MultiDimensionalSpec(Ints.asList(30, 10, 25));
    // Add some data.
    Random rnd = new Random(TestUtils.SEED);
    int[][] data = generateData(spec, 1 << 16, rnd);
    SpaceFillingCurve sfc = new CompactHilbertCurve(spec);
    logger.log(Level.INFO, "Populating table with up to {0} rows.", data.length);
    populateTable(family, qualifiers, spec, data, sfc, table);
    int cacheSize = 1 << 8;
    logger.log(Level.INFO, "Building cache of size {0}.", cacheSize);
    // The cache is optional.
    Map<Pow2LengthBitSetRange, NodeValue<BigIntegerContent>> rolledupMap = createRolledupCache(
      table, spec, sfc, cacheSize);
    logger.log(Level.INFO, "Constructed cache of actual size {0}.", rolledupMap.size());
    for (int trial = 0; trial < 1; ++trial) {
      logger.log(Level.INFO, "trial={0}", trial);
      int[] maxLengthPerDimension = new int[spec.getBitsPerDimension().size()];
      for (boolean useCache : new boolean[] {false, true}) {
        int m = useCache ? 256 : 32;
        /*
         * For testing purposes limit the range size to m values for each
         * dimension to speed up query computation. In practice, query volume
         * should be enforced to be small, and when a certain query volume is
         * exceeded, a full table scan will probably be faster anyway.
         */
        Arrays.fill(maxLengthPerDimension, m);
        int[][] ranges = generateRanges(spec, maxLengthPerDimension, rnd);
        logger.log(Level.INFO, "ranges={0}", Arrays.deepToString(ranges));
        // Limit the maximum number of ranges.
        int maxRanges = 1 + rnd.nextInt(32);
        List<int[]> actual = queryAndFilter(
          table, spec, sfc, ranges, maxRanges, useCache ? rolledupMap : null);
        List<int[]> expected = uniq(fullScanQuery(data, sfc, ranges));
        logger.log(Level.INFO, "expected.size()={0}", expected.size());
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); ++i) {
          Assert.assertArrayEquals(expected.get(i), actual.get(i));
        }
      }
    }
  }

  public Map<Pow2LengthBitSetRange, NodeValue<BigIntegerContent>> createRolledupCache(
    MockHTable table, MultiDimensionalSpec spec, SpaceFillingCurve sfc, int cacheSize)
    throws IOException {
    int[] elementLengths = Ints.toArray(new HilbertIndexMasks(sfc.getSpec()).cardinalities());
    BitVector[] path = new BitVector[elementLengths.length];
    for (int i = 0; i < path.length; ++i) {
      path[i] = BitVectorFactories.OPTIMAL.apply(elementLengths[path.length - i - 1]);
    }
    StreamingRollup<BitVector, BigIntegerContent> rollup = BoundedRollup.create(
      new BigIntegerContent(BigInteger.ZERO), cacheSize);
    Scan fullScan = new Scan();
    ResultScanner scanner = table.getScanner(fullScan);
    BitVector hilbertIndex = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
    for (Result row : scanner) {
      hilbertIndex.copyFromBigEndian(row.getRow());
      for (int i = 0; i < path.length; ++i) {
        path[i] = path[i].clone();
      }
      BitVectorMath.split(hilbertIndex, path);
      // We should say the exact number of times. Saying one is correct, but
      // suboptimal.
      BigIntegerContent v = new BigIntegerContent(BigInteger.ONE);
      rollup.feedRow(Iterators.<BitVector>forArray(path), v);
    }
    MapNode<BitVector, BigIntegerContent> rolledupTree = rollup.finish();
    Pow2LengthBitSetRangeFactory<BigIntegerContent> factory = Pow2LengthBitSetRangeFactory.create(Ints.asList(elementLengths));
    Map<Pow2LengthBitSetRange, NodeValue<BigIntegerContent>> rolledupMap = factory.apply(rolledupTree);
    return rolledupMap;
  }

  public List<int[]> fullScanQuery(int[][] data, SpaceFillingCurve sfc, int[][] ranges) {
    MultiDimensionalSpec spec = sfc.getSpec();
    List<Integer> filtered = filter(data, ranges);
    List<Pair<BitVector, Integer>> pairs = new ArrayList<>(filtered.size());
    BitVector[] point = new BitVector[spec.getBitsPerDimension().size()];
    for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
      point[j] = BitVectorFactories.OPTIMAL.apply(spec.getBitsPerDimension().get(j));
    }
    for (int i : filtered) {
      BitVector index = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
      // int has 32 bits, which fits in each dimensions.
      for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
        point[j].copyFrom(data[i][j]);
      }
      sfc.index(point, 0, index);
      pairs.add(Pair.of(index.clone(), i));
    }
    // Sort by Hilbert index.
    Collections.sort(pairs);
    List<int[]> expected = new ArrayList<>(pairs.size());
    for (Pair<BitVector, Integer> pair : pairs) {
      expected.add(data[pair.getRight()]);
    }
    return expected;
  }

  private static List<Integer> filter(int[][] data, int[][] ranges) {
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < data.length; ++i) {
      if (RangeUtil.contains(ranges, data[i])) {
        result.add(i);
      }
    }
    return result;
  }

  public List<int[]> queryAndFilter(
    MockHTable table, MultiDimensionalSpec spec, SpaceFillingCurve sfc, int[][] ranges,
    int maxRanges, Map<Pow2LengthBitSetRange, NodeValue<BigIntegerContent>> rolledupMap)
    throws IOException {
    List<BigIntegerRange> region = rangesToQueryRegion(ranges);
    List<FilteredIndexRange<Object, BigIntegerRange>> indexRanges = query(
      table, region, sfc, maxRanges, rolledupMap);
    Assert.assertTrue(indexRanges.size() <= maxRanges);
    logger.log(Level.INFO, "indexRanges={0}", indexRanges);
    // The ranges are in strictly increasing hilbert index order.
    for (int i = 0; i < indexRanges.size() - 1; ++i) {
      FilteredIndexRange<Object, BigIntegerRange> a = indexRanges.get(i);
      FilteredIndexRange<Object, BigIntegerRange> b = indexRanges.get(i + 1);
      Assert.assertTrue(a.getIndexRange().getEnd().compareTo(b.getIndexRange().getStart()) < 0);
    }
    BitVector start = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
    BitVector end = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
    Scan[] scans = new Scan[indexRanges.size()];
    for (int i = 0; i < indexRanges.size(); ++i) {
      FilteredIndexRange<Object, BigIntegerRange> indexRange = indexRanges.get(i);
      BigInteger startBigInteger = indexRange.getIndexRange().getStart();
      start.copyFrom(startBigInteger);
      BigInteger endBigInteger = indexRange.getIndexRange().getEnd();
      final Scan scan;
      if (endBigInteger.testBit(spec.sumBitsPerDimension())) {
        scan = new Scan(start.toBigEndianByteArray());
      } else {
        end.copyFrom(endBigInteger);
        scan = new Scan(start.toBigEndianByteArray(), end.toBigEndianByteArray());
      }
      scans[i] = scan;
    }
    BitVector[] point = new BitVector[spec.getBitsPerDimension().size()];
    BitVector index = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
    for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
      point[j] = BitVectorFactories.OPTIMAL.apply(spec.getBitsPerDimension().get(j));
    }
    List<int[]> actual = new ArrayList<>();
    for (int i = 0; i < indexRanges.size(); ++i) {
      ResultScanner scanner = table.getScanner(scans[i]);
      FilteredIndexRange<Object, BigIntegerRange> indexRange = indexRanges.get(i);
      logger.log(Level.FINE, "indexRange={0}", indexRange);
      for (Result result : scanner) {
        byte[] row = result.getRow();
        index.copyFromBigEndian(row);
        sfc.indexInverse(index, point);
        boolean isContained = RangeUtil.containsBigInteger(
          region, Arrays.asList(bitVectorPointToBigIntegerPoint(point)));
        if (!indexRange.isPotentialOverSelectivity()) {
          Assert.assertTrue(isContained);
        }
        if (isContained) {
          int[] e = new int[point.length];
          for (int j = 0; j < e.length; ++j) {
            e[j] = (int) point[j].toExactLong();
          }
          actual.add(e);
        }
      }
    }
    return actual;
  }

  private BigInteger[] bitVectorPointToBigIntegerPoint(BitVector[] point) {
    BigInteger[] a = new BigInteger[point.length];
    for (int i = 0; i < a.length; ++i) {
      a[i] = point[i].toBigInteger();
    }
    return a;
  }

  private List<FilteredIndexRange<Object, BigIntegerRange>> query(
    MockHTable table, List<BigIntegerRange> region, SpaceFillingCurve sfc, int maxRanges,
    Map<Pow2LengthBitSetRange, NodeValue<BigIntegerContent>> rolledupMap) {
    List<? extends List<BigIntegerRange>> x = ImmutableList.of(region);
    BigIntegerContent zero = new BigIntegerContent(BigInteger.ZERO);
    Object filter = "";
    BigIntegerContent one = new BigIntegerContent(BigInteger.ONE);
    RegionInspector<Object, BigIntegerContent> simpleRegionInspector = SimpleRegionInspector.create(
      x, one, Functions.constant(filter), BigIntegerRangeHome.INSTANCE, zero);
    final RegionInspector<Object, BigIntegerContent> regionInspector;
    if (rolledupMap == null) {
      regionInspector = simpleRegionInspector;
    } else {
      regionInspector = MapRegionInspector.create(
        rolledupMap, simpleRegionInspector, false, zero, one);
    }
    // Not using using sub-ranges here.
    PlainFilterCombiner<Object, BigInteger, BigIntegerContent, BigIntegerRange> combiner = new PlainFilterCombiner<>(
      filter);
    BacktrackingQueryBuilder<Object, BigInteger, BigIntegerContent, BigIntegerRange> queryBuilder = BacktrackingQueryBuilder.create(
      regionInspector, combiner, maxRanges, true, BigIntegerRangeHome.INSTANCE, zero);
    sfc.accept(new ZoomingSpaceVisitorAdapter(sfc, queryBuilder));
    Query<Object, BigIntegerRange> query = queryBuilder.get();
    return query.getFilteredIndexRanges();
  }

  private static List<BigIntegerRange> rangesToQueryRegion(int[][] ranges) {
    List<BigIntegerRange> region = new ArrayList<>();
    for (int j = 0; j < ranges.length; ++j) {
      region.add(BigIntegerRange.of(ranges[j][0], ranges[j][1]));
    }
    return region;
  }

  private static int[][] generateRanges(
    MultiDimensionalSpec spec, int[] maxLengthPerDimension, Random rnd) {
    int[][] ranges = new int[spec.getBitsPerDimension().size()][2];
    for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
      int bound = 1 << spec.getBitsPerDimension().get(j);
      int start = bound / 2 - rnd.nextInt(Math.min(bound, maxLengthPerDimension[j])) / 2;
      assert start >= 0;
      int end = (bound + 1) / 2 + rnd.nextInt(Math.min(bound, maxLengthPerDimension[j])) / 2;
      assert end <= bound;
      ranges[j][0] = start;
      ranges[j][1] = end;
    }
    return ranges;
  }

  private static void populateTable(
    final byte[] family, final byte[][] qualifiers, MultiDimensionalSpec spec, int[][] data,
    SpaceFillingCurve sfc, MockHTable table) throws IOException, InterruptedException {
    BitVector[] point = new BitVector[spec.getBitsPerDimension().size()];
    BitVector index = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
    for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
      point[j] = BitVectorFactories.OPTIMAL.apply(spec.getBitsPerDimension().get(j));
    }
    Put[] puts = new Put[data.length];
    for (int i = 0; i < data.length; ++i) {
      // int has 32 bits, which fits in each dimensions.
      for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
        point[j].copyFrom(data[i][j]);
      }
      sfc.index(point, 0, index);
      byte[] row = index.toBigEndianByteArray();
      Put put = new Put(row);
      KeyValue[] keyValues = new KeyValue[qualifiers.length];
      for (int k = 0; k < qualifiers.length; ++k) {
        // Put a nice string representation of the data point in the dummy
        // column.
        keyValues[k] = new KeyValue(row, family, qualifiers[k], Arrays.toString(data[i]).getBytes(
          Charsets.ISO_8859_1));
      }
      put.setFamilyMap(ImmutableMap.of(family, Arrays.asList(keyValues)));
      puts[i] = put;
    }
    table.batch(Arrays.asList(puts));
  }

  /**
   * It may generate duplicates.
   */
  private static int[][] generateData(MultiDimensionalSpec spec, int n, Random rnd) {
    int[][] data = new int[n][spec.getBitsPerDimension().size()];
    for (int i = 0; i < n; ++i) {
      // int has 32 bits, which fits in each dimensions.
      for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
        int bound = 1 << spec.getBitsPerDimension().get(j);
        double gauss = rnd.nextGaussian();
        // Std of 1024.
        int d = bound / 2 + (int) (gauss * (1 << (spec.getBitsPerDimension().get(j) / 2)) / 1024);
        if (d < 0) {
          d = 0;
        }
        if (d >= bound) {
          d = bound - 1;
        }
        data[i][j] = d;
      }
    }
    return data;
  }

  public static List<int[]> uniq(List<int[]> data) {
    List<int[]> u = new ArrayList<>();
    for (int i = 0; i < data.size(); ++i) {
      if (i == data.size() - 1 || !Arrays.equals(data.get(i), data.get(i + 1))) {
        u.add(data.get(i));
      }
    }
    return u;
  }
}
