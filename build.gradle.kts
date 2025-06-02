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
    implementation("org.yaml:snakeyaml:$snakeyamlVersion")
    implementation("com.hierynomus:sshj:$sshjVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.slf4j:slf4j-nop:$slf4jNopVersion")
}

application {
    mainClass.set("io.labv.sftptransfer.MainCommand")
}
