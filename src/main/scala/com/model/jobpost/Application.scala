package com.model.jobpost

import cats.*
import cats.effect.*
import cats.effect.IO
import cats.effect.IOApp
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.ember.server.EmberServerBuilder

import com.model.jobpost.http.routes.HealthRoutes

object Application extends IOApp.Simple {

  override def run = EmberServerBuilder
    .default[IO]
    .withHttpApp(HealthRoutes[IO].routes.orNotFound)
    .build
    .use(_ => IO.println("Server ready!") *> IO.never)
}
