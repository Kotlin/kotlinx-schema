plugins {
    `kotlin-dsl`
}
repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.gradle.maven.publish.plugin)
}
