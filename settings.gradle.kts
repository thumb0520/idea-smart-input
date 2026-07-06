pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.intellij.platform") version "2.5.0"
    }
}

rootProject.name = "smart-plugin"
