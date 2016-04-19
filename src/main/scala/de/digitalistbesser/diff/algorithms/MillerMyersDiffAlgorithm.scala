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

import de.digitalistbesser.diff._

import scala.collection._

/** Diff algorithm implementation after "A File Comparison Program" by Webb Miller and Eugene W. Myers.
  *
  * @see http://onlinelibrary.wiley.com/doi/10.1002/spe.4380151102/abstract
  */
class MillerMyersDiffAlgorithm[TData, TElement : Equiv](implicit
    asSeq: AsSeq[TData, TElement])
  extends DiffAlgorithm[TData, TElement] {
  /** @inheritdoc
    */
  protected def computeDifferences(
      source: Seq[TElement],
      target: Seq[TElement]): Seq[Difference] = {
    val eq = implicitly[Equiv[TElement]]
    import eq.equiv

    // initialize from identical prefixes
    val maxRow = source.length
    val maxColumn = target.length
    var row = 0
    while (row < maxRow && row < maxColumn && equiv(source(row), target(row))) {
      row = row + 1
    }

    // compute diff
    var lower = if (row == maxRow) 1 else -1
    var upper = if (row == maxColumn) -1 else 1
    if (lower <= upper) {
      val actionsForDiagonal = mutable.HashMap[Int, List[Difference]]()
      val rowForDiagonal = mutable.HashMap[Int, Int](0 -> row)
      for (diagonal <- Stream.from(1)) {
        for (offset <- lower to upper by 2 if offset <= upper) {
          // determine next action
          if (offset == -diagonal ||
              (offset != diagonal && rowForDiagonal(offset + 1) >= rowForDiagonal(offset - 1))) {
            row = rowForDiagonal(offset + 1) + 1
            actionsForDiagonal(offset) = Deletion(row - 1, row + offset, source(row - 1)) :: actionsForDiagonal.getOrElse(offset + 1, Nil)
          } else {
            row = rowForDiagonal(offset - 1)
            actionsForDiagonal(offset) = Insertion(row, row + offset - 1, target(row + offset - 1)) :: actionsForDiagonal.getOrElse(offset - 1, Nil)
          }

          // check identical sub-sequence on current diagonal
          var column = row + offset
          while (row < maxRow && column < maxColumn && equiv(source(row), target(column))) {
            row = row + 1
            column = column + 1
          }
          rowForDiagonal(offset) = row

          // check whether processing is finished
          if (row == maxRow && column == maxColumn) {
            return actionsForDiagonal(offset)
          }

          // adjust bounds if any of the input sequences has been processed completely
          if (row == maxRow) {
            lower = offset + 2
          }
          if (column == maxColumn) {
            upper = offset - 2
          }
        }

        lower = lower - 1
        upper = upper + 1
      }
    }

    Seq.empty[Difference]
  }
}
