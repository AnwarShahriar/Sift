package io.github.anwarshahriar.sift.engine.bundle

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ReviewValidationTest {
    private val validator = BundleValidator()
    private val mapper = JsonMapper.builder().build()

    private fun sample(): ObjectNode =
        mapper.readTree(javaClass.getResource("/bundle/review-valid.json")!!.readText()) as ObjectNode

    private fun problems(edit: ObjectNode.() -> Unit = {}): List<Problem> =
        validator.validate(BundleFile.REVIEW, mapper.writeValueAsString(sample().apply(edit)))

    private fun assertProblem(path: String, contains: String, edit: ObjectNode.() -> Unit) {
        val found = problems(edit)
        if (found.none { it.path == path && it.message.contains(contains) }) {
            fail("expected a problem at $path containing \"$contains\", got:\n${found.joinToString("\n")}")
        }
    }

    private fun ObjectNode.obj(vararg path: Any): ObjectNode = path.fold(this as JsonNode) { node, key ->
        when (key) {
            is Int -> node[key]
            else -> node[key as String]
        }
    } as ObjectNode

    private fun ObjectNode.arr(name: String): ArrayNode = this[name] as ArrayNode

    @Test
    fun `sample bundle is valid`() {
        assertEquals(emptyList(), problems())
    }

    @Test
    fun `first review with no follow-ups and a passing build is valid`() {
        val found = problems {
            putNull("delta")
            putArray("followUps")
            obj("status", "build").put("state", "passing").putNull("failure")
            obj("status").put("review", "ready")
            arr("passes").removeIf { it["state"].stringValue() == "awaiting-build" }
            arr("cards").forEach { card ->
                (card["evidence"] as ArrayNode).forEach { (it as ObjectNode).put("side", "new") }
            }
        }
        assertEquals(emptyList(), found)
    }

    @Test
    fun `malformed JSON is reported, not thrown`() {
        val found = validator.validate(BundleFile.REVIEW, "{ not json")
        assertEquals("", found.single().path)
    }

    // Schema rules

    @Test
    fun `unknown fields are rejected`() {
        assertTrue(problems { put("extra", 1) }.isNotEmpty())
    }

    @Test
    fun `score must be 1 to 10`() {
        assertTrue(problems { obj("passes", 1).put("score", 11) }.isNotEmpty())
    }

    @Test
    fun `failing build needs a failure summary`() {
        assertTrue(problems { obj("status", "build").putNull("failure") }.isNotEmpty())
    }

    @Test
    fun `review cannot be ready while the build is red`() {
        assertTrue(problems { obj("status").put("review", "ready") }.isNotEmpty())
    }

    @Test
    fun `ai card needs evidence`() {
        assertTrue(problems { obj("cards", 1).putArray("evidence") }.isNotEmpty())
    }

    @Test
    fun `cards scored 7 or more need the code opened`() {
        assertTrue(problems { obj("cards", 1).put("needsCodeOpened", false) }.isNotEmpty())
    }

    @Test
    fun `hard gate card has no score`() {
        assertTrue(problems { obj("cards", 0).put("score", 5) }.isNotEmpty())
    }

    @Test
    fun `skipped pass has no score`() {
        assertTrue(problems { obj("passes", 5).put("score", 3) }.isNotEmpty())
    }

    @Test
    fun `a question cannot be addressed`() {
        assertTrue(problems { obj("followUps", 1).put("verdict", "addressed") }.isNotEmpty())
    }

    @Test
    fun `a verdict other than open needs evidence`() {
        assertTrue(problems { obj("followUps", 0).putArray("evidence") }.isNotEmpty())
    }

    @Test
    fun `reason is one line`() {
        assertTrue(problems { obj("passes", 0).put("reason", "two\nlines") }.isNotEmpty())
    }

    @Test
    fun `sha must be full length`() {
        assertTrue(problems { obj("meta").put("headSha", "2222") }.isNotEmpty())
    }

    @Test
    fun `dates are checked`() {
        assertTrue(problems { obj("meta").put("preparedAt", "yesterday") }.isNotEmpty())
    }

    // Cross-field rules

    @Test
    fun `duplicate ids`() = assertProblem("/cards/1/id", "duplicate") {
        obj("cards", 1).put("id", "ci-cd-gradle")
    }

    @Test
    fun `card must reference a known pass`() = assertProblem("/cards/1/passId", "no pass") {
        obj("cards", 1).put("passId", "nope")
    }

    @Test
    fun `card cannot belong to a collapsed pass`() = assertProblem("/cards/1/passId", "collapsed") {
        obj("cards", 1).put("passId", "security")
    }

    @Test
    fun `shown pass needs a card`() = assertProblem("/passes/1", "no cards") {
        arr("cards").remove(1)
        obj("cards", 1).put("order", 1)
    }

    @Test
    fun `card hard gate matches its pass`() = assertProblem("/cards/1/hardGate", "must match") {
        obj("cards", 1).put("hardGate", true).putNull("score")
    }

    @Test
    fun `one spot check at most`() = assertProblem("/passes/3/spotCheck", "at most one") {
        obj("passes", 3).put("spotCheck", true)
    }

    @Test
    fun `no awaiting passes on a green build`() = assertProblem("/passes/4/state", "awaiting-build") {
        obj("status", "build").put("state", "passing").putNull("failure")
        obj("status").put("review", "ready")
    }

    @Test
    fun `orders have no gaps`() = assertProblem("/cards", "each used once") {
        obj("cards", 2).put("order", 5)
    }

    @Test
    fun `hard gates come first`() = assertProblem("/cards/0/order", "hard gates first") {
        obj("cards", 0).put("order", 1)
        obj("cards", 1).put("order", 0)
    }

    @Test
    fun `higher score comes first`() = assertProblem("/cards/1/order", "highest score") {
        obj("cards", 1).put("order", 2)
        obj("cards", 2).put("order", 1)
    }

    @Test
    fun `line ranges run forward`() = assertProblem("/cards/1/evidence/0/endLine", "before startLine") {
        obj("cards", 1, "evidence", 0).put("endLine", 9)
    }

    @Test
    fun `re-review evidence stays inside the delta`() = assertProblem("/cards/1/evidence/0/file", "filesChanged") {
        obj("cards", 1, "evidence", 0).put("file", "app/src/main/kotlin/Other.kt")
    }

    @Test
    fun `authorReplied agrees with the replies`() = assertProblem("/followUps/1/signals/authorReplied", "must be false") {
        obj("followUps", 1, "signals").put("authorReplied", true)
    }

    @Test
    fun `reply evidence points at a real reply`() = assertProblem("/followUps/0/evidence/1/replyId", "no reply") {
        obj("followUps", 0, "evidence", 1).put("replyId", "PRRC_missing")
    }

    @Test
    fun `follow-ups need a delta`() = assertProblem("/delta", "re-review") {
        putNull("delta")
    }

    @Test
    fun `delta must move the head`() = assertProblem("/delta/previousSha", "same as meta.headSha") {
        obj("delta").put("previousSha", "2222222222222222222222222222222222222222")
    }
}
