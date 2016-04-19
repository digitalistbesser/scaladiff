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

import scala.annotation.tailrec

/** Defines a diff algorithm.
 */
abstract class DiffAlgorithm[TData, TElement : Equiv](implicit
    asSeq: AsSeq[TData, TElement]) {
  /** Denotes a difference between the source and the target.
    */
  protected abstract sealed class Difference

  /** Denotes a deletion of a single element.
    *
    * @param sourceIndex The index of the element in the source.
    * @param targetIndex The index in the target after which the element is missing.
    * @param element The deleted element itself.
    */
  protected case class Deletion(
      sourceIndex: Int,
      targetIndex: Int,
      element: TElement)
    extends Difference

  /** Denotes an insertion of a single element.
    *
    * @param sourceIndex The index in the source after which the element is inserted.
    * @param targetIndex The index of the element in the target.
    * @param element The deleted element itself.
    */
  protected case class Insertion(
      sourceIndex: Int,
      targetIndex: Int,
      element: TElement)
    extends Difference

  /** Computes the differences of the specified source and target.
    *
    * @param source The source data.
    * @param target The target data.
    * @return The hunks of edits necessary to translate the source into the target.
    */
  def diff(
      source: TData,
      target: TData): Seq[Hunk[TElement]] = {
    @tailrec
    def loop(
        differences: Seq[Difference],
        hunks: Seq[Hunk[TElement]] = Nil,
        edits: Seq[Edit[TElement]] = Nil): Seq[Hunk[TElement]] = differences match {
      case Seq(Insertion(s1, _, e1), i @ Insertion(s2, _, _), dt @ _*) if s1 == s2 =>
        loop(i +: dt, hunks, Insert(e1) +: edits)

      case Seq(Insertion(s1, _, e1), d @ Deletion(s2, _, _), dt @ _*) if s1 == s2 + 1 =>
        loop(d +: dt, hunks, Insert(e1) +: edits)

      case Seq(Deletion(s1, _, e1), d @ Deletion(s2, _, _), dt @ _*) if s1 == s2 + 1 =>
        loop(d +: dt, hunks, Delete(e1) +: edits)

      case Seq(Insertion(s, t, e), dt @ _*) =>
        loop(dt, Hunk(s, t, Insert(e) +: edits) +: hunks)

      case Seq(Deletion(s, t, e), dt @ _*) =>
        loop(dt, Hunk(s, t, Delete(e) +: edits) +: hunks)

      case Seq(Insertion(s, t, e)) =>
        Hunk(s, t, Insert(e) +: edits) +: hunks

      case Seq(Deletion(s, t, e)) =>
        Hunk(s, t, Delete(e) +: edits) +: hunks

      case _ =>
        hunks
    }

    loop(this.computeDifferences(source, target))
  }

  /** Computes the differences of the specified source and target sequences.
    *
    * @param source The source sequence.
    * @param target The target sequence.
    * @return The differences between the source and target sequences in reverse order.
    */
  protected def computeDifferences(
      source: Seq[TElement],
      target: Seq[TElement]): Seq[Difference]
}
