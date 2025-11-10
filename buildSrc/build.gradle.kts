plugins {
    `kotlin-dsl`
}
repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle)
}
