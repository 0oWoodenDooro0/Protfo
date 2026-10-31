plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("checkNoEmojis") {
    group = "verification"
    description = "Checks that no emojis are present in Kotlin and XML source files per project standards."
    doLast {
        val emojiRegex = Regex("[\\uD83C-\\uD83E][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]")
        val sourceDirs = listOf(
            file("composeApp/src"),
            file("shared/src")
        )
        val violations = mutableListOf<String>()
        sourceDirs.filter { it.exists() }.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }.forEach { file ->
                file.useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        if (emojiRegex.containsMatchIn(line)) {
                            violations.add("${file.relativeTo(rootDir)}:${index + 1}: ${line.trim()}")
                        }
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Violation: Found emojis in source files (project requires zero emojis):\n" + violations.joinToString("\n"))
        }
    }
}

