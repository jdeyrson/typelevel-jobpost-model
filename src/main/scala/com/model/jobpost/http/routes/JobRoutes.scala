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

import java.util.UUID
import scala.collection.mutable
import com.model.jobpost.domain.job.*
import com.model.jobpost.http.responses.*

class JobRoutes[F[_]: Concurrent] private extends Http4sDsl[F] {

  // "database"
  private val database = mutable.Map[UUID, Job]()

  // POST /jobs?offset=x&limit=y { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok(database.values)
  }

  // GET /jobs/uuid
  private val findJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    database.get(id) match {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found."))
    }
  }

  // POST /jobs/create { jobInfo }
  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "TODO@model.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo]
        job     <- createJob(jobInfo)
        resp    <- Created(job.id)
      } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo]
            _       <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
            resp    <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot update job $id: not found."))
      }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ DELETE -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo]
            _       <- database.remove(id).pure[F]
            resp    <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id: not found."))
      }
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobsRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent] = new JobRoutes[F]
}
