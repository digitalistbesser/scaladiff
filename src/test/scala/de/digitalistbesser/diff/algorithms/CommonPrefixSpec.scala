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

package de.digitalistbesser.diff.algorithms

import de.digitalistbesser.diff.DiffAlgorithm
import org.scalatest.FlatSpec

/** Spec implementation for the common prefix trait implementation.
  */
class CommonPrefixSpec
  extends FlatSpec {
  /** Tests whether the common prefix is stripped properly.
    */
  private class DummyDiffAlgorithm(
      expectedSource: Seq[Char],
      expectedTarget: Seq[Char])(implicit
      equiv: Equiv[Char])
    extends DiffAlgorithm[String, Char] {
    /** Checks the the supplied sequences against the expected ones.
      */
    protected def computeDifferences(
        source: Seq[Char],
        target: Seq[Char]): Seq[Difference] =  {
      assert(expectedSource == source)
      assert(expectedTarget == target)

      Nil
    }
  }

  "CommonPrefix" should "not strip elements from inputs without common prefix" in {
    val diffAlgorithm = new DummyDiffAlgorithm("abc", "xyz") with CommonPrefix[String, Char]
    diffAlgorithm.diff("abc", "xyz")
  }
  it should "strip common elements from the start of both inputs" in {
    val diffAlgorithm = new DummyDiffAlgorithm("abcde", "xyz") with CommonPrefix[String, Char]
    diffAlgorithm.diff("123 abcde", "123 xyz")
  }
  it should "use the supplied Equiv instance to match the source and target elements" in {
    implicit val equiv = Equiv.fromFunction[Char]((l, r) => l.toLower == r.toLower)
    val diffAlgorithm = new DummyDiffAlgorithm("abcde", "xyz") with CommonPrefix[String, Char]
    diffAlgorithm.diff("aBc abcde", "abC xyz")
  }
}
