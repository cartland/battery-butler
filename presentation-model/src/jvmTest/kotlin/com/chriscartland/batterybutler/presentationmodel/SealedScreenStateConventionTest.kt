package com.chriscartland.batterybutler.presentationmodel

import java.io.File
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Convention test: every sealed interface in the presentation-model module
 * must have a name ending with "ScreenState" and must have at least one
 * concrete subtype.
 *
 * This ensures naming consistency and catches empty sealed hierarchies
 * that would be unusable at runtime.
 */
class SealedScreenStateConventionTest {
    private val packages = listOf(
        "com.chriscartland.batterybutler.presentationmodel",
    )

    @Test
    fun `every sealed interface name ends with ScreenState`() {
        val sealedInterfaces = packages.flatMap { pkg ->
            discoverSealedInterfaces(pkg)
        }

        assertTrue(
            sealedInterfaces.isNotEmpty(),
            "Expected to find sealed interfaces in packages $packages, but found none. " +
                "Ensure the test is running against compiled sources.",
        )

        val violations = sealedInterfaces.filterNot { klass ->
            (klass.simpleName ?: "").endsWith("ScreenState")
        }

        if (violations.isNotEmpty()) {
            fail(
                "Sealed interfaces not following the '*ScreenState' naming convention:\n" +
                    violations.joinToString("\n") { "  - ${it.qualifiedName}" } +
                    "\n\nRename each sealed interface listed above to end with 'ScreenState'.",
            )
        }
    }

    @Test
    fun `every sealed ScreenState interface has at least one concrete subtype`() {
        val sealedInterfaces = packages.flatMap { pkg ->
            discoverSealedInterfaces(pkg)
        }

        assertTrue(
            sealedInterfaces.isNotEmpty(),
            "Expected to find sealed interfaces in packages $packages, but found none. " +
                "Ensure the test is running against compiled sources.",
        )

        val violations = sealedInterfaces.filter { klass ->
            klass.sealedSubclasses.isEmpty()
        }

        if (violations.isNotEmpty()) {
            fail(
                "Sealed interfaces with no concrete subtypes:\n" +
                    violations.joinToString("\n") { "  - ${it.qualifiedName}" } +
                    "\n\nAdd at least one concrete subtype to each sealed interface listed above.",
            )
        }
    }

    /**
     * Scans the JVM classpath for compiled sealed interfaces in [packageName]
     * (recursively). Inner/anonymous/companion classes (names containing '$')
     * are excluded.
     */
    private fun discoverSealedInterfaces(packageName: String): List<KClass<*>> {
        val classLoader = Thread.currentThread().contextClassLoader
        val packagePath = packageName.replace('.', '/')
        val result = mutableListOf<KClass<*>>()

        val resources = classLoader.getResources(packagePath)
        while (resources.hasMoreElements()) {
            val dir = File(resources.nextElement().toURI())
            if (!dir.isDirectory) continue

            dir
                .walkTopDown()
                .filter { it.isFile && it.name.endsWith(".class") && '$' !in it.name }
                .mapNotNull { classFile ->
                    val relative = classFile
                        .relativeTo(dir)
                        .path
                        .removeSuffix(".class")
                        .replace(File.separatorChar, '.')
                    runCatching {
                        Class.forName("$packageName.$relative", false, classLoader).kotlin
                    }.getOrNull()
                }.filter { it.isSealed }
                .forEach { result.add(it) }
        }

        return result
    }
}
