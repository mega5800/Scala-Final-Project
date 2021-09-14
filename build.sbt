name := "ScalaFinalProject"
version := "1.0"

lazy val `test` = (project in file(".")).enablePlugins(PlayScala)
scalaVersion := "2.13.5"
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
libraryDependencies ++= Seq(
    //  jdbc,
    // ehcache ,
    // ws ,
    guice,
    specs2 % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
    "com.typesafe.play" %% "play-slick" % "5.0.0",
    "com.typesafe.slick" %% "slick-codegen" % "3.3.3",
    "com.typesafe.play" %% "play-json" % "2.9.2",
    "org.postgresql" % "postgresql" % "42.2.23",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
    "org.mindrot" % "jbcrypt" % "0.4",
    "com.typesafe.play" %% "play-mailer" % "8.0.1",
    "com.typesafe.play" %% "play-mailer-guice" % "8.0.1",
    // extremely important async await support, otherwise code is a mess
    "org.scala-lang.modules" %% "scala-async" % "0.10.0",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
)
