val stableVersion = "1.23.0-SNAPSHOT"
val alphaVersion = "1.23.0-alpha-SNAPSHOT"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}