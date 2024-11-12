pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(url = "https://europe-maven.pkg.dev/contentsurfacingservice/public")
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from("com.neomo:library-catalog:1.8.4")
        }
    }
}
