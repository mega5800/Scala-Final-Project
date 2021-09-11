lazy val `scalafinalproject` = (project in file(".")).enablePlugins(PlayScala).settings(
    name := "ScalaFinalProject",
    version := "1.0",
    scalaVersion := "2.13.5",
    resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",
    libraryDependencies ++= Seq( 
        // jdbc ,
        // ehcache ,
        // ws ,
        specs2 % Test ,
        guice, 
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
        "com.typesafe.play" %% "play-slick" % "5.0.0",
        "com.typesafe.slick" %% "slick-codegen" % "3.3.2",
        "com.typesafe.play" %% "play-json" % "2.8.1",
        "org.postgresql" % "postgresql" % "42.2.11",
        "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
        "org.mindrot" % "jbcrypt" % "0.4"
        // "com.google.firebase" % "firebase-admin" % "5.5.0",
        // "org.apache.hadoop" % "hadoop-client" % "2.7.2"
        // "com.google.guava" % "guava" % "30.1.1-jre",
        // "org.apache.httpcomponents" % "httpcore" % "4.4.8"
        )
)