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

/** The result of a patch operation.
  *
  * @param data The resulting data.
  * @param hunks A map containing the results for the single hunks that were processed by the operation.
  */
final case class PatchResult[TData, TElement](
    data: TData,
    hunks: Map[Hunk[TElement], HunkResult])
