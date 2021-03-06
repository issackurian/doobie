// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package hikari

import cats.effect._
import cats.implicits._
import com.zaxxer.hikari.HikariDataSource
import scala.concurrent.ExecutionContext

object HikariTransactor {

  /** Construct a `HikariTransactor` from an existing `HikariDatasource`. */
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def apply[M[_]: Async: ContextShift](
    hikariDataSource : HikariDataSource,
    connectEC:         ExecutionContext,
    transactEC:        ExecutionContext
  ): HikariTransactor[M] =
    Transactor.fromDataSource[M](hikariDataSource, connectEC, transactEC)

  /** Resource yielding an unconfigured `HikariTransactor`. */
  def initial[M[_]: Async: ContextShift](
    connectEC:  ExecutionContext,
    transactEC: ExecutionContext
  ): Resource[M, HikariTransactor[M]] = {
    val alloc = Async[M].delay(new HikariDataSource)
    val free = (ds: HikariDataSource) => Async[M].delay(ds.close())
    Resource.make(alloc)(free).map(Transactor.fromDataSource[M](_, connectEC, transactEC))
  }

  /** Resource yielding a new `HikariTransactor` configured with the given info. */
  def newHikariTransactor[M[_]: Async: ContextShift](
    driverClassName: String,
    url:             String,
    user:            String,
    pass:            String,
    connectEC:       ExecutionContext,
    transactEC:      ExecutionContext
  ): Resource[M, HikariTransactor[M]] =
    for {
      _ <- Resource.liftF(Async[M].delay(Class.forName(driverClassName)))
      t <- initial[M](connectEC, transactEC)
      _ <- Resource.liftF {
            t.configure { ds =>
              Async[M].delay {
                ds setJdbcUrl  url
                ds setUsername user
                ds setPassword pass
              }
            }
          }
    } yield t

}
