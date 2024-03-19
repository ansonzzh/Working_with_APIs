ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "Working_with_APIs"
  )

libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M11"
libraryDependencies += "com.lihaoyi" %% "upickle" % "3.2.0"
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.5.2"
