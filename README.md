# scaladiff
Basic diff and patch implementation for Scala sequences.

## Getting Started
_At the moment there are no Maven artifacts available._ To give it a try you have to download the project and compile it yourself.

## Usage
### Creating and Using a Diff
#### The Default Diff and Patch Implementations
The default diff and patch implementations are imported via the following statement.
```scala
scala> import de.digitalistbesser.diff.default._
import de.digitalistbesser.diff.default._
```
Afterwards you can create the diff for a source and target sequence by invoking `diffTo` on the source (unfortunately Scala sequences already contain a `diff` method so we have to go with the somewhat ugly `diffTo`).
```scala
scala> val source = Vector("123", "abc")
source: scala.collection.immutable.Vector[String] = Vector(123, abc)

scala> val target = Vector("ABC", "123")
target: scala.collection.immutable.Vector[String] = Vector(ABC, 123)

scala> val hunks = source diffTo target
hunks: Seq[de.digitalistbesser.diff.Hunk[String]] = List(Hunk(0,0,List(Insert(ABC), Match(123), Delete(abc))))
```
`diffTo` returns a sequence of hunks necessary to transform the source into the target or an empty sequence if the sequences are considered equal. Each hunk contains information about its location in the source and target sequences and the edits necessary to transform one into the other.

Invoke `patchWith` to apply the hunks to the source (as with `diff` Scala sequences unfortunately already contain a `patch` method themselves).
```scala
scala> source patchWith hunks
res0: de.digitalistbesser.diff.PatchResult[scala.collection.immutable.Vector[String],String] = PatchResult(Vector(ABC, 123),...)
```
The method returns a `PatchResult` that contains the resulting sequence (which should equal the target) and some additional data (omitted in the example output) that is explained below.

The target sequence can also be unpatched by invoking the `unpatchWith` method on the target with the corresponding hunks (although there is no `unpatch` method in Scala sequences the method is named `unpatchWith` in conformance with `patchWith`).
```scala
scala> target unpatchWith hunks
res1: de.digitalistbesser.diff.PatchResult[scala.collection.immutable.Vector[String],String] = PatchResult(Vector(123, abc),...)
```
The returned `PatchResult` should contain the source sequence.

#### Using Custom Diff and Patch Implementations
The aforementioned `de.digitalistbesser.diff.default._` import provides a convenient way to create diffs of two sequences.

Scaladiff comes with a small set of diff algorithm implementations in the `de.digitalistbesser.diff.algorithms` package that can be instantiated and combined separately.

| Name | Description |
| --- | --- |
| `MillerMyersDiffAlgorithm` | Diff algorithm implementation after _A File Comparison Program_ by Webb Miller and Eugene W. Myers. |
| `MyersGreedyDiffAlgorithm` | Default diff algorithm implementation after _An O(ND) Difference Algorithm and Its Variations_ by Eugene W. Myers |
| `MyersSpaceOptimizedDiffAlgorithm` | A variation of the greedy implementation. The name is misleading since this implementation differs from its ideal. |
| `CommonPrefix` trait | Strips the source and target sequences of their shared prefix and passes the remainder to the underlying algorithm to speed up processing. |
| `CommonSuffix` trait | Strips the source and target sequences of their shared suffix and passes the remainder to the underlying algorithm to speed up processing. |
| `Context` trait | The diff algorithm implementations only return information about the necessary deletions and insertions. Mix in this trait to add additional context information that can be used to better determine the actual position of the edits while patching. The context size defaults to 3 but can be adjusted in a custom instance by overriding the `contextSize` field. |

A custom implementation is created by instantiating an algorithm and a combination of the available traits.
```scala
scala> import de.digitalistbesser.diff.algorithms._
import de.digitalistbesser.diff.algorithms._

scala> val diff1 = new MillerMyersDiffAlgorithm[Vector[String], String]
diff1: de.digitalistbesser.diff.algorithms.MillerMyersDiffAlgorithm[Vector[String],String] = de.digitalistbesser.diff.algorithms.MillerMyersDiffAlgorithm@2f6bbeb0

scala> diff1.diff(Vector("123", "456", "789"), Vector("789", "456", "123"))
res0: Seq[de.digitalistbesser.diff.Hunk[String]] = List(Hunk(0,0,List(Delete(123), Delete(456))), Hunk(3,1,List(Insert(456), Insert(123))))

scala> val diff2 = new MillerMyersDiffAlgorithm[Vector[String], String] with Context[Vector[String], String]
diff2: de.digitalistbesser.diff.algorithms.MillerMyersDiffAlgorithm[Vector[String],String] with de.digitalistbesser.diff.algorithms.Context[Vector[String],String] = $anon$1@217dc48e

scala> diff2.diff(Vector("123", "456", "789"), Vector("789", "456", "123"))
res1: Seq[de.digitalistbesser.diff.Hunk[String]] = List(Hunk(0,0,List(Delete(123), Delete(456), Match(789), Insert(456), Insert(123))))
```
The first diff algorithm produces two hunks that contain only deletions and insertions. The second algorithm results in a single hunk that combines the edits with additional context information.

The default implementation uses the `MyersSpaceOptimizedDiffAlgorithm` with the `CommonPrefix`, `CommonSuffix` and `Context` traits with their respective default settings.

The basic patching/unpatching algorithm is implemented in the abstract `de.digitalistbesser.diff.PatchAlgorithm` class. A trait that provides an implementation for determining the location of a hunk in the source must be mixed-in with the class. Scaladiff provides the following implementations.

| Name | Description |
| --- | --- |
| `NoMatch` trait | The location is used as specified in the hunk without any attempt to determine a better location. |
| `ContextMatch` trait | The hunk's context is used to determine the location starting at the location specified in the hunk. If the hunk doesn't match it is moved one position in front of the starting location, then one position behind it, then two positions before, then two behind, then three before and so on until a match has been found or the permissible portion of the source has been checked. If no match could be found the first and last element of the hunk's context is removed and the whole process is repeated. The number of times the process is repeated is determined by the `maximumFuzz` setting in `ContextMatch` which defaults to 2. If no match could be found after maximum fuzz is exhausted the hunk is rejected and not applied. Otherwise it is applied at the first location that matches. |

A custom implementation is created by instantiating the basic algorithm implementation and one of the available traits.
```scala
scala> import de.digitalistbesser.diff.default._
import de.digitalistbesser.diff.default._

scala> val hunks = "abcdefghijklmnopqrstuvw" diffTo "abcDefghijkLmnopqrsTuvw"
hunks: Seq[de.digitalistbesser.diff.Hunk[Char]] = List(Hunk(0,0,List(Match(a), Match(b), Match(c), Delete(d), Insert(D), Match(e), Match(f), Match(g))), Hunk(8,8,List(Match(i), Match(j), Match(k), Delete(l), Insert(L), Match(m), Match(n), Match(o))), Hunk(16,16,List(Match(q), Match(r), Match(s), Delete(t), Insert(T), Match(u), Match(v), Match(w))))

scala> import de.digitalistbesser.diff._
import de.digitalistbesser.diff._

scala> import de.digitalistbesser.diff.algorithms._
import de.digitalistbesser.diff.algorithms._

scala> val patchAlgorithm = new PatchAlgorithm[String, Char] with ContextMatch[String, Char]
patchAlgorithm: de.digitalistbesser.diff.PatchAlgorithm[String,Char] with de.digitalistbesser.diff.algorithms.ContextMatch[String,Char] = $anon$1@43f0c2d1

scala> patchAlgorithm.patch("abcdefghpqrstuv", hunks)
res0: de.digitalistbesser.diff.PatchResult[String,Char] = PatchResult(abcDefghpqrsTuv,List(Applied(Hunk(...),ContextOffset(0,0)), Rejected(Hunk(...)), Applied(Hunk(...),ContextOffset(-7,1))))
```
The result of the patch operation contains the resulting string and a list with additional information on whether the hunks were applied or rejected. If they could be applied implementation specific data about the location of the hunk is provided. The first hunk of the example could be applied at the specified location so no additional offset and fuzz was necessary. The second hunk was rejected since its context couldn't be matched in the altered source string. The third hunk was applied with an offset of -7 and a fuzz of 1. The offset stems from the removed middle part of the source that caused the rejection of the second hunk. The necessary fuzz is caused by the removed _w_ at the end of the source that is expected by the hunk's full context but ignored after the context is reduced.

The default patch implementation uses the `ContextMatch` trait with its default settings.

#### Diffs for Custom Data
The diff and patch implementations work on `Seq`. To use the algorithms with custom data structures an implicit conversion that converts the data into a `Seq` must be specified when a diff algorithm is instantiated. A corresponding patch algorithm furthermore needs an implicit `scala.collections.generic.CanBuildFrom` instance for the data type.

#### Diffs for Strings
Scaladiff provides implicit conversions that enable diffing and patching of strings that are treated as `Seq[Char]`.
```scala
scala> import de.digitalistbesser.diff.default._
import de.digitalistbesser.diff.default._

scala> val hunks = "AbC" diffTo "aBc"
hunks: Seq[de.digitalistbesser.diff.Hunk[Char]] = List(Hunk(0,0,List(Delete(A), Delete(b), Delete(C), Insert(a), Insert(B), Insert(c))))

scala> "AbC" patchWith hunks
res0: de.digitalistbesser.diff.PatchResult[String,Char] = PatchResult(aBc,...)
```

#### Diffs for Arrays
Scaladiff provides implicit conversions that enable diffing and patching of arrays.
```scala
scala> import de.digitalistbesser.diff.default._
import de.digitalistbesser.diff.default._

scala> val hunks = Array(1, 2, 3) diffTo Array(2, 3, 4)
hunks: Seq[de.digitalistbesser.diff.Hunk[Int]] = List(Hunk(0,0,List(Delete(1), Match(2), Match(3), Insert(4))))

scala> Array(1, 2, 3) patchWith hunks
res0: de.digitalistbesser.diff.PatchResult[Array[Int],Int] = PatchResult([I@39d77de9,...))

scala> res0.result
res0: Array[Int] = Array(2, 3, 4)
```

#### Determining Differences
The diff and patch algorithms determine equality of the elements of the processed sequences through an `Equiv` instance that is implicitly passed to the `diff`, `patch` and `unpatch` methods. By default the `Equiv` implementation provided by the Scala language is used. To alter this behaviour you can provide an alternative implementation.
```scala
scala> import de.digitalistbesser.diff.algorithms.MillerMyersDiffAlgorithm
import de.digitalistbesser.diff.algorithms.MillerMyersDiffAlgorithm

scala> val diff = new MillerMyersDiffAlgorithm[String, Char]
diff: de.digitalistbesser.diff.algorithms.MillerMyersDiffAlgorithm[String,Char] = de.digitalistbesser.diff.algorithms.MillerMyersDiffAlgorithm@2a2bb0eb

scala> diff.diff("AbC", "aBc")
res0: Seq[de.digitalistbesser.diff.Hunk[Char]] = List(Hunk(0,0,List(Delete(A), Delete(b), Delete(C), Insert(a), Insert(B), Insert(c))))

scala> implicit val ignoreCase = Equiv.fromFunction[Char](_.toLower == _.toLower)
ignoreCase: scala.math.Equiv[Char] = scala.math.Equiv$$anon$4@67110f71

scala> diff.diff("AbC", "aBc")
res1: Seq[de.digitalistbesser.diff.Hunk[Char]] = List()
```
The first invocation of `diff` is executed with the system's default `Equiv` implementation for `Char` and returns a non-empty list of changes since the strings differ in case. The second invocation uses the implicitly provided custom implementation that ignores casing. The algorithm deems both strings equal and returns an empty list of changes.

The same applies for the convenience methods `diffTo`, `patchWith` and `unpatchWith`.
```scala
scala> import de.digitalistbesser.diff.default._
import de.digitalistbesser.diff.default._

scala> "AbC" diffTo "aBc"
res0: Seq[de.digitalistbesser.diff.Hunk[Char]] = List(Hunk(0,0,List(Delete(A), Delete(b), Delete(C), Insert(a), Insert(B), Insert(c))))

scala> implicit val ignoreCase = Equiv.fromFunction[Char](_.toLower == _.toLower)
ignoreCase: scala.math.Equiv[Char] = scala.math.Equiv$$anon$4@5dbf5634

scala> "AbC" diffTo "aBc"
res1: Seq[de.digitalistbesser.diff.Hunk[Char]] = List()
```

_When patching/unpatching a list of hunks you should use the same equality relation that was used to create the hunks._

### Saving and Loading Diffs
Scaladiff provides implementations for reading and writing the unified, context, and normal diff formats. 

#### Unified Format
The following listing showcases how to write a sequence of hunks in the unified format. 
```scala
scala> import de.digitalistbesser.diff.default._
import de.digitalistbesser.diff.default._

scala> val hunks = Vector("123", "abc") diffTo Vector("ABC", "123")
hunks: Seq[de.digitalistbesser.diff.Hunk[String]] = List(Hunk(0,0,List(Insert(ABC), Match(123), Delete(abc))))

scala> import java.io.{BufferedWriter, FileWriter}
import java.io.{BufferedWriter, FileWriter}

scala> val writer = new BufferedWriter(new FileWriter("~/sample.diff"))
writer: java.io.BufferedWriter = java.io.BufferedWriter@36d5c2ce

scala> import de.digitalistbesser.diff.io.unified._
import de.digitalistbesser.diff.io.unified._

scala> write(writer, HunkData("sourceHeader", "targetHeader", hunks))
res0: de.digitalistbesser.diff.io.WriteResult = WriteSuccess

scala> writer.close()
```
The source and target headers used by the unified format are passed along with the hunks to the `write` method in a `HunkData` instance. The method returns the `WriteSuccess` object on success and a `WriteFailed` instance with the exception if the write operation failed. A successful write of the previous example results in the following output.
```udiff
--- sourceHeader
+++ targetHeader
@@ -1,2 +1,2 @@
+ABC
 123
-abc
```
The source and target header are written as specified in the `HunkData` instance. All custom formatting must be done by the caller.

Reading hunks is done through the `read` method.
```scala
scala> import java.io.{BufferedReader, FileReader}
import java.io.{BufferedReader, FileReader}

scala> val reader = new BufferedReader(new FileReader("~/sample.diff"))
reader: java.io.BufferedReader = java.io.BufferedReader@6f6efa4f

scala> read(reader)
res1: de.digitalistbesser.diff.io.ReadResult[de.digitalistbesser.diff.io.unified.Data[String],de.digitalistbesser.diff.io.unified.Line] =
  ReadSuccess(HunkData(sourceHeader,targetHeader,List(Hunk(0,0,List(Insert(ABC), Match(123), Delete(abc))))))

scala> reader.close()
```
The method returns a `ReadSuccess` instance with the corresponding `HunkData` on success. The source and target headers are provided by the `HunkData` as they are found in the file. The interpretation is up to the caller. A `ReadFailure` instance with the exception and line number is returned if the operation fails.

#### Context Format
To read and write diffs in the context format use `import de.digitalistbesser.diff.io.context._`.

The same rules as for the unified format also apply for the context format. The example above results in the output.
```
*** sourceHeader
--- targetHeader
***************
*** 1,2 ****
  123
- abc
--- 1,2 ----
+ ABC
  123
```

#### Normal Format
To read and write diffs in the normal format use `import de.digitalistbesser.diff.io.normal._`.

The normal format doesn't support source and target headers. Therefore the `HunkData` for this format only contains the hunks.
```scala
scala> write(writer, HunkData(hunks))
res0: de.digitalistbesser.diff.io.WriteResult = WriteSuccess
```
The normal format also doesn't support context. All context information is stripped from the hunks when they are written and is not restored when a diff in the normal format is read. The example above results in the output.
```
0a1
> ABC
2d2
< abc
```

#### Output and Input Formatting
The `read` and `write` methods of the formats rely on implicitly provided functions that transform the hunks from and to the output format respectively. The previous example used the identity function since the input and output data were strings.

Custom transformation functions can be provided as implicit values.
```scala
scala> // imports of diff & creation of writer as before

scala> val hunks = Vector[Byte](0, 1, 2, 4, 8, 16, 31, 64) diffTo Vector[Byte](0, 1, 2, 4, 8, 16, 32, 64)
hunks: Seq[de.digitalistbesser.diff.Hunk[Byte]] = List(Hunk(3,3,List(Match(4), Match(8), Match(16), Delete(31), Insert(32), Match(64))))

scala> import de.digitalistbesser.diff.io.unified._
import de.digitalistbesser.diff.io.unified._

scala> write(writer, HunkData("sourceHeader", "targetHeader", hunks))
<console>:21: error: No implicit view available from Byte => String.
       write(writer, HunkData("sourceHeader", "targetHeader", hunks))
            ^

scala> implicit val byte2String: Byte => String = "0x%02X" format _
byte2String: Byte => String = <function1>

scala> write(writer, HunkData("sourceHeader", "targetHeader", hunks))
res0: de.digitalistbesser.diff.io.WriteResult = WriteSuccess

scala> writer.close()
```
The first attempt to write the hunks fails since no matching conversion from `Byte` to `String` can be found. The second attempt uses the custom implementation. The result looks like this.
```udiff
--- sourceHeader
+++ targetHeader
@@ -4,5 +4,5 @@
 0x04
 0x08
 0x10
-0x1F
+0x20
 0x40
```
To read the hunks in the correct format another transformation function is necessary. Furthermore the target type needs to be specified when invoking `read`.
```scala
scala> // imports & creation of reader as before

scala> read(reader)
res1: de.digitalistbesser.diff.io.ReadResult[de.digitalistbesser.diff.io.unified.Data[String],de.digitalistbesser.diff.io.unified.Line] = ReadSuccess(HunkData(sourceHeader,targetHeader,List(Hunk(3,3,List(Match(0x04), Match(0x08), Match(0x10), Delete(0x1F), Insert(0x20), Match(0x40))))))

scala> reader.close()

scala> // create new reader

scala> implicit val string2Byte: String => Byte = s => java.lang.Integer.parseInt(s.substring(2), 16).toByte
string2Byte: String => Byte = <function1>

scala> read[Byte](reader)
res2: de.digitalistbesser.diff.io.ReadResult[de.digitalistbesser.diff.io.unified.Data[Byte],de.digitalistbesser.diff.io.unified.Line] = ReadSuccess(HunkData(sourceHeader,targetHeader,List(Hunk(3,3,List(Match(4), Match(8), Match(16), Delete(31), Insert(32), Match(64))))))

scala> reader.close()
```
The first invocation again uses the identity function and yields hunks of type `String`. The second invocation explicitly specifies the output type `Byte` and the provided transformation is used. The result fits the source and target sequences and can be used to patch or unpatch them.

## Performance Considerations
_At the moment no special performance enhancements and benchmarks are implemented._

My main goal for this project is learning a thing or two about the diff and patch algorithms and practice Scala while doing so. The algorithms have been tested with smaller inputs (1000 to 10000 element sequences) and performed reasonably well (as far as I am concerned). Implementing the algorithms correctly has priority over speed. Large inputs will result in long running times and probably cause memory problems. Handle with care!

But there is one word of advice: the implemented diff algorithms randomly access the elements in the source and target sequences. To speed up processing use sequence implementations that provide fast random access.

## Hat Tip
Hat tip to Neil Fraser whose writing about [diff](https://neil.fraser.name/writing/diff/) and [patch](https://neil.fraser.name/writing/patch/) was a valuable source for the work on this project. 

## License
Licensed under the [Apache 2.0 license](./LICENSE).

---

_Enjoy!_
