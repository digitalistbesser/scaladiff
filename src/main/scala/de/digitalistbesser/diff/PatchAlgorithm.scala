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
import scala.collection.mutable

/** Defines a patch algorithm.
  */
class PatchAlgorithm[TData, TElement : Equiv](implicit
    protected val asSeq: AsSeq[TData, TElement],
    protected val asData: AsData[TData, TElement]) {
  /** Applies the specified hunks to the data.
    *
    * @param data The source data.
    * @param hunks The hunks to apply.
    * @return The result containing the result data and information on the single hunks.
    */
  def patch(
      data: TData,
      hunks: Seq[Hunk[TElement]]): PatchResult[TData, TElement] = this.computePatch(data, hunks)

  /** Unapplies the specified hunks from the data.
    *
    * @param data The target data.
    * @param hunks The hunks to unapply.
    * @return The result containing the result data and information on the single hunks
    */
  def unpatch(
      data: TData,
      hunks: Seq[Hunk[TElement]]): PatchResult[TData, TElement] = {
    def invertHunk(hunk: Hunk[TElement]): Hunk[TElement] = Hunk(
      hunk.targetIndex,
      hunk.sourceIndex,
      hunk.edits.map {
        case Insert(i) => Delete(i)
        case Delete(i) => Insert(i)
        case e => e
      })

    this.computePatch(data, hunks.map(invertHunk)) match { case PatchResult(d, m) =>
        PatchResult(
          d,
          m.map { case (h, r) => (invertHunk(h), r) })
    }
  }

  /** Computes the patch for the sequence from the specified hunks.
    *
    * @param seq The original sequence.
    * @param hunks The hunks to unapply.
    * @return The result containing the result data and information on the single hunks.
    */
  protected def computePatch(
      seq: Seq[TElement],
      hunks: Seq[Hunk[TElement]]): PatchResult[TData, TElement] = {
    @tailrec
    def loop(
        seq: Seq[TElement],
        hunks: Seq[Hunk[TElement]],
        offset: Int,
        builder: mutable.Builder[TElement, TData],
        results: mutable.Builder[(Hunk[TElement], HunkResult), Map[Hunk[TElement], HunkResult]]): PatchResult[TData, TElement] = hunks match {
      case Seq(h @ Hunk(s, _, e), ht @ _*) =>
        val length = this.computeOffset(seq, e, s + offset)
          .map { o =>
            builder ++= seq.view(0, o)
            e.foreach {
              case Insert(l) =>
                builder += l

              case Match(l) =>
                builder += l

              case _ =>
            }

            results += ((h, Applied(o - offset)))
            o + h.sourceLength
          }
          .getOrElse {
            results += ((h, Rejected))
            0
          }
        loop(seq.drop(length), ht, offset - length, builder, results)

      case _ =>
        builder ++= seq

        PatchResult(builder.result(), results.result())
    }

    loop(seq, hunks, 0, dataBuilder[TData, TElement], Map.newBuilder[Hunk[TElement], HunkResult])
  }

  /** Computes the offset of the specified edits in the sequence.
    *
    * @param seq The target sequence.
    * @param edits The edits to apply.
    * @param offset The initial start offset in the sequence that shall be used for the search of the best fitting offset.
    * @return The start offset in the sequence or None if the position of the edits cannot be determined.
    */
  protected def computeOffset(
      seq: Seq[TElement],
      edits: Seq[Edit[TElement]],
      offset: Int): Option[Int] = Some(offset)
}