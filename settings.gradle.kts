pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Only in settings.gradle in modern Gradle
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.6.0")
}
rootProject.name = "kotter"

include(":kotter")
include(":kotterx:kotter-test-support")

include(":examples:anim")
include(":examples:blink")
include(":examples:border")
include(":examples:chatgpt")
include(":examples:clock")
include(":examples:compiler")
include(":examples:doomfire")
include(":examples:extend")
include(":examples:grid")
include(":examples:input")
include(":examples:keys")
include(":examples:life")
include(":examples:list")
include(":examples:mandelbrot")
include(":examples:native")
include(":examples:picker")
include(":examples:sliding")
include(":examples:snake")
include(":examples:splash")
include(":examples:text")
include(":examples:wordle")

include(":examples:mosaic:counter")
include(":examples:mosaic:jest")
include(":examples:mosaic:robot")
