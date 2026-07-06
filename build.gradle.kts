plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.smartinput"
version = "1.0.0"

repositories {
    mavenLocal()
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    mavenCentral()
}

dependencies {
    // Add your dependencies here
}

intellij {
    version.set("2025.3")
    type.set("IU")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("253.*")
    }
    buildSearchableOptions {
        enabled = false
    }
}
