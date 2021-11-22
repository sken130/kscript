package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import kscript.app.appdir.UriCache
import kscript.app.model.*
import kscript.app.parser.Parser
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.createDirectories

class ScriptResolverTest {
    private val testHome = Paths.get("build/tmp/script_resolver_test")
    private val config = Config.builder().apply { homeDir = testHome.resolve("home") }.build()
    private val uriCache = UriCache(testHome.resolve("cache").createDirectories())
    private val sectionResolver = SectionResolver(Parser(), uriCache, config)
    private val scriptResolver = ScriptResolver(sectionResolver, uriCache)

    @Test
    fun `Test includes consolidation`() {
        val input = "test/resources/consolidate_includes/template.kts"
        val expected = File("test/resources/consolidate_includes/expected.kts").readText()

        val script = scriptResolver.resolve(input)

        assertThat(script).apply {
            prop(Script::scriptSource).isEqualTo(ScriptSource.FILE)
            prop(Script::scriptType).isEqualTo(ScriptType.KTS)
            prop(Script::sourceUri).transform { it.toString() }
                .endsWith("/test/resources/consolidate_includes/template.kts")
            prop(Script::sourceContextUri).transform { it.toString() }.endsWith("/test/resources/consolidate_includes/")
            prop(Script::scriptName).isEqualTo("template.kts")
            prop(Script::packageName).isEqualTo(null)
            prop(Script::entryPoint).isEqualTo(null)
            prop(Script::importNames).isEqualTo(
                listOf(
                    ImportName("java.io.BufferedReader"),
                    ImportName("java.io.File"),
                    ImportName("java.io.InputStream"),
                    ImportName("java.net.URL"),
                )
            )
            prop(Script::includes).isEqualTo(
                setOf(
                    Include("file1.kts"),
                    Include("file2.kts"),
                    Include("file3.kts"),
                )
            )
            prop(Script::dependencies).isEqualTo(
                setOf(
                    Dependency("com.eclipsesource.minimal-json:minimal-json:0.9.4"), Dependency("log4j:log4j:1.2.14")
                )
            )
            prop(Script::repositories).isEmpty()
            prop(Script::kotlinOpts).isEmpty()
            prop(Script::compilerOpts).isEmpty()

            prop(Script::resolvedCode).isEqualTo(expected)
        }
    }


    @Test
    fun `Test includes annotations`() {
        val input = "test/resources/includes/include_variations.kts"
        val expected = File("test/resources/includes/expected_variations.kts").readText()

        val script = scriptResolver.resolve(input)

        assertThat(script).apply {
            prop(Script::scriptSource).isEqualTo(ScriptSource.FILE)
            prop(Script::scriptType).isEqualTo(ScriptType.KTS)
            prop(Script::sourceUri).transform { it.toString() }
                .endsWith("/test/resources/includes/include_variations.kts")
            prop(Script::sourceContextUri).transform { it.toString() }.endsWith("/test/resources/includes/")
            prop(Script::scriptName).isEqualTo("include_variations.kts")
            prop(Script::packageName).isEqualTo(null)
            prop(Script::entryPoint).isEqualTo(null)
            prop(Script::importNames).isEqualTo(emptyList())
            prop(Script::includes).isEqualTo(
                setOf(
                    Include("rel_includes/include_1.kt"),
                    Include("./rel_includes//include_2.kt"),
                    Include("./include_3.kt"),
                    Include("include_4.kt"),
                    Include("include_6.kt"),
                    Include("../include_7.kt"),
                )
            )
            prop(Script::dependencies).isEqualTo(
                setOf(
                    Dependency("com.eclipsesource.minimal-json:minimal-json:0.9.4"), Dependency("log4j:log4j:1.2.14")
                )
            )
            prop(Script::repositories).isEmpty()
            prop(Script::kotlinOpts).isEmpty()
            prop(Script::compilerOpts).isEmpty()

            prop(Script::resolvedCode).isEqualTo(expected)
        }
    }

    @Test
    fun `Test includes detection - should not include dependency twice`() {
        val input = "test/resources/includes/dup_include/dup_include.kts"
        val expected = File("test/resources/includes/dup_include/expected_dup_include.kts").readText()

        val script = scriptResolver.resolve(input)

        assertThat(script).apply {
            prop(Script::scriptSource).isEqualTo(ScriptSource.FILE)
            prop(Script::scriptType).isEqualTo(ScriptType.KTS)
            prop(Script::sourceUri).transform { it.toString() }
                .endsWith("/test/resources/includes/dup_include/dup_include.kts")
            prop(Script::sourceContextUri).transform { it.toString() }.endsWith("/test/resources/includes/dup_include/")
            prop(Script::scriptName).isEqualTo("dup_include.kts")
            prop(Script::packageName).isEqualTo(null)
            prop(Script::entryPoint).isEqualTo(null)
            prop(Script::importNames).isEqualTo(
                setOf(
                    ImportName("java.io.File"),
                    ImportName("java.net.URL"),
                    ImportName("java.io.BufferedReader"),
                    ImportName("java.io.InputStream")
                )
            )
            prop(Script::includes).isEqualTo(
                setOf(
                    Include("file1.kts"),
                    Include("file2.kts"),
                    Include("file3.kts"),
                )
            )
            prop(Script::dependencies).isEqualTo(
                setOf(
                    Dependency("com.eclipsesource.minimal-json:minimal-json:0.9.4"), Dependency("log4j:log4j:1.2.14")
                )
            )
            prop(Script::repositories).isEmpty()
            prop(Script::kotlinOpts).isEmpty()
            prop(Script::compilerOpts).isEmpty()

            prop(Script::resolvedCode).isEqualTo(expected)
        }
    }
}
