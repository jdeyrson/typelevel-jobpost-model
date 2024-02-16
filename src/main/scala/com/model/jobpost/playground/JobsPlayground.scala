package com.model.jobpost.playground

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.io.StdIn

import com.model.jobpost.domain.job.*
import com.model.jobpost.core.*

object JobsPlayground extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    company = "Jobpost, Inc",
    title = "Software Engineer",
    description = "Full time",
    externalUrl = "jobpost.com",
    remote = true,
    location = "Latam"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs <- LiveJobs[IO](xa)
      _  <- IO(println("Create a new job entry [Press ENTER to continue...]")) *> IO(StdIn.readLine)
      id <- jobs.create("myemail@jobpost.com", jobInfo)
      _ <- IO(println("New job entry has been published [Press ENTER to see the job...]")) *> IO(
        StdIn.readLine
      )
      list <- jobs.all()
      _ <- IO(
        println(s"Review new job's info: $list. [Press ENTER to update job's title...]")
      ) *> IO(
        StdIn.readLine
      )
      _      <- jobs.update(id, jobInfo.copy(title = "Scala Software Engineer"))
      newJob <- jobs.find(id)
      _ <- IO(
        println(s"Job's title has been updated $newJob. [Press ENTER to delete this job entry...")
      ) *> IO(StdIn.readLine)
      _         <- jobs.delete(id)
      listAfter <- jobs.all()
      _ <- IO(
        println(
          s"Job entry has been deleted. List now: $listAfter. [Press ENTER to exit this program..."
        )
      ) *> IO(StdIn.readLine)
    } yield ()
  }
}
