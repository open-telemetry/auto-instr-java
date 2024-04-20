val stableVersion = "1.33.2"
val alphaVersion = "1.33.2-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
