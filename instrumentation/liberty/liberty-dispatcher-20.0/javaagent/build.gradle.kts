plugins {
  id("otel.javaagent-instrumentation")
}

// liberty and liberty-dispatcher are loaded into different class loaders
// liberty module has access to servlet api while liberty-dispatcher does not

dependencies {
  // liberty jars are not available as a maven dependency so we compile against
  // stub classes
  compileOnly(project(":instrumentation:liberty:compile-stub"))
}