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

package monix.connect.redis.client

import cats.effect.Resource
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.{RedisClient, RedisURI}
import monix.eval.Task

/**
  * An object that provides an aggregation of all the different Redis Apis.
  * They can be equally accessed independently or from this object.
  */
object Redis {

  def connect(uri: String): Resource[Task, RedisCmd[String, String]] =
    RedisCmd.connectResource[String, String, StatefulRedisConnection[String, String]] {
      Task.evalAsync(RedisClient.create(uri).connect)
    }.evalMap(RedisCmd.single)

   def connect(uri: RedisUri): Resource[Task, RedisCmd[String, String]] =
     RedisCmd.connectResource {
       Task.evalAsync(RedisClient.create(uri.toJava).connect)
     }.evalMap(RedisCmd.single)

  //def connect[K, V](uri: RedisURI, codec: RedisCodec[K, V]): Resource[Task, RedisCmd[K, V]] =
  //  RedisCmd.connectResource {
  //    Task.evalAsync(RedisClient.create(uri).connect(codec))
  //  }.evalMap(RedisCmd.single(_))

}
