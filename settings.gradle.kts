pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Bastion"

// HIM-016: código separado por plataforma en platforms/<plataforma>/. El path del include ya
// mapea al directorio físico por convención de Gradle (":platforms:android" -> platforms/android/).
include(":platforms:android")
include(":core")
// include(":platforms:desktop")  // se agrega en la siguiente fase de HIM-016
// include(":platforms:ios")      // futuro, bloqueado — ver ADR-D4 en HIM-016.spec.md
