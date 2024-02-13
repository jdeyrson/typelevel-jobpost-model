package com.model.foundations

import cats.effect.kernel.MonadCancelThrow
import cats.effect.{IO, IOApp}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.ExecutionContexts

// DOCS doobie
// a FP (cats-effect) library for interacting with DBs (persistent store)

/*
READ docker
$ docker-compose up
$ docker ps (process status)
$ docker exec -it typelevel-jobpost-model-db-1 psql -U docker

docker=# create database demo;
docker=# \c demo (connect to this demo DB)

demo=# create table students(id serial not null, name character varying not null, primary key(id));
demo=# select * from students;
demo=# truncate students (deletes the date inside a table, but not the table itself)
 */

object Doobie extends IOApp.Simple {

  case class Student(id: Int, name: String)

// DOCS Transactor data structure and APIs
// A Transactor allow us to interact with the postgres DB
// This transactor can be use to execute sql queries as an effect

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // JDBC connector
    // "jdbc:postgresql://localhost:5432/demo", (add next line for short)
    "jdbc:postgresql:demo", // database URL
    "docker",               // user
    "docker"                // pass
  )

  // read
  def findAllStudentNames: IO[List[String]] = {
    val query  = sql"select name from students".query[String]
    val action = query.to[List]
    action.transact(xa)
  }

  // write
  def saveStudent(id: Int, name: String): IO[Int] = {
    val query  = sql"insert into students(id, name) values ($id, $name)"
    val action = query.update.run
    action.transact(xa)
  }

  // read as Case Classes with fragments
  def findStudentsByInitial(letter: String): IO[List[Student]] = {
    val selectPart = fr"select id, name"
    val fromPart   = fr"from students"
    val wherePart  = fr"where left(name, 1) = $letter"

    val statement = selectPart ++ fromPart ++ wherePart
    val action    = statement.query[Student].to[List]

    action.transact(xa)
  }

  // organize code
  trait Students[F[_]] { // "repository"
    def findById(id: Int): F[Option[Student]]
    def findAll: F[List[Student]]
    def create(name: String): F[Int]
  }

  object Students {
    def make[F[_]: MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students[F] {
      override def findById(id: Int): F[Option[Student]] =
        sql"select id, name from students where id=$id".query[Student].option.transact(xa)

      override def findAll: F[List[Student]] =
        sql"select id, name from students".query[Student].to[List].transact(xa)

      override def create(name: String): F[Int] =
        sql"insert into students(name) values ($name)".update
          .withUniqueGeneratedKeys[Int]("id")
          .transact(xa)
    }
  }

  val postgresResource = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](16)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver", // JDBC connector
      "jdbc:postgresql:demo",  // database URL
      "docker",                // user
      "docker",                // pass
      ec
    )
  } yield xa

  val smallProgram = postgresResource.use { xa =>
    val studentsRepo = Students.make[IO](xa)
    for {
      id       <- studentsRepo.create("Jonathan")
      jonathan <- studentsRepo.findById(id)
      _        <- IO.println(s"The first student is $jonathan")
    } yield ()
  }

  override def run: IO[Unit] = smallProgram
}
