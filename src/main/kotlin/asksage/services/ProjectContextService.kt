package asksage.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor

@Service(Service.Level.PROJECT)
class ProjectContextService(private val project: Project) {
    fun getProjectName(): String = project.name
    fun getProjectBasePath(): String? = project.basePath
    fun getProjectSummary(): String {
        val extensions = detectFileExtensions()
        val languages = inferLanguages(extensions)
        return buildString {
            append("Project: ${project.name}")
            if (languages.isNotEmpty()) {
                append(" | Languages: ${languages.joinToString(", ")}")
            }
        }
    }
    fun buildContextPrefix(): String = "[Context: ${getProjectSummary()}]\n\n"
    private fun detectFileExtensions(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        val roots = ProjectRootManager.getInstance(project).contentSourceRoots
        for (root in roots) {
            VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Void>(limit(MAX_FILE_SCAN)) {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isDirectory) {
                        val ext = file.extension?.lowercase()
                        if (ext != null && ext !in IGNORED_EXTENSIONS) {
                            counts[ext] = (counts[ext] ?: 0) + 1
                        }
                    }
                    return true
                }
            })
        }
        return counts.toList().sortedByDescending { it.second }.take(TOP_EXTENSIONS).toMap()
    }
    private fun inferLanguages(extensions: Map<String, Int>): List<String> {
        val languages = mutableSetOf<String>()
        for (ext in extensions.keys) {
            EXTENSION_TO_LANGUAGE[ext]?.let { languages.add(it) }
        }
        return languages.toList()
    }
    companion object {
        private const val MAX_FILE_SCAN = 500
        private const val TOP_EXTENSIONS = 10
        private val IGNORED_EXTENSIONS = setOf(
            "class", "jar", "pyc", "pyo", "o", "so", "dll", "exe",
            "png", "jpg", "jpeg", "gif", "svg", "ico", "woff", "woff2", "ttf", "eot",
            "lock", "map",
        )
        private val EXTENSION_TO_LANGUAGE = mapOf(
            "kt" to "Kotlin", "kts" to "Kotlin", "java" to "Java", "py" to "Python",
            "js" to "JavaScript", "ts" to "TypeScript", "tsx" to "TypeScript", "jsx" to "JavaScript",
            "rb" to "Ruby", "go" to "Go", "rs" to "Rust", "cpp" to "C++", "c" to "C", "h" to "C/C++",
            "cs" to "C#", "swift" to "Swift", "scala" to "Scala", "groovy" to "Groovy", "php" to "PHP",
            "html" to "HTML", "css" to "CSS", "scss" to "SCSS", "xml" to "XML", "yaml" to "YAML",
            "yml" to "YAML", "json" to "JSON", "sql" to "SQL", "sh" to "Shell", "bash" to "Shell", "md" to "Markdown",
        )
    }
}
