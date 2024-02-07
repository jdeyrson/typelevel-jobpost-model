package com.model.jobpost.http

import cats.effect.*
import cats.effect.kernel.Concurrent
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import com.model.jobpost.http.routes.*

class HttpApi[F[_]: Concurrent] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F].routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent] = new HttpApi[F]
}
