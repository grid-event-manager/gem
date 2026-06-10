package org.hostess.apps.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

internal object DesktopTestDirectoryCleaner {
    fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) {
            return
        }
        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }
}
