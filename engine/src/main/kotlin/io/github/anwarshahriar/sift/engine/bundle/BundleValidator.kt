package io.github.anwarshahriar.sift.engine.bundle

import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SchemaRegistryConfig
import com.networknt.schema.SpecificationVersion
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/** One thing wrong with a bundle file, located by a JSON Pointer such as `/cards/2/passId` ("" is the whole document). */
data class Problem(val path: String, val message: String) {
    override fun toString() = "${path.ifEmpty { "(document)" }}: $message"
}

/** The files a review bundle can hold, each with its schema and cross-field rules. */
enum class BundleFile(val fileName: String, val schemaResource: String) {
    REVIEW("review.json", "/review.schema.json"),
    ;

    companion object {
        fun forFileName(name: String): BundleFile? = entries.firstOrNull { it.fileName == name }
    }
}

/**
 * Checks bundle files: first against their JSON Schema, then, if the shape is right, against the rules
 * a schema can't express (unique ids, references between sections, card order).
 */
class BundleValidator {
    private val mapper = JsonMapper.builder().build()

    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) { builder ->
        builder.schemaRegistryConfig(SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build())
    }

    private val schemas = mutableMapOf<BundleFile, Schema>()

    fun validate(file: BundleFile, json: String): List<Problem> {
        val root = try {
            mapper.readTree(json)
        } catch (e: JacksonException) {
            val where = e.location?.let { " (line ${it.lineNr}, column ${it.columnNr})" }.orEmpty()
            val what = e.originalMessage.substringBefore(" (start marker")
            return listOf(Problem("", "not valid JSON: $what$where"))
        }
        val schemaProblems = schemaFor(file).validate(root).map { error ->
            Problem(error.instanceLocation.toString(), error.message)
        }.distinct()
        if (schemaProblems.isNotEmpty()) return schemaProblems
        return when (file) {
            BundleFile.REVIEW -> ReviewRules(root).check()
        }
    }

    private fun schemaFor(file: BundleFile): Schema = schemas.getOrPut(file) {
        val stream = checkNotNull(javaClass.getResourceAsStream(file.schemaResource)) {
            "schema ${file.schemaResource} missing from the classpath"
        }
        stream.use { registry.getSchema(it, InputFormat.JSON) }
    }
}

internal fun JsonNode.str(field: String): String? = get(field)?.takeIf { it.isString }?.stringValue()
