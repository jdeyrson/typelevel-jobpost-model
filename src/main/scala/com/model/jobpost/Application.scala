package com.model.jobpost

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.IO
import cats.effect.IOApp
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import com.model.jobpost.http.HttpApi
import com.model.jobpost.config.*
import com.model.jobpost.config.syntax.*
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
object Application extends IOApp.Simple {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  override def run = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
    EmberServerBuilder
      .default[IO]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(HttpApi[IO].endpoints.orNotFound)
      .build
      .use(_ => IO.println("Server ready!") *> IO.never)
  }
}
