# Sift — Human-First Code Tool Spec

Formerly "Swipe Review". Nov 18, 2026 · updated Nov 24, 2026 · @Md Shahriar Anwar

Build plan for v1: see "Sift v1 — Build Plan and Claude Code Handoff" in this project. Design canvas ("Sift — UI"): https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q

## Problem and goal

Sift turns a 2,000-line AI-generated PR into a short deck of cards, so a human reviews what matters and skips what doesn't.

AI now writes most of the code. PRs get large and verbose, and line-by-line GitHub diffs cause review fatigue. Reviewers still own quality, so the tool must keep their judgment central while doing the sorting for them.

**Design principles**

- Succinct: one short status note on top; detail only on demand.
- Fun: cards, swipes and light microcopy instead of a wall of diff.
- Reviewer-led: the flow mirrors how an experienced client developer actually reviews.
- Quiet by default: anything CI, linters or tooling already enforce is never repeated.
- Accountable: risky areas need an explicit, recorded human sign-off.
- Holds no secrets: Sift never stores or handles a credential (see Auth and secrets).
- Spends only when asked: AI work starts only when the reviewer asks for it, unless they opt in to automatic preparation.
- Content first: playful look, but nothing decorative competes with the code and the finding (see Visual language).

## PR list and preparing reviews

The app opens on a list of open PRs. Filling the list uses plain `gh` calls with no AI, so opening the app costs nothing.

**Tabs:** Review requested (default), All open, Mine. Search by title, number or repo.

**Each PR shows quick facts for free** (from GitHub and scripts, no AI): repo, author, files and size, CI status, target branch check, notable touches such as build files. Enough to decide whether a PR is worth preparing.

**Review states:**

| State | What you see | Actions |
| --- | --- | --- |
| Not prepared | title, details, quick facts | **Prepare review** |
| Queued | position in queue | Cancel |
| Preparing | live progress (facts gathered, passes done, e.g. 6 of 11) | Cancel |
| Ready | card count, "needs you" count, build status | **Open** |
| In review | progress through the deck and reds, open hard gates | Resume |
| Stale | "N new commits since your review" | **Re-prepare** (changed parts only) |
| Author replied | a Stale PR with replies to your comments, plus the follow-up counts | **Re-prepare** |

**Layout (decided): grouped by what the PR needs from you.**

- **Needs you:** Ready, Author replied, In review. Accent edge and a filled primary action.
- **Cooking:** Preparing and Queued. Quieter, with a plain Cancel.
- **Not prepared:** with the free quick facts and a Prepare action.
- Group headings are solid colour pills (larger, uppercase, with a count) followed by a rule, so they never read as PR titles.
- Each PR reads in three lines: title and one status tag; a details line (repo · author · files · +/− · CI ✓/✕); and, only when it matters, an attention line (build failing, follow-up dots, card progress, "touches build files"). Colour only on what needs attention.
- One action per PR, filled only where the reviewer should act; Prepare and Re-prepare say they use one unit of the daily budget.

**Rules:**

- **Manual by default.** A review is prepared only when the reviewer clicks Prepare review. No background preparation, no watchers.
- **No automatic re-run when CI goes green.** A review with "awaiting build" passes shows a **Refresh** action once CI is green.
- **One at a time.** Preparation jobs run through a single queue, so marking several PRs never fires them in parallel.
- **Budget visible before clicking.** The header shows today's count against the daily budget ("review fuel").
- **Cancel stops the worker cleanly**; nothing half-written is kept.

### Setting: automatic preparation (opt-in, off by default)

- **When on:** new PRs matching the filters are prepared in the background through the same queue and worker as manual ones.
- **Filters:** repositories (pick which), which PRs (only where requested as reviewer, the default, or all open PRs), skip drafts (default on).
- **Own daily cap** for automatic runs, separate from manual (default 5). When reached, auto stops, manual still works, and the list shows "Auto-prep paused: daily limit".
- **Refresh when CI goes green:** a separate flag, off by default.
- **Disclaimer must be accepted to turn it on,** and is shown again whenever the automatic cap is raised. Settings are per machine.
- **Always visible:** auto-prepared PRs carry an "Auto" tag; the PR list header shows the auto-prep state.

Disclaimer text:

> **Heads up: this spends AI in the background.**
> Every prepared review sends that PR's code to the AI worker and uses your plan, even for PRs you never open.
> - Personal plan: keep it to your own reviews. Claude plans assume ordinary, individual use, and heavy automated use may not be covered.
> - Work account: check your organisation allows automated use first.
> - Auto runs get their own daily limit.
>
> ☐ Got it, turn on auto-prep

When the worker is local Gemma, the disclaimer drops the usage and plan warnings and notes instead that it uses the GPU and may slow the machine while it runs.

## Core experience

A first review is three steps: read the status note, swipe the deck, then work through your reds. A re-review starts with your follow-ups.

### 1. Status note

One glance at the top of the PR:

```
PR #482 · PROJ-123 → epic/onboarding-v2 ✓ · v4.12
⚠ Build failing – Gradle :shared:compileKotlinIosArm64
  Unresolved ref `PromoState.Expired` (likely missing expect/actual)
Review: PENDING (approval blocked until green)

Needs you (2)
  7  Components – new PromoCard ~80% like PromoBanner
  6  Visual – banner grows on tablet

Awaiting build (2): Visual proof · Tests
Passed (9) ▸ collapsed
```

- A failing build never stops the review. The tool summarises why it fails, runs every other pass, and marks the review **pending** until the build is green.
- Passes that need build output (visual proof, tests, performance) show as *awaiting build*; once CI is green the reviewer refreshes them with one action (or automatically, if that setting is on).
- On a re-review the note adds: "Your follow-ups: 2 addressed · 1 answered · 1 open".

### 2. Swipe deck

- **One card per finding**, grouped under its pass (decided for v1). Passes with no related change never appear.
- Each card says, in a line or two and a light tone, what it found and why it matters, plus its concern score.
- **Swipe right** = fine, marked green. **Swipe left** = wants a deeper look, marked red.
- **Tap "show me the code"** to open the exact snippet the card refers to, not the whole file.
- **Footnotes** link short docs (platform API, guideline, conventions rule) when useful. Never long.
- Order: hard gates first, then highest score down.
- Undo is always one tap away.
- On desktop and terminal, keys replace swipes: ← dig in, → looks fine, Space show code, Z undo, C comment, R reds, F refresh awaiting passes, Esc back to the PR list.
- **Spot check cards** are labelled as such: a normally hidden low-score pass shown at random (see Scoring model).

### 3. Red queue

- After the deck, revisit only the red cards.
- Comment like on a GitHub diff, anchored to the snippet, with @mentions to tag people.
- Comments sync back to the PR as normal review comments, so authors don't need the tool.
- Submit as approve, request changes, or pending (when the build is red).

### 4. Follow-ups (re-review)

When the reviewer requests changes or asks a question, Sift tracks it until the reviewer says it's done.

- **Saved per comment:** type (*change requested* or *question*, guessed from the wording, flippable), anchor (file, lines, commit), the GitHub thread, and a status: Open, Answered, Addressed, Partly addressed, Not addressed, Closed by you.
- **Checked for free, no AI:** author replies in the thread, thread resolved or not, GitHub's *outdated* flag, whether new commits touched the commented lines. This drives the **Author replied** state in the PR list.
- **AI judgment only on Re-prepare (manual):** does the new code do what was asked, does the reply answer the question; each verdict comes with evidence (new diff lines or the reply text).
- **Follow-up deck first on re-review,** before any new cards. Each card: your original comment, the author's reply, the diff since your comment, Sift's verdict and a short "why". Swipe right = satisfied (resolves the GitHub thread); swipe left = still open (reply in the same step).
- **Sift never closes a follow-up by itself.**
- **Delta review:** after the follow-ups, the regular cards cover only what changed since your last review.

### Guardrails so fun doesn't become rubber-stamping

- Cards scored 7+ need "show me the code" opened before a right swipe counts.
- Hard-gate cards (secrets, compliance, CI/CD, breaking changes) turn a right swipe into a recorded sign-off with the approver's name.
- Light progress touches are fine (progress dots, fuel); speed is never rewarded: no timers, no streak counts.

## Scoring model

Every pass gets a 1–10 concern score; only scores at or above the threshold become cards that ask for attention.

- **Concern, not quality.** 1 = nothing worth a look, 10 = look now.
- **Rubric per pass.** Concrete anchors keep scores consistent, e.g. "2 new booleans on one state = 4; 4+ = 7".
- **Threshold.** Start at 5 globally, then tune per pass (security might surface at 4, naming at 7).
- **One-line reason on every score**, even collapsed ones: "Architecture 2 – matches SettingsScreen pattern."
- **Hard gates ignore scores.** Failed build, wrong target branch, secrets, compliance trigger, breaking proto/API change and CI/CD edits always show and need sign-off.
- **Learn from overrides.** Opening a collapsed pass and finding an issue, or dismissing a flagged one, is recorded to recalibrate rubrics.
- **Spot checks.** About one PR in five shows one random collapsed pass as a card labelled "Spot check", so hidden passes stay honest.

## The review passes

Twenty-one passes, grouped from cheap gates to deep detail; the tool runs only those the PR touches. v1 ships a subset (see the build plan).

| Group | Pass | You check | Tool does | Runs when |
| --- | --- | --- | --- | --- |
| Gates | Build status | Why it fails, what the dev missed | Failure summary (step, file, likely cause; code vs infra); review marked pending | Always |
| Gates | Target branch | Ticket → epic branch → develop/master | Infers expected branch from Jira hierarchy; else states target branch and release version plainly | Always |
| Context | Impact map | File structure vs known risky areas | Changed-file tree with team-marked sensitive areas lit up, change size per folder | Always |
| Context | Collapse non-reviewables | Assets out of the code view | Auto-collapses images, SVGs, fonts, generated files into the visual lane | Assets changed |
| Visual | Asset scalability | SVGs scale across form factors; layered background/foreground; max width | Renders phone, tablet, foldable, landscape, max-width; flags flat SVGs in fixed-height containers; shows SVG layers | UI or assets changed |
| Visual | Visual proof | UI work comes with proof | Requires screenshots; generated by local screenshot tests (Roborazzi/Paparazzi); light/dark, large font, RTL; side by side with Figma; before/after | UI changed |
| Structure | Components | New vs duplicated vs enrich existing | Near-duplicate detection against the component library, with a suggestion to extend | UI code changed |
| Structure | Shared vs platform code | Could platform code move to KMP shared? | Flags platform code with no platform dependencies | Platform code changed |
| Structure | First-party APIs | Official platform API over custom or third-party | Flags custom code and new deps overlapping official APIs; links current docs | New logic or deps |
| Structure | Architecture fit | Team's MVVM and clean architecture; same pattern as similar screens, modules, DB access | Closest-sibling comparison; good deviations marked "new pattern candidate", not errors | Always |
| Structure | Logic placement and data layer | Data behaviour on the data class, not the ViewModel; repository; caching decision | Placement hints; data-layer checklist | Data or VM code changed |
| Structure | Standards | Standard libs (e.g. Navigation 3 in Compose Multiplatform); dimension and font tokens; HIG and Material | Approved-stack check; design token drift report with closest token and a drafted note to the designer | UI or new deps |
| Detail | Naming and style | Names carry the ticket's intent; enums over booleans; single return; visibility; no unneeded state; pure functions | Conventions-file checks surfaced as "worth a look", never blockers | Always |
| Detail | Platform divergence | Per-platform and form-factor behaviour filled in | Platform/form-factor coverage matrix with empty cells highlighted | Shared code changed |
| Detail | Compatibility and proto | Backward compatibility; proto conveys intent, no boolean piles | `buf breaking` and API diffs as a red banner with consumers; proto lint for enum/oneof | API or proto changed |
| Detail | Code-level performance | Is the code itself efficient (incl. Metal shaders) | Flags recomposition/re-render, main-thread work, hot-path allocations, coroutine/Task lifetime | Always |
| Detail | Tests | Real scenarios, proper mocks and fakes, run locally | One-line "what this proves" per test; trivial tests flagged; over-mocking flagged; emulator-needed tag | Tests changed or missing |
| Detail | Deep links, analytics, dependencies | Auth vs unauth deep link params; never override existing events; dependency need, licence, CVEs, newer versions | Deep link matrix; analytics diff vs tracking plan; dependency card (licence, OSV CVEs, versions, size) | Relevant files changed |
| Sign-off | Security | Exploits, env vars, exposed keys | Secrets gate; code-owner approval; public-key allowlist with approver and reason | Always (hard gate) |
| Sign-off | Compliance | GDPR/PII when the change touches user data, analytics or Braze | Auto-trigger from ticket, design or code; data-flow view; named sign-off | Triggered (hard gate) |
| Sign-off | CI/CD and tooling | Workflow and build changes; tool and plugin updates tested; newest OS tested | CI/CD banner with owner approval; tooling update card; OS coverage | CI or tooling changed |

Other checks folded in where relevant: scope creep, loading/empty/error/offline states, strings and localisation, accessibility, feature flags, migrations, logging without PII.

## Team conventions file

One versioned rulebook, read by both the review tool and the coding agents, so every review finding stops the same mistake from being generated again.

- **Architecture:** the team's own interpretation of MVVM and clean architecture, not the textbook versions.
- **Style rules:** enums over boolean flags (code and proto), single return, minimal visibility, no unnecessary stored state, pure self-contained functions where possible.
- **Approved stack:** navigation, serialization, DI, image loading and other standard choices.
- **Design tokens:** dimensions, typography and colour constants.
- **Sensitive areas:** folders and modules the impact map lights up (auth, payments, PII paths).
- **Rubrics and thresholds** for each pass's score.
- **Ownership:** a named owner and changes reviewed like code, so it doesn't drift.

Rules a linter can enforce move to CI; rules that need judgment stay here as "worth a look".

## Clients and tech stack (decided Nov 24)

- **Desktop and mobile apps, built with Compose Multiplatform.** Android, iOS and desktop are all Stable in Compose Multiplatform; the web (Wasm) target is still Beta, so a browser client is not on the critical path.
- **Desktop first.** v1 is the desktop app only.
- **Terminal client (planned).** A terminal UI built with Mosaic (Compose runtime rendering to the terminal) reuses the shared models and view models. It runs as a Ghostty tab or a Herdr plugin pane. Neither Ghostty tabs nor Herdr panes can host a GUI app, so the terminal UI is the only way in. Keyboard-first, and screenshots can render inline through the Kitty graphics protocol, which Ghostty supports.
- **Kotlin end to end.** Shared KMP modules for the models, so the scoring output and what the UI shows can't drift apart.

## UI designs

Design canvas "Sift — UI": https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q, three pages. The earlier neutral "Current" design was retired.

- **Playful (Dark), the v1 theme:** desktop PR list (grouped tiles with details lines, review fuel gauge, auto-prep state), desktop review (upright card, deck peeking behind, score tile, side-by-side code with the near-duplicate, "code peeked", Space/C/R hints, reds pile, "Waiting on the build" with Refresh disabled until green, next hard gate), desktop follow-ups ("You asked for 4 things.", status-dot list, follow-up card, Not yet · reply / Sorted · resolve), desktop settings (worker, review fuel, auto-prep with disclaimer), mobile swipe (Spot check example, lime outline + straight "LOOKS FINE →" label), mobile hard-gate sign-off.
- **Playful (Light):** Shahriar's contrast-fixed light theme, same six screens. White ground, navy text (#0F1B2D), slate secondary text (#51627A), deep green fills for "fine" (#4F8F00), mandarin primary (#FF7A1A), tangerine attention (#FFA43A), frosty tracks (#E6EEF6, #C3D1DF), frosty sign-off backdrop (#DCE9F5); code blocks stay dark.
- **Playful (Terminal):** PR list and review drawn strictly on a 140-column character grid, one font size, whole-cell spacing, colour only as foreground/background on characters (no outlines, rounded corners or larger text). Group headings are coloured blocks with a ─ rule; accent bars use block characters (▌, or ▎ for thinner) that terminals draw edge to edge, so stacked rows join into a solid bar. Outlined tags become `[ FRESH ]`.

Not yet designed in the playful style: desktop red queue and submit bar, the collapsed "Passed" list on desktop, empty/loading/error/first-run states, mobile PR list and red queue, terminal settings. v1 builds the desktop ones from existing parts (see the build plan).

### Visual language (playful)

- **Dark:** near-black ground (#0E0F16), cards #181A26, raised elements #222536. **Light:** see the Playful (Light) page above. Both themes come from one token set.
- **Shapes:** rounded, chunky (cards 28–32 px radius, pill buttons).
- **Type:** Bricolage Grotesque for text and headings (heavy weights for titles), JetBrains Mono for code, file names and PR numbers. Terminal: monospace only.
- **Colour roles (dark):** lime #C8F45A = fine / ready / handled; coral #FF7A66 = dig in / concern / still open; violet #9B8CFF = preparing / answered / spot check; amber #FFC857 = attention / replied / partly / warnings; off-white #F1F2F7 = hard gates and neutral primary actions. Colours always pair with a label or icon.
- **Content first:** cards sit upright at rest; no tilts, rotated stamps or decorative motion. Swipe feedback is an outline plus a small straight label; during a drag the card may follow the finger, but resting states are always straight.
- **Voice:** light, human microcopy ("PromoCard is PromoBanner wearing a hat.", "Build's grumpy"), but findings, risks, errors and sign-offs stay plain and exact.
- **No speed rewards:** progress dots and fuel are fine; no timers or streaks.

## Architecture

### Engine and UI are separate

- **sift-engine** is headless and does all the work: fetch the PR, run passes, score, track follow-ups, hold review state, sync back to GitHub.
- **UIs never decide anything.** They render a review state and send back intents (prepare, swipe, comment, sign-off, resolve follow-up, submit).
- **The contract comes first:** commands in, plus a stream of state updates out (passes finish at different times; "awaiting build" passes can be refreshed when CI goes green).
- **Agents are clients too.** A `sift review 482 --json` CLI or an MCP server lets an agent run Sift on its own PR (only when that is set up deliberately; see Agent pre-review).

### The review bundle is the real contract

A `review.json` file (validated against a schema) holds the cards, scores, reasons, evidence (file and line ranges), hard gates, follow-up verdicts and status. Whoever produces it (a Claude skill now, a Gemma + Lore engine later) is swappable; the desktop app, terminal UI and any HTML prototype only read it. The UI writes the reviewer's decisions (verdicts, comments, sign-offs, follow-up resolutions) to a separate decisions file, which also holds the tracked follow-ups. Field outline: see the build plan.

### v1 route: the app is the front door, a Claude Code skill does the AI work

1. The app lists PRs with plain `gh` calls and shows the free quick facts.
2. The reviewer clicks **Prepare review** (or runs `/sift-review 482` directly in Claude Code). The app queues the job and starts Claude Code headless with the skill, streaming progress back to the PR.
3. The skill gathers the facts with bundled scripts: `gh pr view`, `gh pr diff`, `gh pr checks`, the failed build log, review threads, gitleaks, `buf breaking`, the conventions file. These deterministic results show first.
4. Claude runs the judgment passes (and, on Re-prepare, the follow-up verdicts) and searches the codebase itself where a pass needs context. AI cards fill in as passes finish.
5. The skill writes `.sift/482/review.json`; the PR turns Ready.
6. The reviewer opens it in the desktop app (later also a Ghostty tab running the terminal UI, or a Herdr plugin pane), swipes and comments; the UI writes decisions only.
7. `sift sync` (plain code, no AI) posts comments, resolves threads the reviewer marked sorted, and submits the review through `gh`.

What it saves: no AI code inside the app, no auth to build (Claude Code and `gh` handle their own sign-in), no Lore dependency for v1. What it costs: Claude tokens per prepared review (keep it cheap by gathering facts with scripts and sending only relevant hunks), and a few minutes per review. The worker is behind one call ("prepare PR N, then read `review.json`"), so a Gemma + Lore engine can replace Claude without UI changes. Mobile still needs a server later.

Running a Gemma + Lore engine on the same PR and comparing its `review.json` with Claude's gives an evaluation of the local setup for free.

### Where AI is used, and where it isn't

Rule: ordinary code does everything that can be computed; AI only judges and summarises.

- **Code only, no AI:** PR list and quick facts, follow-up signals (replies, resolved, outdated, lines touched), target branch, impact map, collapsing assets, CI/CD change detection, hard-gate triggers, secret scanning (gitleaks), `buf breaking`, dependency card (OSV, licences), analytics diff against the tracking plan.
- **Code first, AI writes the one-liner:** build failure (a parser finds the first real error; AI summarises and guesses code vs infra), visual proof (screenshot tests produce images; AI notes what changed).
- **AI judgment, with codebase context:** naming and style, architecture fit, near-duplicate components (Lore narrows candidates with embeddings, AI confirms; in v1 Claude searches the repo), logic placement, first-party APIs, code-level performance, tests' "what this proves", compliance data-flow explanation, follow-up verdicts.

How AI calls work: one bounded call per pass, not a free-roaming agent (except where the v1 skill has Claude search the codebase). The engine assembles the context (relevant hunks, that pass's conventions rules, codebase snippets) and requires structured JSON: score, reason, evidence as file and line ranges.

Guardrails:

- **Checked evidence:** findings whose evidence doesn't map to real diff lines are dropped.
- **Rubric before model:** where a rubric can be computed (e.g. counting new booleans), code computes it and AI only explains.
- **Cache:** keyed by pass, diff hash and conventions version, so re-opening a PR costs nothing and Re-prepare only redoes what changed.
- **Model tiers and budget:** a cheap or local model for triage and chunking, Claude for judgment passes, a token budget per PR, a daily budget per machine.

### Auth and secrets (security rule)

**Sift never holds a credential.** No keys, tokens or secrets in the app's code, config files, bundles, caches or logs, and Sift has no sign-in of its own.

- **Each tool owns its own sign-in.** Claude Code signs in itself (personal subscription or organisation SSO); `gh` signs in itself (`gh auth login`). Sift only launches them and never reads, copies or stores their credentials.
- **Anything else that needs a secret gets it from the environment it runs in,** set up by the user or the OS, never read from or written to Sift's own files: for example an organisation API key for an `api-key` worker, or a Lore token. Sift's config only names *which* variable to pass through, never the value.
- **Jira:** skipped in v1. Later, prefer a tool with its own sign-in (an Atlassian CLI or the Jira connector in Claude Code) so the skill does the lookup.
- **Clean worker environment:** the worker starts with a stripped environment; only variables the config explicitly allows pass through. `ANTHROPIC_API_KEY` is removed unless the worker is set to use an API key, so nothing is billed to the wrong place.
- **No secrets in outputs:** bundles, decisions files and logs never contain credentials. Evidence snippets are scanned (gitleaks patterns) and any secret found in a diff is redacted in the snippet while still raising the Security hard gate.
- **Comments post as the reviewer** through `gh` (decided), matching named sign-offs.
- **Later, server mode:** GitHub App and installation tokens live in the server's secret manager, never in the desktop, mobile or terminal clients.

### Accounts and billing

**Now: one context per machine.** Personal and work happen on different machines, so Sift simply uses whatever `claude` and `gh` are signed in on that machine. No account routing in v1. v1 runs on the personal machine.

- **Personal machine:** personal Claude subscription. Headless `claude -p` runs for one's own reviews count as ordinary individual use and draw from the subscription's usage limits (Anthropic paused the planned move of headless usage to a separate credit on June 15, 2026; it may come back, so keep the worker swappable). Manual preparation by default keeps usage clearly ordinary.
- **Work machine:** organisation SSO seat (Team or Enterprise plan, Commercial Terms). Billing depends on the plan type (seat limits plus admin-enabled usage credits, or usage-based at API rates). Confirm with the admin before running headless reviews on the work seat, and before turning on automatic preparation there. If teammates adopt Sift, use an organisation Console API key (supplied through the environment) with its own budget instead of personal seats.
- **Both machines:** a small config file holds the worker choice (`claude-cli` or `gemma`), the daily budgets (manual and automatic), the automatic-preparation setting and the Lore URL; no secrets and nothing machine-specific hard-coded. Lore, caches and bundles stay on their own machine, so work code never leaves the work machine.
- Check the first run's usage lands on the expected dashboard (claude.ai usage page vs Console).

**Later: profiles, when both contexts share one machine.** A profile per context, chosen automatically by repo:

| | Work | Personal |
| --- | --- | --- |
| Matches | repos under the company GitHub org | repos under the personal account |
| GitHub sign-in | work account | personal account |
| AI worker | Claude Code on the SSO seat; later an org API key from the environment | Claude Code on the personal plan, local Gemma as an option or fallback |
| Lore | company Lore instance | personal Lore instance (or none) |
| Budget | reviews per day, as approved by the admin | own limit |
| Automatic preparation | own setting and disclaimer | own setting and disclaimer |
| Bundles | `.sift/work/…` | `.sift/personal/…` |

```toml
# ~/.config/sift/profiles.toml (no secrets in this file, only paths and names)
[profile.work]
match   = ["github.com/<company-org>/*"]
github  = { config_dir = "~/.config/gh-work" }   # gh keeps its own sign-in here
worker  = "claude-cli"
claude  = { config_dir = "~/.claude-work" }      # Claude Code keeps its own SSO sign-in here
budget  = { reviews_per_day = 20 }
auto    = { enabled = false }
lore    = { url = "http://localhost:8080/mcp", token_env = "LORE_TOKEN" }

[profile.personal]
match    = ["github.com/<your-user>/*"]
github   = { config_dir = "~/.config/gh-personal" }
worker   = "claude-cli"
claude   = { config_dir = "~/.claude-personal" } # personal plan sign-in
fallback = "gemma"
auto     = { enabled = false }
```

- **Separate sign-ins side by side:** each profile runs Claude Code and `gh` with its own config folder, where each tool keeps its own sign-in, so nothing needs switching. Verify early that Claude Code supports a per-account config folder (`CLAUDE_CONFIG_DIR`): sign in under each, run one headless prompt, check usage lands on the right account.
- **Automatic routing:** the PR list shows PRs from both profiles, tagged Work or Personal; preparing one runs the worker under that PR's profile.
- **Hard guardrails:** a repo matching no profile is refused, never guessed; the worker's environment is cleaned per profile; Lore, caches and bundles are separate per profile.
- **Swappable workers per profile:** `claude-cli`, `api-key` (org Console key from the environment) or `gemma` (local Ollama + Lore); a one-line change.
- **Budgets per profile:** run counts and a daily stop, which also gives real numbers to show the admin.

## Relationship to Lore

- Lore matters most for **local Gemma**: small models give good feedback only when context is prepared for them (Lore handoff cites 0.720 with prepared context vs 0.520 when a 7B model greps on its own).
- **Code-only passes don't need Lore. Claude-scored passes gain from it but work without it.** Gemma-driven and Lore-heavy passes (components, architecture fit) need it.
- Lore's MVP (`search_code`, `get_symbol`, `get_file`, `index_status`) is what unlocks the Gemma experiment. Sift's engine uses a `LoreClient` interface with a fake for tests, so both can proceed in parallel.
- Hardware note: an RTX 2070 Super (8 GB VRAM) fits Gemma 4 12B at 4-bit only tightly; models under ~7B often fail to use retrieved context. Test 12B early.

## Packaging: Claude Code plugin

One **Sift + Lore** Claude Code plugin, a thin wrapper over the vendor-neutral pieces:

- the `/sift-review` skill, its fact-gathering scripts and the `review.json` schema
- Lore MCP config (endpoint plus token from an environment variable; no token in the plugin)
- a "how to use Lore" skill (query Lore before grepping; check `index_status` for freshness)
- commands such as `/lore status` and `/lore reindex`
- an optional hook that triggers incremental indexing after agent commits

Agents running on Gemma (Pi, Paperclip with local models) won't load a Claude plugin, so the same "how to use Lore" guidance also ships as a plain instructions file. The Claude Desktop extension (`.mcpb`) in the Lore handoff is a separate distribution for non-technical colleagues.

## Planning approach

- v1 is built milestone by milestone from the build plan (M0 repo + schema + fixtures, then UI from fixtures, live PR list, deterministic skill, AI skill, sync, follow-ups).
- Use the **wayfinder** skill for the larger open areas after v1 (Lore + Gemma worker, team rollout). Keep its map in the Sift repo, not in a chat workspace.
- Use the **prototype** skill (logic branch) if the review state model needs a quick HTML check.

## Proposals and future work

The performance pipeline is planned as a separate build; the rest are proposals to discuss after v1.

- **Performance pipeline (planned, Shahriar to build).** Firebase Performance traces per affected screen: baseline before merge, new vs previous release after, regressions linked back to the PR. Metal frame-time captures when shaders change.
- **Acceptance mapping (proposal).** Map each acceptance criterion in the ticket to code and tests; unmapped criteria become a card.
- **Stacked PRs at the source (proposal).** Agents split work into proto/API, data, UI and tests PRs, so each review runs fewer passes.
- **Author attestation (proposal).** The submitter answers 2–3 generated questions about the core logic before review starts.
- **Release safety (proposal).** Check for a feature flag or kill switch and a staged rollout on risky changes.
- **Agent pre-review (opt-in proposal).** The authoring agent runs Sift on its own PR and fixes obvious findings before a human review. This is the agent's own run, set up deliberately in the pipeline; Sift never triggers it. Pre-preparing reviews from Paperclip or a GitHub Action is dropped; background preparation happens only through the automatic preparation setting.
- **Profiles (later).** Work and personal profiles on one machine; see Accounts and billing.

## Open questions

- [x] GitHub app, standalone web app, or both? → Desktop and mobile Compose Multiplatform apps, desktop first, plus a terminal UI; GitHub App later for server/mobile.
- [x] Which model does scoring and chunking? → Claude via the v1 skill; Gemma + Lore later as a swappable engine producing the same `review.json`.
- [x] AI billing? → Per machine: personal subscription on the personal machine (v1); org SSO seat on the work machine pending admin OK; org API key if the team adopts Sift; profiles later.
- [x] Where do credentials live? → Never in Sift. Claude Code and `gh` own their sign-ins; anything else comes from the environment.
- [x] When are reviews prepared? → Only when the reviewer clicks Prepare review; automatic preparation is an opt-in setting with a disclaimer and its own daily cap.
- [x] Visual direction? → Playful: Dark (v1) and Light (Shahriar's page), terminal on a real character grid.
- [x] Cards per pass or per finding? → Per finding, grouped under its pass.
- [x] Who is v1 for? → Shahriar only, personal machine, personal repos (pilot: the Weight Loss app).
- [x] Comments as the reviewer or a bot? → As the reviewer.
- [x] Jira in v1? → No; state the target branch plainly.
- [ ] Define the `review.json` schema (M0; outline in the build plan).
- [ ] Admin OK for headless Claude Code reviews on the work SSO seat (and for automatic preparation there), and which plan type the org is on.
- [ ] Can the judgment passes and follow-up verdicts run well on local Gemma 4 12B with Lore? (Test on 8 GB VRAM.)
- [ ] Follow-ups: how reliable is guessing *change requested* vs *question* from wording, and should a comment be able to opt out of tracking?
- [ ] Where do Figma links come from: ticket, PR description, or a component registry?
- [ ] Who owns the conventions file and approves rubric changes once teammates use it?
- [ ] Lore: one monorepo or several repos?
