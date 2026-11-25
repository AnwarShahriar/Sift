package io.github.anwarshahriar.sift.engine.bundle

import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """usage: scripts/validate <bundle-dir | file>...
  A bundle dir (e.g. .sift/482 or fixtures/pr-482) is checked file by file.
  Known files: review.json"""

/** Entry point for scripts/validate. Exit 0: all valid. 1: problems found. 2: bad usage. */
fun main(args: Array<String>) {
    if (args.isEmpty() || args.any { it == "-h" || it == "--help" }) {
        System.err.println(USAGE)
        exitProcess(2)
    }
    val validator = BundleValidator()
    var failed = false
    for (arg in args) {
        val targets = targetsFor(File(arg))
        if (targets.isEmpty()) {
            System.err.println("$arg: no bundle files found (known: ${BundleFile.entries.joinToString { it.fileName }})")
            failed = true
        }
        for ((file, kind) in targets) {
            val problems = validator.validate(kind, file.readText())
            if (problems.isEmpty()) {
                println("ok    ${file.path}")
            } else {
                failed = true
                println("FAIL  ${file.path}")
                problems.forEach { println("      $it") }
            }
        }
    }
    exitProcess(if (failed) 1 else 0)
}

private fun targetsFor(path: File): List<Pair<File, BundleFile>> = when {
    path.isDirectory -> BundleFile.entries.map { File(path, it.fileName) to it }.filter { it.first.isFile }
    path.isFile -> listOfNotNull(BundleFile.forFileName(path.name)?.let { path to it })
    else -> emptyList()
}
