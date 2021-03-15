/*
 * Copyright (c) 2020-2021 by The Monix Connect Project Developers.
 * See the project homepage at: https://connect.monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.connect.redis.domain

import io.lettuce.core.{Range => R}

final class ZRange[T](private[redis] val underlying: R[T])

object ZRange {

  def apply[T](lower: T, upper: T): ZRange[T] =
    new ZRange(R.from(R.Boundary.including(lower), R.Boundary.including(upper)))

  def create[T](lower: T, upper: T): ZRange[T] = new ZRange(R.create(lower, upper))

  def unbounded[T](): ZRange[T] = new ZRange(R.unbounded())

  def lt[T](upperLimit: T): ZRange[T] = new ZRange(R.from(R.Boundary.unbounded(), R.Boundary.excluding(upperLimit)))

  def lte[T](upperLimit: T): ZRange[T] = new ZRange(R.from(R.Boundary.unbounded(), R.Boundary.including(upperLimit)))

  def gt[T](lowerLimit: T): ZRange[T] = new ZRange(R.from(R.Boundary.excluding(lowerLimit), R.Boundary.unbounded()))

  def gte[T](lowerLimit: T): ZRange[T] = new ZRange(R.from(R.Boundary.including(lowerLimit), R.Boundary.unbounded()))

}