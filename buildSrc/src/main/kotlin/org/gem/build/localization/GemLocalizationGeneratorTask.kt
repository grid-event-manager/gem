package org.gem.build.localization

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GemLocalizationGeneratorTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val generatedSourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val reportDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val sources = GemLocalizationPropertiesParser.parseDirectory(sourceDirectory.get().asFile)
        GemLocalizationKotlinWriter.write(
            sources = sources,
            generatedSourceRoot = generatedSourceDirectory.get().asFile,
            reportDirectory = reportDirectory.get().asFile,
        )
    }
}
