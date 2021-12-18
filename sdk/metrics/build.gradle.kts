import ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer

plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")

  id("otel.jmh-conventions")

  id("otel.animalsniffer-conventions")
}

description = "OpenTelemetry SDK Metrics"
otelJava.moduleName.set("io.opentelemetry.sdk.metrics")

dependencies {
  api(project(":api:metrics"))
  api(project(":sdk:common"))

  compileOnly("org.codehaus.mojo:animal-sniffer-annotations")

  annotationProcessor("com.google.auto.value:auto-value")

  testAnnotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":sdk:metrics-testing"))
  testImplementation(project(":sdk:testing"))
  testImplementation("com.google.guava:guava")

  jmh(project(":sdk:trace"))
}

testing {
  suites {
    val debugEnabledTest by registering(JvmTestSuite::class) {
      targets {
        all {
          testTask.configure {
            jvmArgs("-Dotel.experimental.sdk.metrics.debug=true")
          }
        }
      }
    }
  }
}

tasks {
  named<AnimalSniffer>("animalsnifferMain") {
    // We cannot use IgnoreJreRequirement since it does not work correctly for fields.
    // https://github.com/mojohaus/animal-sniffer/issues/131
    exclude("**/concurrent/Jre*Adder*")
  }

  check {
    dependsOn(testing.suites)
  }
}

jmh {
  // includes.set(listOf("io.opentelemetry.sdk.metrics.internal.state.ExponentialCounterBenchmarks"))
  includes.set(listOf("io.opentelemetry.sdk.metrics.internal.aggregator.HistogramBenchmark"))
  // includes.set(listOf("io.opentelemetry.sdk.metrics.MetricsBenchmarks"))

  val valueGen = objects.listProperty<String>()
  valueGen.add("RANDOM_WITHIN_2K")

  val aggregation = objects.listProperty<String>()
  aggregation.add("EXPONENTIAL")
  // profilers.add("stack:period=1;lines=20;top=5;detailLine=true")
  benchmarkParameters.put("valueGen", valueGen)
  // benchmarkParameters.put("aggregation", aggregation)
  // mapOf("valueGen" to listOf("RANDOM_WITHIN_2K"), "aggregation" to listOf("EXPONENTIAL")))
}
