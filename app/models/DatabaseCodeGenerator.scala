package models

object DatabaseCodeGenerator extends App {
    slick.codegen.SourceCodeGenerator.run(
        profile = "slick.jdbc.PostgresProfile",
        jdbcDriver = "org.postgresql.Driver",
        url = "jdbc:postgresql://localhost/scaladb?user=scala&password=scala",
        outputDir = "C:\\Users\\Valerian\\Desktop\\CS_bullshit\\projects\\Scala-Final-Project\\app",
        pkg = "models", user = None, password = None, ignoreInvalidDefaults = true, outputToMultipleFiles = false
    )
    
}