name := "FB_REST_API_1"

version := "2.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/"
)

libraryDependencies ++= {
  val akkaV = "2.3.13"
  val sprayV = "1.3.3"
  val sprayJsonV = "1.3.2"
  val specs2V = "3.6.5"
  val scalazV = "7.1.5"
  val scalacryptV = "0.4.0"

  Seq(
    "io.spray"            %%  "spray-can"     % sprayV withSources() withJavadoc(),
    "io.spray"            %%  "spray-routing" % sprayV withSources() withJavadoc(),
    "io.spray"            %%  "spray-json"    % sprayJsonV withSources() withJavadoc(),
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.specs2"          %%  "specs2-core"   % specs2V % "test",
    "org.scalaz"          %%  "scalaz-core"   % scalazV
//    "xyz.wiedenhoeft"     %%  "scalacrypt"    % scalacryptV
  )
}