# Sift

Human-first review tool for AI-generated PRs. Spec: `docs/plans/sift-spec.md`. v1 plan: `docs/plans/v1-slices.md` (one slice = one GitHub issue). Design canvas "Sift — UI": https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q, Playful (Dark) is the v1 theme.

## Non-negotiable rules

- Sift never holds a credential: no keys or tokens in code, config, fixtures, bundles or logs. Claude Code and `gh` own their sign-ins; anything else comes from the environment, and config only names the variable.
- The worker runs with a stripped environment; `ANTHROPIC_API_KEY` never passes through unless the worker is explicitly set to an API key.
- The UI never decides anything. It renders review state and sends intents. `review.json` is the contract; the decisions file is the UI's only output.
- Code computes everything computable; AI only judges and summarises. Every AI finding must carry evidence that maps to real diff lines, or it is dropped.
- Nothing spends AI without the reviewer asking (Prepare / Re-prepare), except the opt-in auto-prep setting.
- Colours, type and radii come from theme tokens, never literals, so the light theme drops in later.

## Modules

| Module | What |
| --- | --- |
| `core-model` | Kotlin Multiplatform models shared by the engine and every UI |
| `engine` | JVM: PR list via `gh`, queue, worker launcher, budget, sync, follow-up signals |
| `app-desktop` | Compose Multiplatform, desktop target |

Package root: `io.github.anwarshahriar.sift`.

## Build

Needs JDK 21 (`JAVA_HOME`). Versions live in `gradle/libs.versions.toml`.

- `./gradlew build`: compile and test everything
- `./gradlew :app-desktop:run`: launch the desktop app

## Workflow

- Work happens on a branch per issue and lands through a PR; never commit to `main`.
- PRs are opened by the `anwar-autobot` account so the owner can review them in Sift. Keep `AnwarShahriar` as gh's active account; switch to the bot only to push and open a PR.
- `docs/temp/` is gitignored scratch for handoffs and temporary work. Handoffs are `docs/temp/handoff-YYYY-MM-DD.md`; when resuming, read the newest one.
- GitHub tokens are fine-grained (owner) and classic (bot): there is no Checks API access, so read CI from Actions runs and commit statuses.
