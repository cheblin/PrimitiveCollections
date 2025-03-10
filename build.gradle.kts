plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.3"
    id("io.morethan.jmhreport") version "0.9.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openjdk.jol:jol-core:0.17")
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")


    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

jmh {
    fork.set(1)
    timeUnit.set("ns")
    benchmarkMode.set(listOf("avgt"))
    resultFormat.set("JSON")
    resultsFile.set(file("$buildDir/reports/jmh/results.json"))
}

tasks.withType<JavaExec> {
    jvmArgs("-Djdk.attach.allowAttachSelf=true")
}