package com.model.jobpost.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.*
import cats.effect.kernel.Concurrent
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

import java.util.UUID
import scala.collection.mutable
import com.model.jobpost.core.*
import com.model.jobpost.domain.job.*
import com.model.jobpost.http.responses.*
import com.model.jobpost.logging.syntax.*

// (jobs: Job[F]) use plain dependency injection to pass the jobs algebra to the job routs
class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends Http4sDsl[F] {

  // POST /jobs?offset=x&limit=y { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    for {
      jobsList <- jobs.all()
      resp     <- Ok(jobsList)
    } yield resp
  }

  // GET /jobs/uuid
  private val findJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found."))
    }
  }

  // POST /jobs/create { jobInfo }
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
        jobId   <- jobs.create("mail@model.com", jobInfo)
        resp    <- Created(jobId)
      } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      for {
        jobInfo     <- req.as[JobInfo]
        maybeNewJob <- jobs.update(id, jobInfo)
        resp <- maybeNewJob match {
          case Some(job) => Ok()
          case None      => NotFound(FailureResponse(s"Cannot update job $id: not found."))
        }
      } yield resp
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ DELETE -> Root / UUIDVar(id) =>
      jobs.find(id).flatMap {
        case Some(job) =>
          for {
            _    <- jobs.delete(id)
            resp <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id: not found."))
      }
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobsRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

// smart constructor
object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
