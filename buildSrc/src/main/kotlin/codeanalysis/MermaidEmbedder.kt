package codeanalysis

import java.io.File

object MermaidEmbedder {
    fun embed(
        target: File,
        mmdFileName: String,
        mermaidContent: String,
    ) {
        val beginMarker = "<!-- GENERATED:BEGIN $mmdFileName -->"
        val endMarker = "<!-- GENERATED:END $mmdFileName -->"
        val lines = target.readLines()
        val output = StringBuilder()
        var skipping = false

        for (line in lines) {
            when {
                line == beginMarker -> {
                    output.appendLine(line)
                    output.appendLine("```mermaid")
                    output.appendLine(mermaidContent)
                    output.appendLine("```")
                    skipping = true
                }
                line == endMarker -> {
                    skipping = false
                    output.appendLine(line)
                }
                !skipping -> output.appendLine(line)
            }
        }

        target.writeText(output.toString())
    }
}
