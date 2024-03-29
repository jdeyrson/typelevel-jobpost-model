package com.model.jobpost.modules

import cats.effect.*
import com.model.jobpost.config.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database {
  def apply[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[F](
      "org.postgresql.Driver",
      "jdbc:postgresql:board", // TODO move to config
      "docker",
      "docker",
      ec
    )
  } yield xa
}
