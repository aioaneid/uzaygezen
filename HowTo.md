## Multi-Dimensional Space Specification ##

Before doing anything useful the library needs to know the specification of the multi-dimensional space, that is, how many dimensions there are, together with the precision (number of bits) of each dimension. All this information is captured in a [MultiDimensionalSpec](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/MultiDimensionalSpec.java) object. There are no practical limitations on the number of bits of each dimension or on the number of dimensions.

## Fixed Size Bit Vector ##

The coordinates of a point in a multi-dimensional space are represented as an array of type [BitVector](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/BitVector.java)`[]`. If the coordinates are originally of type `long`, [BitSet](http://docs.oracle.com/javase/7/docs/api/java/util/BitSet.html), [BigInteger](http://docs.oracle.com/javase/7/docs/api/java/math/BigInteger.html), big-endian `byte[]` or little-endian `long[]`, the corresponding bit vectors can be created empty by invoking [BitVectorFactories](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/BitVectorFactories.java)`.OPTIMAL.apply(precision)` and then initialised by calling the `copyFrom(long/`[BitSet](http://docs.oracle.com/javase/7/docs/api/java/util/BitSet.html)`/`[BigInteger](http://docs.oracle.com/javase/7/docs/api/java/math/BigInteger.html)`/long[])` overloads or `copyFromBigEndian(byte[])`. Methods for the reverse transformation are also provided: `toExactLong()`, `toBitSet()`, `toBigInteger()`, `toBigEndianByteArray()` and `toLongArray()`, respectively.

## Compact Hilbert Index Mappings ##

For performance reasons some methods need a pre-allocated output parameter to be passed in. To compute an index, call [SpaceFillingCurve](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/SpaceFillingCurve.java)`.index(point, 0, index)` with a pre-allocated index bit vector of size [MultiDimensionalSpec](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/MultiDimensionalSpec.java)`.sumBitsPerDimension()`.

To extract the coordinates back from an index, use [SpaceFillingCurve](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/SpaceFillingCurve.java)`.index(index, point)` with a pre-allocated coordinate array.

Here's an example showing how to compute the Hilbert index for two dimensions, each with just 2 bits, together with the inverse Hilbert index, recovering therefore the original point:

```
CompactHilbertCurve chc = new CompactHilbertCurve(new int[] {2, 2});
List<Integer> bitsPerDimension = chc.getSpec().getBitsPerDimension();
BitVector[] p = new BitVector[bitsPerDimension.size()];
for (int i = p.length; --i >= 0;) {
	p[i] = BitVectorFactories.OPTIMAL.apply(bitsPerDimension.get(i));
}
p[0].copyFrom(0b10);
p[1].copyFrom(0b11);
BitVector chi = BitVectorFactories.OPTIMAL.apply(chc.getSpec().sumBitsPerDimension());
chc.index(p, 0, chi);
System.out.println(String.format(Locale.ROOT, "index([0b%s, 0b%s])=0b%s", p[0], p[1], chi));
// Next line overwrites whatever is already written in p.
chc.indexInverse(chi, p);
System.out.println(String.format(
	Locale.ROOT, "indexInverse(0b%s)=[0b%s, 0b%s]", chi, p[0], p[1]));
```

The example above prints:

```
index([0b10, 0b11])=0b1001
indexInverse(0b1001)=[0b10, 0b11]
```

## Queries in Read-Write Tables ##

Space filling curves can be used to index multidimensional data so that regions described mainly as range constraints on some or all of the dimensions can be queried more efficiently than if one indexed only one dimension. The challenge is to encode the data in such a way that small regions in the multi-dimensional space result in a small number of ranges for a plain-vanilla uni-dimensional relational database index. One way of achieving this is by storing the (big-endian) multi-dimensional index either as the primary key (recommended), or as an indexed column in the table. Then there are a few options to consider:
  * The original coordinates do not necessarily need to be stored in the table. However if the number of ranges and optionally sub-ranges (more generally, filters combined through a [FilterCombiner](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/FilterCombiner.java)) generated for queries tends to be too high, it might be useful to either limit, or not use the sub-ranges feature at all, besides limiting the number of high level ranges, and append to the where clause of the SQL query further filtering of the points to be selected based on their coordinates.
  * There are a few ways to construct the main part of the SQL queries, and which one to use can probably be decided only through experimentation:
    * Some databases have special support for array-type prepared statement parameters, in which case redundantly storing the coordinates as separate columns for extra filtering probably works best (see H2's [TestMultiDimension](http://code.google.com/p/h2database/source/browse/trunk/h2/src/test/org/h2/test/db/TestMultiDimension.java) and [MultiDimension](http://code.google.com/p/h2database/source/browse/trunk/h2/src/main/org/h2/tools/MultiDimension.java) classes).
    * Some databases might combine multiple UNION ALL statements into one during query optimisation, and if the number of ranges is high then do a full-table scan, even though only a small subset of the table is selected. In such a database the coordinates should be stored redundantly and the sub-ranges feature should not be used at all, i.e. use [PlainFilterCombiner](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/PlainFilterCombiner.java) as the filter combiner and [PlainFilterCombiner](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/PlainFilterCombiner.java)`.FILTER` as the unique filter object). A typical query would look like this:
```
select columns from table where (hilbertIndex between lo_0 and hi_0 or ... hilbertIndex between lo_n and hi_n)
    and x between xlo and xhi and y between ylo and yhi and z between zlo and zhi and t between tlo and thi ...
```
    * For the other databases it might make sense to create a UNION ALL of the main ranges, and express each range as a where clause consisting in a set of sub-ranges, i.e. use [ListConcatCombiner](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/ListConcatCombiner.java) as the filter combiner. A typical query would be
```
select columns from (select hilbertIndex, columns from table where hilbertIndex between lo_0 and hi_0)
    where hilbertIndex between sublo_00 and subhi_00 or hilbertIndex between sublo_01 and subhi_01 ...
UNION ALL
... (the same but replace lo_0 with lo_n, hi_0 with hi_n, sublo_0i with sublo_ni and subhi_0i with subhi_ii)
```
Note that all sub-ranges of a range are fully covered by that range.

In either case the query region is represented by the [RegionInspector](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/RegionInspector.java) abstraction, with the plain-vanilla [SimpleRegionInspector](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/SimpleRegionInspector.java) implementation provided out of the box for queries consisting of one or more hyper-rectangles. Note that the query doesn't have to look like a (small) set of hyper-rectangles, and can in general have any shape, with the inherent performance price and the need to implement a custom region inspector.

If the total precision of the multidimensional space is less than 63 then the query logic may be executed using the (possibly faster) [Long](http://docs.oracle.com/javase/7/docs/api/java/lang/Long.html) class and its related classes [LongContent](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/LongContent.java), [LongRange](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/ranges/LongRange.java) and [LongRangeHome](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/ranges/LongRangeHome.java), while [BigInteger](http://docs.oracle.com/javase/7/docs/api/java/math/BigInteger.html) and its related classes [BigIntegerContent](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/BigIntegerContent.java), [BigIntegerRange](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/ranges/BigIntegerRange.java) and [BigIntegerRangeHome](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/ranges/BigIntegerRangeHome.java) serve the creation of queries in arbitrary-precision multidimensional spaces. In general it's a good idea to keep the total precision of the data space as small as possible.

Here's a snippet of what the code may look like for a total precision of less than 63 bits:
```
// int[] min, int[] max represent the query hyper-rectangle, both ends inclusive
List<LongRange> criterion = new ArrayList<>(min.length);
for (int i = 0; i < min.length; ++i) {
    criterion.add(LongRange.of(min[i], max[i] + 1));
}
LongContent zero = new LongContent(0L);
RegionInspector<Object, LongContent> simpleRegionInspector = SimpleRegionInspector.create(
      x, one, Functions.constant(filter), LongRangeHome.INSTANCE, zero);
final int maxFilteredRanges = 20;
// PlainFilterCombiner since we're not using sub-ranges here
PlainFilterCombiner<Object, Long, LongContent, LongRange> combiner = new PlainFilterCombiner<>(filter);
QueryBuilder<Object, LongRange> queryBuilder = BacktrackingQueryBuilder.create(
      regionInspector, combiner, maxRanges, true, LongRangeHome.INSTANCE, zero);
// sfc is of type SpaceFillingCurve, such as an instance of CompactHilbertIndex
sfc.accept(new ZoomingSpaceVisitorAdapter(sfc, queryBuilder));
Query<Object, LongRange> query = queryBuilder.get();
List<FilteredIndexRange<Object, LongRange>> ranges = query.getFilteredIndexRanges();
```
To remove the limitation on the total precision, just replace `Long` with `BigInteger` everywhere in the code above.

Then construct an SQL query that selects between `ranges.get(i).getIndexRange().getStart()` and `ranges.get(i).getIndexRange().getEnd() - 1` inclusive at both ends, and for those ranges returning `true` from `ranges.get(i).isPotentialOverSelectivity()` add a where clause based on the coordinate values if stored redundantly in the database. Note that those ranges that return `false` from `ranges.get(i).isPotentialOverSelectivity()` are guaranteed to not select any extraneous points. Also, if maxFilteredRanges is made large enough then it is guaranteed that no ranges will have potential over-selectivity, but the number of ranges can easily grow too large.

## Queries in Mostly Read-Only Tables ##

Optionally for a mostly read-only table, after each modification the table can be scanned to build a limited rolled-up version of the full table in memory and used to significantly speed up both query building and query execution, the latter as a result of better queries being produced. The idea is that the table is analysed to see which regions have points and which regions have more points than others and the information is aggregated into a tree-shaped cache of specified size. To ensure correctness, the cache needs to be updated whenever there are new data points in the underlying data store in regions that the cache regards as empty. The API for caching is centered around [StreamingRollup](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/StreamingRollup.java), [BoundedRollup](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/BoundedRollup.java) and [Pow2LengthBitSetRangeFactory](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/main/java/com/google/uzaygezen/core/Pow2LengthBitSetRangeFactory.java).

## HBase example ##

The test case [HBaseQueryTest](https://code.google.com/p/uzaygezen/source/browse/trunk/uzaygezen-core/src/test/java/com/google/uzaygezen/core/hbase/HBaseQueryTest.java) includes a complete example based on [MockHTable](http://blog.erdemagaoglu.com/post/1254694314/unit-testing-hbase-applications) which is an in-memory implementation of HBase's [HTableInterface](http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/client/HTableInterface.html) API. The mentioned test case populates a table with gaussian data from a three-dimensional space with a total precision that is larger than 62 bits and it queries it either using the non-caching API or the caching wrapper.

## Other Considerations ##

At this point very little performance testing has been done, and although the index and inverse computation routines are pretty stable, significant changes may be made in the query building mechanism in the future. Also, note that many relational databases provide built-in multi-indexing support based on R-trees and not only, which may be more efficient where available than the database independent approach employed here.

From a stability point of view, although this version is marked as 0.2, there are extensive unit tests for most of the functionality with very good coverage. The unit tests should also serve as further usage examples.

Dependencies: The library has only 3 direct dependencies: `com.google.guava/guava`, `log4j/log4j` and `org.apache.commons/commons-lang3`. The test code also depends on `junit/junit`, `org.easymock/easymock`, `org.apache.hadoop/hadoop-core` and `org.apache.hbase/hbase`.

The library is available in [Maven central](http://search.maven.org/#search%7Cga%7C1%7Cuzaygezen-core).

## Useful Links ##

  * [Space-Filling Curves in Geospatial Applications](http://www.ddj.com/184410998)
  * [Compact Hilbert Indices](http://www.cs.dal.ca/research/techreports/cs-2006-07)