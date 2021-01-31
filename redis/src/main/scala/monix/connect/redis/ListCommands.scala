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

package monix.connect.redis

import io.lettuce.core.api.reactive.RedisListReactiveCommands
import monix.eval.Task
import monix.reactive.Observable


private[redis] class ListCommands[K, V](reactiveCmd: RedisListReactiveCommands[K, V]) {

  /**
    * Remove and get the first element in a list, or block until one is available.
    * @return A null multi-bulk when no element could be popped and the timeout expired.
    *         A two-element multi-bulk with the first element being the name of the key
    *         where an element was popped and the second element being the value of the popped element.
    */
  def bLPop(timeout: Long, keys: K*): Task[(K, Option[V])] =
    Task.from(reactiveCmd.blpop(timeout, keys: _*)).map(kvToTuple)

  /**
    * Remove and get the last element in a list, or block until one is available.
    * @return A null multi-bulk when no element could be popped and the timeout expired.
    *          A two-element multi-bulk with the first element being the name of the key
    *          where an element was popped and the second element being the value of the popped element.
    */
  def bRPop(timeout: Long, keys: K*): Task[(K, Option[V])] =
    Task.from(reactiveCmd.brpop(timeout, keys: _*)).map(kvToTuple)

  /**
    * Pop a value from a list, push it to another list and return it; or block until one is available.
    * @return The element being popped from source and pushed to destination.
    */
  def bRPopLPush(timeout: Long, source: K, destination: K): Task[V] =
    Task.from(reactiveCmd.brpoplpush(timeout, source, destination))

  /**
    * Get an element from a list by its index.
    * @return The requested element, or null when index is out of range.
    */
  def lIndex(key: K, index: Long): Task[V] =
    Task.from(reactiveCmd.lindex(key, index))

  /**
    * Insert an element before or after another element in a list.
    * @return The length of the list after the insert operation, or -1 when the value pivot was not found.
    */
  def lInsert(key: K, before: Boolean, pivot: V, value: V): Task[Long] =
    Task.from(reactiveCmd.linsert(key, before, pivot, value)).map(_.longValue)

  /**
    * Get the length of a list.
    * @return Long integer-reply the length of the list at { @code key}.
    */
  def lLen(key: K): Task[Long] =
    Task.from(reactiveCmd.llen(key)).map(_.longValue)

  /**
    * Remove and get the first element in a list.
    * @return The value of the first element, or null when key does not exist.
    */
  def lPop(key: K): Task[V] =
    Task.from(reactiveCmd.lpop(key))

  /**
    * Prepend one or multiple values to a list.
    * @return The length of the list after the push operations.
    */
  def lPush(key: K, values: V*): Task[Long] =
    Task.from(reactiveCmd.lpush(key, values: _*)).map(_.longValue)

  /**
    * Prepend values to a list, only if the list exists.
    * @return The length of the list after the push operation.
    */
  def lPushX(key: K, values: V*): Task[Long] =
    Task.from(reactiveCmd.lpushx(key, values: _*)).map(_.longValue)

  /**
    * Get a range of elements from a list.
    * @return List of elements in the specified range.
    */
  def lRange(key: K, start: Long, stop: Long): Observable[V] =
    Observable.fromReactivePublisher(reactiveCmd.lrange(key, start, stop))

  /**
    * Remove elements from a list.
    * @return The number of removed elements.
    */
  def lRem(key: K, count: Long, value: V): Task[Long] =
    Task.from(reactiveCmd.lrem(key, count, value)).map(_.longValue)

  /**
    * Set the value of an element in a list by its index.
    * @return The same inserted value
    */
  def lSet(key: K, index: Long, value: V): Task[String] =
    Task.from(reactiveCmd.lset(key, index, value))

  /**
    * Trim a list to the specified range.
    * @return Simple string reply
    */
  def lTrim(key: K, start: Long, stop: Long): Task[String] =
    Task.from(reactiveCmd.ltrim(key, start, stop))

  /**
    * Remove and get the last element in a list.
    * @return The value of the last element, or null when key does not exist.
    */
  def rPop(key: K): Task[V] =
    Task.from(reactiveCmd.rpop(key))

  /**
    * Remove the last element in a list, append it to another list and return it.
    * @return The element being popped and pushed.
    */
  def rPopLPush(source: K, destination: K): Task[V] =
    Task.from(reactiveCmd.rpoplpush(source, destination))

  /**
    * Append one or multiple values to a list.
    * @return The length of the list after the push operation.
    */
  def rPush(key: K, values: V*): Task[Long] =
    Task.from(reactiveCmd.rpush(key, values: _*)).map(_.longValue)

  /**
    * Append values to a list, only if the list exists.
    * @return The length of the list after the push operation.
    */
  def rPushX(key: K, values: V*): Task[Long] =
    Task.from(reactiveCmd.rpushx(key, values: _*)).map(_.longValue)
}

object ListCommands {
  def apply[K, V](reactiveCmd: RedisListReactiveCommands[K, V]): ListCommands[K, V] =
    new ListCommands[K, V](reactiveCmd)
}
