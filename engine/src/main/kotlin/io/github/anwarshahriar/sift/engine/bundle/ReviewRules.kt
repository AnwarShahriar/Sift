package io.github.anwarshahriar.sift.engine.bundle

import tools.jackson.databind.JsonNode

/**
 * Rules for review.json that JSON Schema can't express. Runs only on a document that already
 * matches the schema, so every required field is present with the right type.
 *
 * Not checked here: that evidence maps to real diff lines. That needs the diff, so the bundle
 * writer checks it before writing.
 */
internal class ReviewRules(private val root: JsonNode) {
    private val problems = mutableListOf<Problem>()

    private val passes = root["passes"].toList()
    private val cards = root["cards"].toList()
    private val followUps = root["followUps"].toList()
    private val delta = root["delta"].takeUnless { it.isNull }

    fun check(): List<Problem> {
        uniqueIds("passes", passes)
        uniqueIds("cards", cards)
        uniqueIds("followUps", followUps)
        passRules()
        cardRules()
        deckOrder()
        followUpRules()
        deltaRules()
        return problems
    }

    private fun problem(path: String, message: String) {
        problems += Problem(path, message)
    }

    private fun uniqueIds(section: String, items: List<JsonNode>) {
        val seen = mutableSetOf<String>()
        items.forEachIndexed { i, item ->
            val id = item.str("id")!!
            if (!seen.add(id)) problem("/$section/$i/id", "duplicate id \"$id\"")
        }
    }

    private fun passRules() {
        val buildPassing = root["status"]["build"].str("state") == "passing"
        val spotChecks = passes.withIndex().filter { (_, p) -> p["spotCheck"].booleanValue() }
        if (spotChecks.size > 1) {
            problem("/passes/${spotChecks[1].index}/spotCheck", "at most one spot check per review")
        }
        passes.forEachIndexed { i, pass ->
            val id = pass.str("id")!!
            val state = pass.str("state")
            if (state == "awaiting-build" && buildPassing) {
                problem("/passes/$i/state", "awaiting-build but the build is passing")
            }
            val shown = state == "ran" && (!pass["collapsed"].booleanValue() || pass["spotCheck"].booleanValue())
            if (shown && cards.none { it.str("passId") == id }) {
                problem("/passes/$i", "pass \"$id\" is shown (not collapsed) but has no cards")
            }
        }
    }

    private fun cardRules() {
        val passesById = passes.associateBy { it.str("id")!! }
        val deltaFiles: Set<String>? = delta?.get("filesChanged")?.toList()?.map { node: JsonNode -> node.stringValue() }?.toSet()
        cards.forEachIndexed { i, card ->
            val at = "/cards/$i"
            val passId = card.str("passId")!!
            val pass = passesById[passId]
            if (pass == null) {
                problem("$at/passId", "no pass with id \"$passId\"")
            } else {
                when {
                    pass.str("state") != "ran" ->
                        problem("$at/passId", "pass \"$passId\" did not run (${pass.str("state")})")
                    pass["collapsed"].booleanValue() && !pass["spotCheck"].booleanValue() ->
                        problem("$at/passId", "pass \"$passId\" is collapsed, so it has no cards unless it is the spot check")
                }
                if (card["hardGate"].booleanValue() != pass["hardGate"].booleanValue()) {
                    problem("$at/hardGate", "must match pass \"$passId\" hardGate")
                }
            }
            card["evidence"].forEachIndexed { j, ev ->
                lineRange("$at/evidence/$j", ev)
                val file = ev.str("file")!!
                if (deltaFiles != null && ev.str("side") == "new" && file !in deltaFiles) {
                    problem("$at/evidence/$j/file", "re-review cards cover only delta.filesChanged; \"$file\" is not in it")
                }
            }
            card["context"].forEachIndexed { j, ctx -> lineRange("$at/context/$j", ctx) }
        }
    }

    /** Orders are 0..n-1 with no gaps, hard gates first, then score from high to low. */
    private fun deckOrder() {
        val orders = cards.map { it["order"].intValue() }
        if (orders.sorted() != cards.indices.toList()) {
            problem("/cards", "card orders must be 0..${cards.size - 1}, each used once; got ${orders.sorted()}")
            return
        }
        val deck = cards.withIndex().sortedBy { it.value["order"].intValue() }
        deck.zipWithNext().forEach { (a, b) ->
            val aGate = a.value["hardGate"].booleanValue()
            val bGate = b.value["hardGate"].booleanValue()
            val outOfOrder = when {
                aGate -> false
                bGate -> true
                else -> b.value["score"].intValue() > a.value["score"].intValue()
            }
            if (outOfOrder) {
                problem(
                    "/cards/${b.index}/order",
                    "card \"${b.value.str("id")}\" must come before \"${a.value.str("id")}\": hard gates first, then highest score",
                )
            }
        }
    }

    private fun followUpRules() {
        val prAuthor = root["meta"]["pr"].str("author")!!
        followUps.forEachIndexed { i, fu ->
            val at = "/followUps/$i"
            val comment = fu["comment"]
            if (!comment["file"].isNull) lineRange("$at/comment", comment)
            val replies = fu["replies"].toList()
            val authorReplied = replies.any { it.str("author") == prAuthor }
            if (fu["signals"]["authorReplied"].booleanValue() != authorReplied) {
                problem("$at/signals/authorReplied", "must be $authorReplied: the replies ${if (authorReplied) "include" else "don't include"} one from $prAuthor")
            }
            val replyIds = replies.map { it.str("id") }.toSet()
            fu["evidence"].forEachIndexed { j, ev ->
                if (ev.has("replyId")) {
                    if (ev.str("replyId") !in replyIds) problem("$at/evidence/$j/replyId", "no reply with that id in this follow-up")
                } else {
                    lineRange("$at/evidence/$j", ev)
                }
            }
        }
        if (followUps.isNotEmpty() && delta == null) {
            problem("/delta", "follow-ups only exist on a re-review, so delta must be set")
        }
    }

    private fun deltaRules() {
        if (delta == null) return
        if (delta.str("previousSha") == root["meta"].str("headSha")) {
            problem("/delta/previousSha", "same as meta.headSha; nothing changed since the previous review")
        }
    }

    private fun lineRange(at: String, node: JsonNode) {
        if (node["endLine"].intValue() < node["startLine"].intValue()) {
            problem("$at/endLine", "endLine is before startLine")
        }
    }
}
