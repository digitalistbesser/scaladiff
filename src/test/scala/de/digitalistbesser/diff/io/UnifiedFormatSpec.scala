/*
 * Copyright 2016 Thomas Puhl
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

package de.digitalistbesser.diff.io

import de.digitalistbesser.diff.{Delete, Hunk, Insert, Match}
import org.scalatest.FlatSpec
import org.scalatest.Inside._
import org.scalatest.Matchers._

/** Spec implementation for the unified format.
  */
class UnifiedFormatSpec extends FlatSpec {
  /** Unified format implementation for testing purposes.
    */
  private object Format extends UnifiedFormat {
    /** Writer implementation that writes to a sequence of strings.
      */
    private class SeqLineWriter extends LineWriter {
      private val builder = Seq.newBuilder[String]

      /** @inheritdoc
        */
      def writeLine(line: String): Unit = this.builder += line

      /** Returns the written lines.
        */
      def data: Seq[String] = this.builder.result()
    }

    /** Reader implementation that reads from a sequence of strings.
      */
    private class SeqLineReader(data: Seq[String]) extends LineReader {
      private var index = 0

      /** @inheritdoc
        */
      def currentLine: Option[Line] =
        if (this.index >= 0 && this.index < this.data.size) Some(Line(this.data(this.index), this.index + 1))
        else None

      /** @inheritdoc
        */
      def readLine(): Unit = this.index = this.index + 1
    }

    /** @inheritdoc
      */
    def write(
        data: UnifiedData[String])(implicit
        toOutput: ToOutput[String, String]): Seq[String] = {
      val lineWriter = new SeqLineWriter
      this.write(lineWriter, data)(toOutput)
      lineWriter.data
    }

    /** @inheritdoc
      */
    def read(
        data: Seq[String])(implicit
        fromInput: FromInput[String, String]): ReadResult[UnifiedData[String], Line] =
      this.read(new SeqLineReader(data))(fromInput)
  }

  import Format._

  private val casing: Seq[String] = List(
    "--- sourceHeader",
    "+++ targetHeader",
    "@@ -1,2 +1,2 @@",
    "-ABC",
    "-XYZ",
    "+XYZ",
    "+ABC")
  private val empty: Seq[String] = Nil
  private val emptySource: Seq[String] = List(
    "--- sourceHeader",
    "+++ targetHeader",
    "@@ -0,0 +1,3 @@",
    "+a",
    "+b",
    "+c")
  private val emptyTarget: Seq[String] = List(
    "--- sourceHeader",
    "+++ targetHeader",
    "@@ -1,3 +0,0 @@",
    "-a",
    "-b",
    "-c")
  private val multipleHunks: Seq[String] = List(
    "--- sourceHeader",
    "+++ targetHeader",
    "@@ -13,7 +13,7 @@",
    " m",
    " n",
    " o",
    "-p",
    "+P",
    " q",
    " r",
    " s",
    "@@ -21,6 +21,6 @@",
    " u",
    " v",
    " w",
    "-x",
    "+X",
    " y",
    " z")
  private val singleHunk: Seq[String] = List(
    "--- sourceHeader",
    "+++ targetHeader",
    "@@ -2 +2,2 @@",
    "-b",
    "+B",
    "+C")
  private val malformedSourceHeader: Seq[String] = this.singleHunk.updated(0, "-- sourceHeader")
  private val malformedTargetHeader: Seq[String] = this.singleHunk.updated(1, "++ targetHeader")
  private val malformedHunkHeader: Seq[String] = this.singleHunk.updated(2, "@@ 2, +2,2 @@")
  private val malformedHunkData: Seq[String] = this.singleHunk.updated(5, "Test")
  private val missingSourceHeader: Seq[String] = "###" +: this.singleHunk
  private val missingTargetHeader: Seq[String] = this.singleHunk.take(1) ++: "###" +: this.singleHunk.drop(1)
  private val missingHunkHeader: Seq[String] = this.singleHunk.take(2) ++: "###" +: this.singleHunk.drop(2)

  "UnifiedFormat" should "write nothing for empty input" in {
    val patch = write(EmptyData)
    assert(patch == this.empty)
  }
  it should "write a single hunk correctly" in {
    val hunks = Seq(Hunk(1, 1, Seq(Delete("b"), Insert("B"), Insert("C"))))
    val patch = write(HunkData("sourceHeader", "targetHeader", hunks))
    assert(patch == this.singleHunk)
  }
  it should "write multiple hunks correctly" in {
    val hunks = Seq(Hunk(12, 12, Seq(Match("m"), Match("n"), Match("o"), Delete("p"), Insert("P"), Match("q"), Match("r"), Match("s"))), Hunk(20, 20, Seq(Match("u"), Match("v"), Match("w"), Delete("x"), Insert("X"), Match("y"), Match("z"))))
    val patch = write(HunkData("sourceHeader", "targetHeader", hunks))
    assert(patch == this.multipleHunks)
  }
  it should "write an empty source correctly" in {
    val hunks = Seq(Hunk(0, 0, Seq(Insert("a"), Insert("b"), Insert("c"))))
    val patch = write(HunkData("sourceHeader", "targetHeader", hunks))
    assert(patch == this.emptySource)
  }
  it should "write an empty target correctly" in {
    val hunks = Seq(Hunk(0, 0, Seq(Delete("a"), Delete("b"), Delete("c"))))
    val patch = write(HunkData("sourceHeader", "targetHeader", hunks))
    assert(patch == this.emptyTarget)
  }
  it should "write using the supplied output transformation function" in {
    val hunks = Seq(Hunk(0, 0, Seq(Delete("aBc"), Delete("xYz"), Insert("XyZ"), Insert("AbC"))))
    val patch = write(HunkData("sourceHeader", "targetHeader", hunks))(_.toUpperCase)
    assert(patch == this.casing)
  }
  it should "read nothing from an empty file" in {
    val result = read(this.empty)
    inside(result) { case ReadSuccess(EmptyData) =>
    }
  }
  it should "read a single hunk correctly" in {
    val result = read(this.singleHunk)
    inside(result) { case ReadSuccess(HunkData("sourceHeader", "targetHeader", Seq(Hunk(1, 1, Seq(Delete("b"), Insert("B"), Insert("C")))))) =>
    }
  }
  it should "read multiple hunks correctly" in {
    val result = read(this.multipleHunks)
    inside(result) { case ReadSuccess(HunkData("sourceHeader", "targetHeader", Seq(Hunk(12, 12, Seq(Match("m"), Match("n"), Match("o"), Delete("p"), Insert("P"), Match("q"), Match("r"), Match("s"))), Hunk(20, 20, Seq(Match("u"), Match("v"), Match("w"), Delete("x"), Insert("X"), Match("y"), Match("z")))))) =>
    }
  }
  it should "read an empty source correctly" in {
    val result = read(this.emptySource)
    inside(result) { case ReadSuccess(HunkData("sourceHeader", "targetHeader", Seq(Hunk(0, 0, Seq(Insert("a"), Insert("b"), Insert("c")))))) =>
    }
  }
  it should "read an empty target correctly" in {
    val result = read(this.emptyTarget)
    inside(result) { case ReadSuccess(HunkData("sourceHeader", "targetHeader", Seq(Hunk(0, 0, Seq(Delete("a"), Delete("b"), Delete("c")))))) =>
    }
  }
  it should "read using the supplied input transformation function" in {
    val result = read(this.casing)(_.toLowerCase)
    inside(result) { case ReadSuccess(HunkData("sourceHeader", "targetHeader", Seq(Hunk(0, 0, Seq(Delete("abc"), Delete("xyz"), Insert("xyz"), Insert("abc")))))) =>
    }
  }
  it should "fail reading input with missing source header" in {
    val result = read(this.missingSourceHeader)
    inside(result) { case ReadFailure(e: HunkFormatException, Some(Line(l, 1))) =>
      l should equal (this.missingSourceHeader.head)
    }
  }
  it should "fail reading input with missing target header" in {
    val result = read(this.missingTargetHeader)
    inside(result) { case ReadFailure(e: HunkFormatException, Some(Line(l, 2))) =>
      l should equal (this.missingTargetHeader(1))
    }
  }
  it should "fail reading input with missing hunk header" in {
    val result = read(this.missingHunkHeader)
    inside(result) { case ReadFailure(e: HunkFormatException, Some(Line(l, 3))) =>
      l should equal (this.missingHunkHeader(2))
    }
  }
  it should "fail reading input with malformed source header" in {
    val result = read(this.malformedSourceHeader)
    inside(result) { case ReadFailure(e: HunkFormatException, Some(Line(l, 1))) =>
      l should equal (this.malformedSourceHeader.head)
    }
  }
  it should "fail reading input with malformed target header" in {
    val result = read(this.malformedTargetHeader)
    inside(result) { case ReadFailure(e: HunkFormatException, Some(Line(l, 2))) =>
      l should equal (this.malformedTargetHeader(1))
    }
  }
  it should "fail reading input with malformed hunk header" in {
    val result = read(this.malformedHunkHeader)
    inside(result) { case ReadFailure(e: HunkFormatException, Some(Line(l, 3))) =>
      l should equal (this.malformedHunkHeader(2))
    }
  }
  it should "fail reading input with malformed hunk data" in {
    val result = read(this.malformedHunkData)
    inside(result) { case ReadFailure(e: HunkFormatException, Some(Line(l, 6))) =>
      l should equal (this.malformedHunkData(5))
    }
  }
}
