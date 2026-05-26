plugins {
    kotlin("jvm") version "1.9.23"
    application
}

val picocliVersion = "4.7.5"
val snakeyamlVersion = "2.2"
val sshjVersion = "0.38.0"
val commonsIoVersion = "2.19.0"
val slf4jNopVersion = "2.0.12"

group = "io.labv"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:$picocliVersion")
    annotationProcessor("info.picocli:picocli-codegen:$picocliVersion")

    implementation("org.yaml:snakeyaml:$snakeyamlVersion")
    implementation("com.hierynomus:sshj:$sshjVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.slf4j:slf4j-nop:$slf4jNopVersion")
}

application {
    mainClass.set("io.labv.sftptransfer.MainCommand")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("labv-sftp-transfer")
    archiveVersion.set("")
}

tasks.withType<JavaCompile> {
    options.annotationProcessorGeneratedSourcesDirectory = file("$buildDir/generated/sources/annotationProcessor/java/main")
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

sourceSets["main"].java {
    srcDir("$buildDir/generated/sources/annotationProcessor/java/main")
}

tasks.register<Copy>("copyNativeImageConfigs") {
    from("$buildDir/classes/java/main/META-INF/native-image")
    into("$buildDir/native-image-configs")
}

tasks.register<Jar>("fatJar") {
    dependsOn("build")
    archiveBaseName.set("labv-sftp-transfer-all")
    archiveVersion.set("")

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map {
            zipTree(it).matching {
                exclude(
                    "META-INF/*.SF",
                    "META-INF/*.DSA",
                    "META-INF/*.RSA",
                    "META-INF/MANIFEST.MF",
                    "META-INF/INDEX.LIST"
                )
            }
        }
    })
}
