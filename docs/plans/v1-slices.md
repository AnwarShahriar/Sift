# Sift v1 — Slices

Companion to [sift-spec.md](sift-spec.md). Each slice is one Claude Code session: small enough to finish, review and commit on its own, and it leaves the app working. Slices run in order inside a milestone; milestones run in order.

Design canvas "Sift — UI": https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q. v1 builds the **Playful (Dark)** page. Screen links below point at its artboards.

- PR list: [PlayPRList](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q#artboard-PlayPRList)
- Review: [PlayReview](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q#artboard-PlayReview)
- Follow-ups: [PlayFollowUps](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q#artboard-PlayFollowUps)
- Settings: [PlaySettings](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q#artboard-PlaySettings)
- Hard-gate sign-off: [PlaySignoffDesktop](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q#artboard-PlaySignoffDesktop)
- Red queue and submit bar: [PlayRedQueue](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q#artboard-PlayRedQueue)
- Side panel with Passed open: [PlayPassed](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q#artboard-PlayPassed)
- Empty, loading, error and first-run states: [PlayStates](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q#artboard-PlayStates)

## Stack

Kotlin end to end. Kotlin Multiplatform for the shared models, Compose Multiplatform for the UI, desktop target only in v1. Mobile and the Mosaic terminal UI reuse the same models later.

## v1 passes

| Pass | Kind | Slice |
| --- | --- | --- |
| Build status | code (log parser) + AI one-liner | S3.2, S4.3 |
| Target branch | code only (no Jira) | S3.3 |
| Impact map | code only (sensitive paths from conventions) | S3.3 |
| Collapse non-reviewables | code only | S3.3 |
| Security (secrets) | code only (gitleaks), hard gate | S3.4 |
| CI/CD and tooling changed | code only, hard gate | S3.4 |
| Naming and style | AI, against the conventions file | S4.3 |
| Tests | AI ("what this proves", trivial / over-mocked) | S4.3 |
| Components (near-duplicate) | AI, Claude searches the repo | S4.3 |

## Rules every slice follows

- Sift never holds a credential. `gh` and Claude Code own their sign-ins.
- The UI never decides. It renders review state and writes intents to the decisions file.
- Code computes what is computable; AI only judges and summarises. Every AI finding carries evidence that maps to real diff lines, or it is dropped.
- Nothing spends AI unless the reviewer asks (Prepare / Re-prepare).
- Colours come from theme tokens, never literals, so the light theme drops in later.

---

## M0 — Contract and fixtures

No app code. Ends with a schema both fixtures validate against.

### S0.1 Repo skeleton and CLAUDE.md
- Kotlin Multiplatform Gradle project: `core-model` (KMP, shared models), `engine` (JVM: `gh`, queue, worker, sync), `app-desktop` (Compose Multiplatform, desktop target). Empty modules that build.
- `CLAUDE.md` with the rules above and pointers to the spec and this file.
- **Done when:** `./gradlew build` passes on an empty project.

### S0.2 `review.json` schema
- `schema/review.schema.json` covering:
  - **meta:** schema version, PR number, repo, head SHA, base and target branch check, prepared at, worker, conventions version, budget used.
  - **status:** build (passing / failing / pending + step, file, likely cause, code vs infra), review state, awaiting-build passes.
  - **passes[]:** id, group, ran or skipped (why), score 1–10 or hard gate, one-line reason, collapsed, spot check.
  - **cards[]:** id, pass id, title, explanation, score, hard gate, evidence[] (file, lines, side), footnotes[], needs code opened, order.
  - **followUps[]:** id, type, original comment (text, file, lines, commit, thread), signals, verdict, why, evidence.
  - **delta:** previous review SHA, files changed since.
- **Done when:** the schema is reviewed and checked in; a validation script (`scripts/validate`) runs against any bundle.

### S0.3 Decisions file schema
- `schema/decisions.schema.json`: card verdicts, comments (anchor, text, mentions), hard-gate sign-offs (name, reason), follow-up resolutions, submit choice, tracked follow-ups.
- **Done when:** schema checked in and validated by the same script.

### S0.4 Fixtures from the design
- `fixtures/pr-482/review.json`: failing iOS build, PromoCard near-duplicate (7), banner grows on tablet (6), 2 awaiting build, 9 passed, a hard gate.
- `fixtures/pr-476/review.json`: 4 follow-ups (handled, answered, partly, still open) plus 2 delta cards.
- PR list fixture with #482, #476, #470, #487, #489, #491 in their design states.
- **Done when:** both bundles validate.

### S0.5 Conventions file
- `conventions/sift.conventions.md`: Shahriar's style rules, sensitive paths, rubrics and threshold 5 for the v1 passes.
- **Done when:** every v1 pass has a rubric with concrete anchors.

---

## M1 — Desktop UI from fixtures

The whole review flow clickable with no network and no AI.

### S1.1 Models and fixture loading
- Kotlin models in `core-model` mirroring both schemas (kotlinx.serialization); load fixtures from disk.
- **Done when:** both fixtures round-trip through the models in a unit test.

### S1.2 Theme tokens and base components
- Dark token set (ground #0E0F16, card #181A26, raised #222536, lime, coral, violet, amber, off-white), Bricolage Grotesque + JetBrains Mono, radii, pill buttons, status tag, group heading pill, score tile, code snippet block.
- **Done when:** a component preview window shows each piece.

### S1.3 PR list screen
- Header with review fuel gauge and auto-prep state; headline; groups Needs you / Cooking / Not prepared; PR tile with title, status tag, details line, attention line, single action.
- **Done when:** the list fixture renders like PlayPRList and each tile's action navigates (Open, Resume, Re-prepare, Prepare, Cancel as stubs).

### S1.4 Review screen: deck
- Top bar (back, PR title, build summary, card progress dots), upright card (score tile, pass, title, explanation, side-by-side snippet, footnote link), Dig in / Undo / Looks fine buttons.
- **Done when:** #482's cards can be swiped through with buttons; order is hard gates first then score.

### S1.5 Keyboard flow and guardrails
- ← → Space Z C R F Esc. Cards scored 7+ need the code opened before → counts ("✓ code peeked").
- **Done when:** the whole deck works from the keyboard and the 7+ rule blocks an unpeeked right swipe.

### S1.6 Side panel
- Reds pile, "Waiting on the build" with Refresh disabled until green, next hard gate hint, and the collapsed "Passed · 9" list with each pass's one-line reason (PlayPassed).
- **Done when:** the panel updates live as cards are swiped.

### S1.7 Hard-gate sign-off (desktop)
- PlaySignoffDesktop: what the gate is, data-flow or change summary, "Why it's OK" field, Sign off as the reviewer's name, Send back to the author. Keep it visually distinct from primary actions.
- **Done when:** a right swipe on a hard gate opens sign-off and records name and reason in the decisions file.

### S1.8 Red queue and submit bar
- PlayRedQueue: red cards only, comment box anchored under the snippet with @mentions, submit bar (Approve / Request changes / Pending), Approve disabled while the build is red.
- **Done when:** comments and the submit choice land in the decisions file and validate.

### S1.9 Follow-ups screen
- PlayFollowUps: "You asked for N things", status-dot list, follow-up card (your comment, author reply, diff since comment, verdict, why), Not yet · reply / Sorted · resolve; then delta cards.
- **Done when:** #476 runs follow-ups first, then its 2 delta cards, and resolutions land in the decisions file.

### S1.10 Settings screen
- PlaySettings: worker choice (Claude Code; Gemma shown disabled), review fuel per day, auto-prep toggle with disclaimer that must be ticked, filters, auto cap, refresh-on-green flag. Per-machine config file, no secrets.
- **Done when:** settings persist to `~/.config/sift/config.toml` and auto-prep can't turn on without the disclaimer.

### S1.11 Empty, loading and error states
- PlayStates: no PRs, loading, `gh` not signed in, Claude Code not found or not signed in, worker failed, fuel used up. Playful voice, plain wording for errors.
- **Done when:** each state is reachable from a fixture or flag.

---

## M2 — Live PR list via `gh`

Opening the app costs no AI.

### S2.1 First-run checks
- Check `gh auth status` and `claude` on PATH; explain what is missing; never ask for a key.
- **Done when:** the app shows the right S1.11 state for each missing tool.

### S2.2 Fetch open PRs
- `engine` lists PRs via `gh` for the tabs Review requested (default), All open, Mine; search by title, number, repo.
- **Done when:** the pilot repo's real PRs appear in the list.

### S2.3 Quick facts
- Repo, author, files, +/−, CI status, target branch, "touches build files" from `gh` and scripts.
- **Done when:** every tile shows its details line from live data.

### S2.4 Review states from disk
- Derive Not prepared, Ready, In review, Stale ("N new commits"), Author replied from `.sift/<pr>/` bundles, decisions and `gh` thread data.
- **Done when:** dropping a fixture bundle into `.sift/` moves that PR to Ready; a new commit makes it Stale.

---

## M3 — Skill, deterministic part

Prepare produces a valid bundle with no AI involved.

### S3.1 Skill scaffold
- `skill/sift-review/SKILL.md` and a fact-gathering entry script: `gh pr view`, `gh pr diff`, `gh pr checks`, review threads, conventions file → a facts JSON.
- **Done when:** running the script on a real PR writes facts with no AI call.

### S3.2 Build status pass (code half)
- Parse the failed build log for the first real error: step, file, message. The AI one-liner comes in M4.
- **Done when:** the #482-style Gradle failure is extracted from a sample log.

### S3.3 Target branch, impact map, collapse non-reviewables
- Three code-only passes writing passes[] entries with scores and one-line reasons.
- **Done when:** each runs against a real PR and its output validates.

### S3.4 Hard gates: secrets and CI/CD
- gitleaks on the diff (redact any hit in snippets); CI/workflow/build-file change detection.
- **Done when:** a planted fake secret raises the Security gate with a redacted snippet.

### S3.5 Bundle writer
- Assemble `.sift/<pr>/review.json`, apply threshold and spot checks (1 in 5), validate before writing.
- **Done when:** Prepare on a real PR produces a valid bundle the UI opens.

---

## M4 — Skill, AI part and the queue

### S4.1 Worker launcher
- Run Claude Code headless with the skill, stripped environment (`ANTHROPIC_API_KEY` removed), progress events streamed back.
- **Done when:** Prepare runs the skill and the tile shows live "Sifting passes… 6 / 11".

### S4.2 Queue, Cancel, review fuel
- One job at a time; Cancel kills the worker and keeps nothing half-written; daily budget counted and shown before clicking.
- **Done when:** queuing two PRs runs them in order, Cancel leaves no bundle, an empty fuel tank blocks Prepare.

### S4.3 AI passes
- Naming and style (against conventions), Tests ("what this proves"), Components (near-duplicate, Claude searches the repo), build failure one-liner. Findings without valid evidence are dropped.
- **Done when:** a real PR gets AI cards that fill in as passes finish, each with evidence on real diff lines.

### S4.4 Awaiting build and Refresh
- Passes needing build output show as awaiting; Refresh appears once CI is green.
- **Done when:** a red-CI PR shows awaiting passes and Refresh works after CI turns green.

---

## M5 — Sync back to GitHub

### S5.1 `sift sync`: comments
- Post decisions-file comments as review comments anchored to file and line via `gh`, as the reviewer.
- **Done when:** a test PR shows the comments in GitHub's diff view.

### S5.2 `sift sync`: submit and resolve
- Submit approve / request changes / pending; resolve threads marked sorted; record hard-gate sign-offs in the review body.
- **Done when:** a full review round-trips to GitHub on a test PR.

---

## M6 — Follow-ups

### S6.1 Follow-up tracking
- Save each comment as a follow-up with its type guessed from wording (flippable), anchor and thread.
- **Done when:** comments from S5.1 appear as tracked follow-ups.

### S6.2 Free signals
- Replies, resolved, outdated, lines touched by new commits; drives Author replied in the PR list.
- **Done when:** an author reply on a test PR flips the tile to "They replied".

### S6.3 Verdicts on Re-prepare
- AI verdict per follow-up with evidence (new diff lines or reply text); never closes one by itself.
- **Done when:** Re-prepare on #476-style PR yields the four statuses with evidence.

### S6.4 Delta review
- After follow-ups, cards cover only what changed since the last review SHA.
- **Done when:** a re-review shows follow-ups first, then only delta cards.

---

## Gaps to settle while building

- **Compliance hard gate in the design, not in v1 passes.** PlaySignoff and the review panel show a Braze/PII compliance gate, but the v1 pass list has only Security and CI/CD as hard gates. Either add Compliance to v1 or use a Security or CI/CD gate as the sign-off example.
- **Mobile screens** (PlayMobile, PlaySignoff) are out of v1.
