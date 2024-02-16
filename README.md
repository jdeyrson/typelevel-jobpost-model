# Job Posting and Application Model

A jobs platform built with Scala 3 and Typelevel Stack

Useful commands

#### Demo

`docker=# create database board;` (Access the docker container's shell)

`$ docker-compose up`

`$ docker ps` (check process status)

`$ docker exec -it typelevel-jobpost-model-db-1 psql -U docker`

**Jobpost program**

`board=# \c board` (connect to this `demo` DB)

`board=# select * from jobs;`

`board=# \dt` (describes the tables)

`board=# q` (to quit the displaying output in the terminal and return to the prompt)

`board=# truncate jobs;` (deletes the date inside a table, but not the table itself)

**HttpApi + Database module integration**

`$ http post localhost:4041/api/jobs/create < src/main/resources/payloads/jobinfo.json`

`$ http get localhost:4041/api/jobs/{UUIDVar}`

`$ http get localhost:4041/api/jobs/{UUIDVar}` (enter a UUID with modified a digit to test error response)

`$ http put localhost:4041/api/jobs/{UUIDVar} < src/main/resources/payloads/jobinfo2.json` (update previous entry based on jobinfo2)

`$ http delete localhost:4041/api/jobs/{UUIDVar}`

## Getting Started

- Scala 3.x
- SBT
- PostgreSQL
- JDK 11