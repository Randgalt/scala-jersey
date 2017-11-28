name := "scala-jersey"

version := "0.1"

scalaVersion := "2.11.8"

val jerseyVersion = "2.26"
val swaggerVersion = "1.5.12"
val jacksonVersion = "2.8.4"

projectDependencies ++= Seq(
  "org.glassfish.jersey.containers" % "jersey-container-jetty-http" % jerseyVersion,
  "org.glassfish.jersey.inject" % "jersey-hk2" % jerseyVersion,
  "org.glassfish.jersey.media" % "jersey-media-json-jackson" % jerseyVersion,
  "io.swagger" % "swagger-annotations" % swaggerVersion,
  "io.swagger" % "swagger-models" % swaggerVersion,
  "org.glassfish.jersey.test-framework.providers" % "jersey-test-framework-provider-jetty" % jerseyVersion % "test",
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion exclude("org.scala-lang", "*"),
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jaxrs" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion exclude("org.scala-lang", "*"),
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)