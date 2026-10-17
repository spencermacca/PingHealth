plugins {
    java
}

group = "co.spencermedia"
version = "1.0.9"
description = "Admin debugging sidebar that shows every online player's ping."

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    // Paper 26.x uses "<mc>.build.<n>-stable" versions instead of "-R0.1-SNAPSHOT".
    // Pinned for reproducible builds; "26.3.build.+" would track the latest build.
    compileOnly("io.papermc.paper:paper-api:26.3.build.200-stable")
}

java {
    // Paper 26.3 requires Java 25.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "description" to project.description,
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
