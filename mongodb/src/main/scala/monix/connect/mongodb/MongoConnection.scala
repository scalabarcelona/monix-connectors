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

package monix.connect.mongodb

import cats.effect.Resource
import com.mongodb.MongoClientSettings
import monix.connect.mongodb.domain.{Collection, MongoConnector, Tuple2F, Tuple3F, Tuple4F, Tuple5F, Tuple6F}
import monix.eval.Task
import com.mongodb.reactivestreams.client.MongoClient
import monix.connect.mongodb.domain.connection.Connection
import monix.execution.annotations.UnsafeBecauseImpure

case class MongoConnection[T <: Product](connector: T)

/**
  * Exposes the signatures to create a connection to the desired mongo collection,
  * the connection to such collection is returned in form of [[MongoConnector]],
  * which is based of three different components, the db, source, single and sink.
  *
  * The aim is to provide a idiomatic interface for the different operations that
  * can be run against the collection, for either read with [[MongoSource]] or to
  * write/delete one by one with [[MongoSingle]] or in streaming fashion
  * with the [[MongoSink]].
  *
  */
object MongoConnection {

  /**
    * Creates a single [[MongoConnector]] from the passed [[Collection]].
    *
    * ==Example==
    * {{{
    *   import com.mongodb.client.model.Filters
    *   import monix.eval.Task
    *   import monix.connect.mongodb.domain.{Collection, MongoConnector}
    *   import monix.connect.mongodb.MongoConnection
    *   import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
    *
    *   case class Employee(name: String, age: Int, companyName: String = "X")
    *
    *   val employee = Employee("Stephen", 32)
    *   val employeesCol = Collection("myDb", "employees", classOf[Employee], createCodecProvider[Employee]())
    *   val connection = MongoConnection.create1("mongodb://localhost:27017", employeesCol)
    *
    *   val t: Task[Employee] =
    *   connection.use { case MongoConnector(db, source, single, sink) =>
    *     // business logic here
    *     single.insertOne(employee)
    *       .flatMap(_ => source.find(Filters.eq("name", employee.name)).headL)
    *   }
    * }}}
    *
    * @param connectionString describes the hosts, ports and options to be used.
    *                         @see for more information on how to configure it:
    *                         https://mongodb.github.io/mongo-java-driver/3.9/javadoc/com/mongodb/ConnectionString.html
    *                         https://mongodb.github.io/mongo-java-driver/3.7/driver/tutorials/connect-to-mongodb/
    * @param collection describes the collection that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]].
    */
  def create1[T1](connectionString: String, collection: Collection[T1]): Resource[Task, MongoConnector[T1]] =
    Connection[T1].create(connectionString, collection)

  /**
    *
    * Creates a single [[MongoConnector]] from the passed [[Collection]].
    *
    * ==Example==
    * {{{
    *   import com.mongodb.client.model.Filters
    *   import monix.eval.Task
    *   import com.mongodb.{MongoClientSettings, ServerAddress}
    *   import monix.connect.mongodb.MongoConnection
    *   import monix.connect.mongodb.domain.{Collection, MongoConnector}
    *   import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
    *
    *   import scala.jdk.CollectionConverters._
    *
    *   case class Employee(name: String, age: Int, companyName: String = "X")
    *
    *   val employee = Employee("Stephen", 32)
    *   val employeesCol = Collection("myDb", "employees", classOf[Employee], createCodecProvider[Employee]())
    *
    *   val mongoClientSettings = MongoClientSettings.builder
    *       .applyToClusterSettings(builder => builder.hosts(List(new ServerAddress("localhost", 27017)).asJava))
    *       .build
    *
    *   val connection = MongoConnection.create1(mongoClientSettings, employeesCol)
    *   val t: Task[Employee] =
    *   connection.use { case MongoConnector(db, source, single, sink) =>
    *     // business logic here
    *     single.insertOne(employee)
    *       .flatMap(_ => source.find(Filters.eq("name", employee.name)).headL)
    *   }
    * }}}
    *
    * @param clientSettings various settings to control the behavior the created [[MongoConnector]].
    * @param collection describes the collection that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]].
    */
  def create1[T1](clientSettings: MongoClientSettings, collection: Collection[T1]): Resource[Task, MongoConnector[T1]] =
    Connection[T1].create(clientSettings, collection)

  /**
    * Creates a single [[MongoConnector]] from the specified [[Collection]].
    *
    * WARN: It is unsafe because it directly expects an instance of [[MongoClient]],
    * which might have already been closed, alternatively it will be released
    * and closed towards the usage of the resource task.
    * Always prefer to use [[create1]].
    *
    * @param client an instance of [[MongoClient]]
    * @param collection describes the collection that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]
    */
  def createUnsafe1[T1](client: MongoClient, collection: Collection[T1]): Resource[Task, MongoConnector[T1]] =
    Connection[T1].createUnsafe(client, collection)

  /**
    * Creates a connection to mongodb and provides with a [[MongoConnector]]
    * for each of the *TWO* provided [[Collection]]s.
    *
    * ==Example==
    * {{{
    *   import monix.eval.Task
    *   import monix.connect.mongodb.domain.{Collection, MongoConnector}
    *   import monix.connect.mongodb.MongoConnection
    *   import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
    *
    *   case class Employee(name: String, age: Int, companyName: String = "X")
    *   case class Company(name: String, employees: List[Employee], investment: Int = 0)
    *
    *   val employee1 = Employee("Gerard", 39)
    *   val employee2 = Employee("Laura", 41)
    *   val company = Company("Stephen", List(employee1, employee2))
    *
    *   val employeesCol = Collection("business", "employees_collection", classOf[Employee], createCodecProvider[Employee]())
    *   val companiesCol = Collection("business", "companies_collection", classOf[Company], createCodecProvider[Company](), createCodecProvider[Employee]())
    *
    *   val connection = MongoConnection.create2("mongodb://localhost:27017", (employeesCol, companiesCol))
    *
    *   val t: Task[Unit] =
    *   connection.use { case (MongoConnector(_, employeeSource, employeeSingle, employeeSink),
    *                          MongoConnector(_, companySource, companySingle, companySink)) =>
    *     // business logic here
    *     for {
    *       r1 <- employeeSingle.insertMany(List(employee1, employee2))
    *       r2 <- companySingle.insertOne(company)
    *     } yield ()
    *   }
    * }}}
    *
    * @param connectionString describes the hosts, ports and options to be used.
    *                         @see for more information on how to configure it:
    *                         https://mongodb.github.io/mongo-java-driver/3.9/javadoc/com/mongodb/ConnectionString.html
    *                         https://mongodb.github.io/mongo-java-driver/3.7/driver/tutorials/connect-to-mongodb/
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]s.
    */
  def create2[T1, T2](
    connectionString: String,
    collections: Tuple2F[Collection, T1, T2]): Resource[Task, Tuple2F[MongoConnector, T1, T2]] =
    Connection[T1, T2].create(connectionString, collections)

  /**
    * Creates a connection to mongodb and provides with a [[MongoConnector]]
    * for each of the *TWO* provided [[Collection]]s.
    *
    * ==Example==
    *
    * {{{
    *   import monix.eval.Task
    *   import monix.connect.mongodb.domain.{Collection, MongoConnector}
    *   import monix.connect.mongodb.MongoConnection
    *   import monix.connect.mongodb.domain.MongoConnector
    *   import com.mongodb.{MongoClientSettings, ServerAddress}
    *   import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
    *   import scala.jdk.CollectionConverters._
    *
    *   case class Employee(name: String, age: Int, companyName: String = "X")
    *   case class Company(name: String, employees: List[Employee], investment: Int = 0)
    *
    *   val employee1 = Employee("Gerard", 39)
    *   val employee2 = Employee("Laura", 41)
    *   val company = Company("Stephen", List(employee1, employee2))
    *
    *   val employeesCol = Collection("business", "employees_collection", classOf[Employee], createCodecProvider[Employee]())
    *   val companiesCol = Collection("business", "companies_collection", classOf[Company], createCodecProvider[Company](), createCodecProvider[Employee]())
    *
    *   val mongoClientSettings = MongoClientSettings.builder
    *       .applyToClusterSettings(builder => builder.hosts(List(new ServerAddress("localhost", 27017)).asJava))
    *       .build
    *
    *   val connection = MongoConnection.create2(mongoClientSettings, (employeesCol, companiesCol))
    *
    *   val t: Task[Unit] =
    *   connection.use { case (MongoConnector(_, employeeSource, employeeSingle, employeeSink),
    *                          MongoConnector(_, companySource, companySingle, companySink)) =>
    *     // business logic here
    *     for {
    *       r1 <- employeeSingle.insertMany(List(employee1, employee2))
    *       r2 <- companySingle.insertOne(company)
    *     } yield ()
    *   }
    * }}}
    *
    * @param clientSettings various settings to control the behavior of the created [[MongoConnector]]s.
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]].
    */
  def create2[T1, T2](
    clientSettings: MongoClientSettings,
    collections: Tuple2F[Collection, T1, T2]): Resource[Task, Tuple2F[MongoConnector, T1, T2]] =
    Connection[T1, T2].create(clientSettings, collections)

  /**
    * Unsafely creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *TWO* provided [[Collection]]s.
    *
    * WARN: It is unsafe because it directly expects an instance of [[MongoClient]],
    * which might have already been closed, or alternatively it will be released
    * and closed towards the usage of the resource task.
    * Always prefer to use [[create2]].
    *
    * @param client an instance of [[MongoClient]]
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]
    */
  @UnsafeBecauseImpure
  def createUnsafe2[T1, T2](
    client: MongoClient,
    collections: Tuple2F[Collection, T1, T2]): Resource[Task, Tuple2F[MongoConnector, T1, T2]] =
    Connection[T1, T2].createUnsafe(client, collections)

  //3

  /**
    * Creates a connection to mongodb and provides a [[MongoConnector]]
    * for each of the *THREE* provided [[Collection]]s.
    *
    * ==Example==
    * {{{
    *   import com.mongodb.client.model.{Filters, Updates}
    *   import monix.eval.Task
    *   import monix.connect.mongodb.domain.{Collection, MongoConnector}
    *   import monix.connect.mongodb.MongoConnection
    *   import monix.connect.mongodb.domain.{MongoConnector, UpdateResult}
    *   import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
    *
    *   import scala.concurrent.duration._
    *   import scala.jdk.CollectionConverters._
    *
    *   case class Employee(name: String, age: Int, companyName: String)
    *   case class Company(name: String, employees: List[Employee], investment: Int = 0)
    *   case class Investor(name: String, funds: Int, companies: List[Company])
    *
    *   val companiesCol = Collection(
    *         "my_db",
    *         "companies_collection",
    *         classOf[Company],
    *         createCodecProvider[Company](),
    *         createCodecProvider[Employee]())
    *   val employeesCol =
    *     Collection("my_db", "employees_collection", classOf[Employee], createCodecProvider[Employee]())
    *   val investorsCol = Collection(
    *     "my_db",
    *     "investors_collection",
    *     classOf[Investor],
    *     createCodecProvider[Investor](),
    *     createCodecProvider[Company]())
    *
    *   val mongoEndpoint = "mongodb://localhost:27017"
    *   val connection = MongoConnection.create3(mongoEndpoint, (companiesCol, employeesCol, investorsCol))
    *
    *   //in this example we are trying to move the employees and investment
    *   //from an old company a the new one, presumably, there is already a `Company`
    *   //with name `OldCompany` which also have `Employee`s and `Investor`s.
    *
    *   val updateResult: Task[UpdateResult] = connection.use {
    *     case (
    *         MongoConnector(_, companySource, companySingle, companySink),
    *         MongoConnector(_, employeeSource, employeeSingle, employeeSink),
    *         MongoConnector(_, investorSource, investorSingle, _)) =>
    *       for {
    *         // creates the new company
    *         _ <- companySingle.insertOne(Company("NewCompany", employees = List.empty, investment = 0)).delayResult(1.second)
    *
    *         //read employees from old company and pushes them into the new one
    *         _ <- {
    *           employeeSource
    *             .find(Filters.eq("companyName", "OldCompany"))
    *             .bufferTimedAndCounted(2.seconds, 15)
    *             .map { employees =>
    *               // pushes them into the new one
    *               (Filters.eq("name", "NewCompany"),
    *                 Updates.pushEach("employees", employees.asJava))
    *             }
    *             .consumeWith(companySink.updateOne())
    *         }
    *         // sums all the investment funds of the old company and updates the total company's investment
    *         investment <- investorSource.find(Filters.in("companies.name", "OldCompany")).map(_.funds).sumL
    *         updateResult <- companySingle.updateMany(
    *           Filters.eq("name", "NewCompany"),
    *           Updates.set("investment", investment))
    *       } yield updateResult
    *   }
    * }}}
    *
    * @param connectionString describes the hosts, ports and options to be used.
    *                         @see for more information on how to configure it:
    *                         https://mongodb.github.io/mongo-java-driver/3.9/javadoc/com/mongodb/ConnectionString.html
    *                         https://mongodb.github.io/mongo-java-driver/3.7/driver/tutorials/connect-to-mongodb/
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]s.
    */
  def create3[T1, T2, T3](
    connectionString: String,
    collections: Tuple3F[Collection, T1, T2, T3]): Resource[Task, Tuple3F[MongoConnector, T1, T2, T3]] =
    Connection[T1, T2, T3].create(connectionString, collections)

  /**
    * Creates a connection to mongodb and provides a [[MongoConnector]]
    * for each of the *THREE* provided [[Collection]]s.
    *
    * @param clientSettings various settings to control the behavior of the created [[MongoConnector]]s.
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]].
    */
  def create3[T1, T2, T3](
    clientSettings: MongoClientSettings,
    collections: Tuple3F[Collection, T1, T2, T3]): Resource[Task, Tuple3F[MongoConnector, T1, T2, T3]] =
    Connection[T1, T2, T3].create(clientSettings, collections)

  /**
    * Unsafely creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *TWO* provided [[Collection]]s.
    *
    * WARN: It is unsafe because it directly expects an instance of [[MongoClient]],
    * which will be released and closed towards the usage of the resource task.
    * Always prefer to use [[create2]].
    *
    * @param client an instance of [[MongoClient]]
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]
    */
  def createUnsafe3[T1, T2, T3](
    client: MongoClient,
    collections: Tuple3F[Collection, T1, T2, T3]): Resource[Task, Tuple3F[MongoConnector, T1, T2, T3]] =
    Connection[T1, T2, T3].createUnsafe(client, collections)

  /**
    * Creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *FOUR* provided [[Collection]]s.
    *
    * @see an example of usage could be extrapolated from the scaladoc
    *      example for [[create1]], [[create2]] and [[create3]].
    *
    * @param connectionString describes the hosts, ports and options to be used.
    *                         @see for more information on how to configure it:
    *                         https://mongodb.github.io/mongo-java-driver/3.9/javadoc/com/mongodb/ConnectionString.html
    *                         https://mongodb.github.io/mongo-java-driver/3.7/driver/tutorials/connect-to-mongodb/
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]s.
    */
  def create4[T1, T2, T3, T4](
    connectionString: String,
    collections: Tuple4F[Collection, T1, T2, T3, T4]): Resource[Task, Tuple4F[MongoConnector, T1, T2, T3, T4]] =
    Connection[T1, T2, T3, T4].create(connectionString, collections)

  /**
    * Creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *FOUR* provided [[Collection]]s.
    *
    * @see an example of usage could be extrapolated from the scaladoc
    *      example for [[create1]], [[create2]] and [[create3]].
    *
    * @param clientSettings various settings to control the behavior of the created [[MongoConnector]]s.
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]].
    */
  def create4[T1, T2, T3, T4](
    clientSettings: MongoClientSettings,
    collections: Tuple4F[Collection, T1, T2, T3, T4]): Resource[Task, Tuple4F[MongoConnector, T1, T2, T3, T4]] =
    Connection[T1, T2, T3, T4].create(clientSettings, collections)

  /**
    * Unsafely creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *FOUR* provided [[Collection]]s.
    *
    * WARN: It is unsafe because it directly expects an instance of [[MongoClient]],
    * which will be released and closed towards the usage of the resource task.
    * Always prefer to use [[create2]].
    *
    * @param client an instance of [[MongoClient]]
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]
    */
  def createUnsafe4[T1, T2, T3, T4](
    client: MongoClient,
    collections: Tuple4F[Collection, T1, T2, T3, T4]): Resource[Task, Tuple4F[MongoConnector, T1, T2, T3, T4]] =
    Connection[T1, T2, T3, T4].createUnsafe(client, collections)

  /**
    * Creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *FIVE* provided [[Collection]]s.
    *
    * @see an example of usage could be extrapolated from the scaladoc
    *      example for [[create1]], [[create2]] and [[create3]].
    *
    * @param connectionString describes the hosts, ports and options to be used.
    *                         @see for more information on how to configure it:
    *                         https://mongodb.github.io/mongo-java-driver/3.9/javadoc/com/mongodb/ConnectionString.html
    *                         https://mongodb.github.io/mongo-java-driver/3.7/driver/tutorials/connect-to-mongodb/
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]s.
    */
  def create5[T1, T2, T3, T4, T5](
    connectionString: String,
    collections: Tuple5F[Collection, T1, T2, T3, T4, T5]): Resource[Task, Tuple5F[MongoConnector, T1, T2, T3, T4, T5]] =
    Connection[T1, T2, T3, T4, T5].create(connectionString, collections)

  /**
    * Creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *FIVE* provided [[Collection]]s.
    *
    * @see an example of usage could be extrapolated from the scaladoc
    *      example for [[create1]], [[create2]] and [[create3]].
    *
    * @param clientSettings various settings to control the behavior of the created [[MongoConnector]]s.
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]].
    */
  def create5[T1, T2, T3, T4, T5](
    clientSettings: MongoClientSettings,
    collections: Tuple5F[Collection, T1, T2, T3, T4, T5]): Resource[Task, Tuple5F[MongoConnector, T1, T2, T3, T4, T5]] =
    Connection[T1, T2, T3, T4, T5].create(clientSettings, collections)

  /**
    * Unsafely creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *FIVE* provided [[Collection]]s.
    *
    * WARN: It is unsafe because it directly expects an instance of [[MongoClient]],
    * which will be released and closed towards the usage of the resource task.
    * Always prefer to use [[create2]].
    *
    * @param client an instance of [[MongoClient]]
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]
    */
  def createUnsafe5[T1, T2, T3, T4, T5](
    client: MongoClient,
    collections: Tuple5F[Collection, T1, T2, T3, T4, T5]): Resource[Task, Tuple5F[MongoConnector, T1, T2, T3, T4, T5]] =
    Connection[T1, T2, T3, T4, T5].createUnsafe(client, collections)

  /**
    * Creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *SIX* provided [[Collection]]s.
    *
    * @see an example of usage could be extrapolated from the scaladoc
    *      example for [[create1]], [[create2]] and [[create3]].
    *
    * @param connectionString describes the hosts, ports and options to be used.
    *                         @see for more information on how to configure it:
    *                         https://mongodb.github.io/mongo-java-driver/3.9/javadoc/com/mongodb/ConnectionString.html
    *                         https://mongodb.github.io/mongo-java-driver/3.7/driver/tutorials/connect-to-mongodb/
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]s.
    */
  def create6[T1, T2, T3, T4, T5, T6](
    connectionString: String,
    collections: Tuple6F[Collection, T1, T2, T3, T4, T5, T6])
    : Resource[Task, Tuple6F[MongoConnector, T1, T2, T3, T4, T5, T6]] =
    Connection[T1, T2, T3, T4, T5, T6].create(connectionString, collections)

  /**
    * Creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *SIX* provided [[Collection]]s.
    *
    * @see an example of usage could be extrapolated from the scaladoc
    *      example for [[create1]], [[create2]] and [[create3]].
    *
    * @param clientSettings various settings to control the behavior of the created [[MongoConnector]]s.
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]].
    */
  def create6[T1, T2, T3, T4, T5, T6](
    clientSettings: MongoClientSettings,
    collections: Tuple6F[Collection, T1, T2, T3, T4, T5, T6])
    : Resource[Task, Tuple6F[MongoConnector, T1, T2, T3, T4, T5, T6]] =
    Connection[T1, T2, T3, T4, T5, T6].create(clientSettings, collections)

  /**
    * Unsafely creates a connection to mongodb and provides a [[MongoConnector]]
    * to each of the *FOUR* provided [[Collection]]s.
    *
    * WARN: It is unsafe because it directly expects an instance of [[MongoClient]],
    * which will be released and closed towards the usage of the resource task.
    * Always prefer to use [[create2]].
    *
    * @param client an instance of [[MongoClient]]
    * @param collections describes the set of collections that wants to be used (db, collectionName, codecs...)
    * @return a [[Resource]] that provides a single [[MongoConnector]] instance, linked to the specified [[Collection]]
    */
  def createUnsafe6[T1, T2, T3, T4, T5, T6](
    client: MongoClient,
    collections: Tuple6F[Collection, T1, T2, T3, T4, T5, T6])
    : Resource[Task, Tuple6F[MongoConnector, T1, T2, T3, T4, T5, T6]] =
    Connection[T1, T2, T3, T4, T5, T6].createUnsafe(client, collections)

}