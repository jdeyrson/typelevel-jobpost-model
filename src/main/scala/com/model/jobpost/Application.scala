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
import pureconfig.ConfigSource

import com.model.jobpost.config.*
import com.model.jobpost.config.syntax.*
import com.model.jobpost.config.EmberConfig
import com.model.jobpost.http.HttpApi

object Application extends IOApp.Simple {

  val configSource = ConfigSource.default.load[EmberConfig]

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
