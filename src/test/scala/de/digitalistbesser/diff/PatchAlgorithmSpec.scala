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

package de.digitalistbesser.diff

import org.scalatest.Inside._
import org.scalatest.Matchers._
import org.scalatest._

/** Specification implementation for patch algorithms.
  */
class PatchAlgorithmSpec
  extends FunSuite {
  private val patchAlgorithm = new PatchAlgorithm[String, Char]
  private val combinations = Map(
    "abc" -> List.empty[Hunk[Char]],
    "Abc" -> List(Hunk(0, 0, List(Delete('a'), Insert('A')))),
    "aBc" -> List(Hunk(1, 1, List(Delete('b'), Insert('B')))),
    "abC" -> List(Hunk(2, 2, List(Delete('c'), Insert('C')))),
    "ABc" -> List(Hunk(0, 0, List(Delete('a'), Delete('b'), Insert('A'), Insert('B')))),
    "AbC" -> List(Hunk(0, 0, List(Delete('a'), Insert('A'))), Hunk(2, 2, List(Delete('c'), Insert('C')))),
    "aBC" -> List(Hunk(1, 1, List(Delete('b'), Delete('c'), Insert('B'), Insert('C')))),
    "ABC" -> List(Hunk(0, 0, List(Delete('a'), Delete('b'), Delete('c'), Insert('A'), Insert('B'), Insert('C')))),
    "bc" -> List(Hunk(0, 0, List(Delete('a')))),
    "ac" -> List(Hunk(1, 1, List(Delete('b')))),
    "ab" -> List(Hunk(2, 2, List(Delete('c')))),
    "c" -> List(Hunk(0, 0, List(Delete('a'), Delete('b')))),
    "b" -> List(Hunk(0, 0, List(Delete('a'))), Hunk(2, 2, List(Delete('c')))),
    "a" -> List(Hunk(1, 1, List(Delete('b'), Delete('c')))),
    "" -> List(Hunk(0, 0, List(Delete('a'), Delete('b'), Delete('c')))),
    " abc" -> List(Hunk(0, 0, List(Insert(' ')))),
    "abc " -> List(Hunk(3, 3, List(Insert(' ')))),
    " abc " -> List(Hunk(0, 0, List(Insert(' '))), Hunk(3, 4, List(Insert(' ')))),
    "ab c" -> List(Hunk(2, 2, List(Insert(' ')))),
    "a bc" -> List(Hunk(1, 1, List(Insert(' ')))),
    "a b c" -> List(Hunk(1, 1, List(Insert(' '))), Hunk(2, 3, List(Insert(' ')))),
    " a b c " -> List(Hunk(0, 0, List(Insert(' '))), Hunk(1, 2, List(Insert(' '))), Hunk(2, 4, List(Insert(' '))), Hunk(3, 6, List(Insert(' ')))),
    " ac" -> List(Hunk(0, 0, List(Insert(' '))), Hunk(1, 2, List(Delete('b')))),
    "ac " -> List(Hunk(1, 1, List(Delete('b'))), Hunk(3, 2, List(Insert(' ')))),
    " ac " -> List(Hunk(0, 0, List(Insert(' '))), Hunk(1, 2, List(Delete('b'))), Hunk(3, 3, List(Insert(' ')))),
    " a c" -> List(Hunk(0, 0, List(Insert(' '))), Hunk(1, 2, List(Delete('b'), Insert(' ')))),
    "a c " -> List(Hunk(1, 1, List(Delete('b'), Insert(' '))), Hunk(3, 3, List(Insert(' '))))
  )

  this.combinations.foreach { case (target, hunks) =>
    def assertAllHunksApplied(
        hunks: Seq[Hunk[Char]],
        results: Map[Hunk[Char], HunkResult]): Unit = hunks.foreach { h =>
      assert(results.getOrElse(h, fail()) == Applied(h.sourceIndex))
    }

    test(s"${patchAlgorithm.getClass.getSimpleName} should yield '$target' for patching 'abc' with $hunks") {
      val result = this.patchAlgorithm.patch("abc", hunks)
      inside(result) { case PatchResult(d, m) =>
        d should equal (target)
        assertAllHunksApplied(hunks, m)
      }
    }
  }

  this.combinations.foreach { case (target, hunks) =>
    def assertAllHunksUnapplied(
        hunks: Seq[Hunk[Char]],
        results: Map[Hunk[Char], HunkResult]): Unit = hunks.foreach { h =>
      assert(results.getOrElse(h, fail()) == Applied(h.targetIndex))
    }

    test(s"${patchAlgorithm.getClass.getSimpleName} should yield 'abc' for unpatching '$target' with $hunks") {
      val result = this.patchAlgorithm.unpatch(target, hunks)
      inside(result) { case PatchResult("abc", m) =>
        assertAllHunksUnapplied(hunks, m)
      }
    }
  }
}
