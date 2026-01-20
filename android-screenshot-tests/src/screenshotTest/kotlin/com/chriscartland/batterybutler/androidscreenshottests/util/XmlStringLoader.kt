package com.chriscartland.batterybutler.androidscreenshottests.util

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Utility for loading string resources from an XML file for use in screenshot tests.
 *
 * This loader handles:
 * 1. Resolving the file from a relative path (relative to the module root).
 * 2. Parsing the XML to extract <string name="...">value</string> pairs.
 * 3. Caching the results to optimize performance for repeated lookups.
 */
object XmlStringLoader {
    private val cache = mutableMapOf<String, Map<String, String>>()

    fun loadStrings(relativePath: String): Map<String, String> {
        val cached = cache[relativePath]
        if (cached != null) {
            println("XmlStringLoader: Returning cached strings for $relativePath")
            return cached
        }

        // We expect the test to run from the :android-screenshot-tests module directory.
        val file = File(relativePath)

        println("XmlStringLoader: Current working directory: ${File(".").absolutePath}")
        println("XmlStringLoader: Requested relative path: $relativePath")
        println("XmlStringLoader: Attempting to load from absolute path: ${file.absolutePath}")

        if (file.exists()) {
            println("XmlStringLoader: File found. Parsing...")
            val strings = parseXml(file)
            cache[relativePath] = strings
            return strings
        } else {
            println("XmlStringLoader: ERROR - File not found at ${file.absolutePath}")
            val empty = emptyMap<String, String>()
            cache[relativePath] = empty
            return empty
        }
    }

    private fun parseXml(file: File): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(file)
            doc.documentElement.normalize()

            val nodeList = doc.getElementsByTagName("string")
            for (i in 0 until nodeList.length) {
                val node = nodeList.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val element = node as org.w3c.dom.Element
                    val name = element.getAttribute("name")
                    val value = element.textContent
                    map[name] = value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }
}
