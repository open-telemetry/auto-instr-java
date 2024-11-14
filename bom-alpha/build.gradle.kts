plugins {
  id("otel.bom-conventions")
}

description = "OpenTelemetry Instrumentation Bill of Materials (Alpha)"
group = "io.opentelemetry.instrumentation"
base.archivesName.set("opentelemetry-instrumentation-bom-alpha")

javaPlatform {
  allowDependencies()
}

dependencies {
  api(platform("io.opentelemetry:opentelemetry-bom"))
  api(platform("io.opentelemetry:opentelemetry-bom-alpha"))
  api(platform(project(":bom")))

  // Get the semconv version from :dependencyManagement
  val semconvConstraint = project(":dependencyManagement").dependencyProject.configurations["api"].allDependencyConstraints
    .find { it.group.equals("io.opentelemetry.semconv")
            && it.name.equals("opentelemetry-semconv") }
    ?: throw Exception("semconv constraint not found")
  val semconvVersion = semconvConstraint.version ?: throw Exception("missing version")
  otelBom.addExtra(semconvConstraint.group, semconvConstraint.name, semconvVersion)
  otelBom.addExtra(semconvConstraint.group, "opentelemetry-semconv-incubating", semconvVersion)
}

otelBom.projectFilter.set { it.findProperty("otel.stable") != "true" }
