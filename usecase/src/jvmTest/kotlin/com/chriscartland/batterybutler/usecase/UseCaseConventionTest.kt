package com.chriscartland.batterybutler.usecase

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Convention test: every class whose name ends with "UseCase" must declare an
 * `operator fun invoke(...)` with any number of parameters and any return type.
 *
 * This enforces the UseCase callable-as-a-function pattern used throughout the app.
 * Both suspend and non-suspend invoke functions satisfy the check.
 */
class UseCaseConventionTest {
    private val packages = listOf(
        "com.chriscartland.batterybutler.usecase",
        "com.chriscartland.batterybutler.experimental.usecase",
    )

    @Test
    fun `every UseCase class has an operator invoke function`() {
        val useCaseClasses = packages.flatMap { discoverUseCaseClasses(it) }

        assertTrue(
            useCaseClasses.isNotEmpty(),
            "Expected to find UseCase classes in packages $packages, but found none. " +
                "Ensure the test is running against compiled sources.",
        )

        val violations = useCaseClasses.filterNot { kClass ->
            kClass.memberFunctions.any { fn -> fn.name == "invoke" && fn.isOperator }
        }

        if (violations.isNotEmpty()) {
            fail(
                "UseCase classes missing an 'operator fun invoke(...)' function:\n" +
                    violations.joinToString("\n") { "  - ${it.qualifiedName}" },
            )
        }
    }

    /**
     * Scans the JVM classpath for compiled classes in [packageName] whose simple name ends with
     * "UseCase". Inner/anonymous/companion classes (names containing '$') are excluded.
     */
    private fun discoverUseCaseClasses(packageName: String): List<KClass<*>> {
        val classLoader = Thread.currentThread().contextClassLoader
        val packagePath = packageName.replace('.', '/')
        val result = mutableListOf<KClass<*>>()

        val resources = classLoader.getResources(packagePath)
        while (resources.hasMoreElements()) {
            val dir = File(resources.nextElement().toURI())
            if (!dir.isDirectory) continue

            dir
                .walkTopDown()
                .filter { it.isFile && it.name.endsWith("UseCase.class") && '$' !in it.name }
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
