package com.chriscartland.batterybutler.viewmodel

import java.io.File
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Convention test: every class whose name ends with "ViewModel" must have a
 * corresponding test class named "*ViewModelTest" in the test source set.
 *
 * This ensures new ViewModels don't slip in without test coverage.
 *
 * Excludes:
 * - Factory classes (*Factory)
 * - Infrastructure classes (KmpViewModelStore)
 * - Inner/anonymous classes (names containing '$')
 */
class ViewModelTestConventionTest {
    private val packages = listOf(
        "com.chriscartland.batterybutler.viewmodel",
        "com.chriscartland.batterybutler.experimental.viewmodel",
    )

    @Test
    fun `every ViewModel class has a corresponding test class`() {
        val viewModelClasses = packages.flatMap { pkg ->
            discoverClasses(pkg, "ViewModel")
                .filterNot { klass ->
                    val name = klass.simpleName ?: ""
                    name.endsWith("Factory") ||
                        name == "KmpViewModelStore" ||
                        name == "ViewModelTestConventionTest"
                }
        }

        assertTrue(
            viewModelClasses.isNotEmpty(),
            "Expected to find ViewModel classes in packages $packages, but found none. " +
                "Ensure the test is running against compiled sources.",
        )

        val testClassNames = packages
            .flatMap { pkg ->
                discoverClasses(pkg, "ViewModelTest")
            }.map { it.simpleName ?: "" }
            .toSet()

        val violations = viewModelClasses.filterNot { klass ->
            val expectedTestName = "${klass.simpleName}Test"
            testClassNames.contains(expectedTestName)
        }

        if (violations.isNotEmpty()) {
            fail(
                "ViewModel classes missing a corresponding '*ViewModelTest' class:\n" +
                    violations.joinToString("\n") { "  - ${it.qualifiedName}" } +
                    "\n\nCreate a test file for each ViewModel listed above.",
            )
        }
    }

    /**
     * Scans the JVM classpath for compiled classes in [packageName] (recursively)
     * whose simple name ends with [suffix].
     * Inner/anonymous/companion classes (names containing '$') are excluded.
     */
    private fun discoverClasses(
        packageName: String,
        suffix: String,
    ): List<KClass<*>> {
        val classLoader = Thread.currentThread().contextClassLoader
        val packagePath = packageName.replace('.', '/')
        val result = mutableListOf<KClass<*>>()

        val resources = classLoader.getResources(packagePath)
        while (resources.hasMoreElements()) {
            val dir = File(resources.nextElement().toURI())
            if (!dir.isDirectory) continue

            dir
                .walkTopDown()
                .filter { it.isFile && it.name.endsWith("$suffix.class") && '$' !in it.name }
                .mapNotNull { classFile ->
                    val relative = classFile
                        .relativeTo(dir)
                        .path
                        .removeSuffix(".class")
                        .replace(File.separatorChar, '.')
                    runCatching {
                        Class.forName("$packageName.$relative", false, classLoader).kotlin
                    }.getOrNull()
                }.forEach { result.add(it) }
        }

        return result
    }
}
