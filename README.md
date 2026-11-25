# Sift

Sift turns a 2,000-line AI-generated pull request into a short deck of cards, so a human reviews what matters and skips what doesn't.

AI now writes most of the code. PRs get large, and line-by-line diffs cause review fatigue. Sift does the sorting and leaves the judgment with the reviewer.

> **Status:** pre-alpha. The spec is written; no app code exists yet.

## How a review works

1. **Status note.** One glance at the top: build status (with a summary of why it fails), target branch, and the few cards that need you.
2. **Swipe deck.** One card per finding, highest concern first. Swipe right if it looks fine, left to dig in. "Show me the code" opens only the snippet the card refers to. On desktop, keys replace swipes: `←` `→` `Space` `Z` `C` `R` `F` `Esc`.
3. **Red queue.** Revisit only the cards you flagged, comment against the snippet, and submit as approve, request changes or pending. Comments sync back to GitHub as normal review comments.
4. **Follow-ups.** On a re-review, Sift shows your earlier comments first with the author's replies and the new diff, then covers only what changed.

## Principles

- **Quiet by default.** Anything CI, linters or tooling already enforce is never repeated.
- **Reviewer-led.** Sift sorts and summarises; every decision stays with the reviewer.
- **Accountable.** Hard gates (secrets, CI/CD changes, breaking changes) need a recorded sign-off.
- **Holds no secrets.** Sift never stores a credential. `gh` and Claude Code own their own sign-ins.
- **Spends only when asked.** Opening the app and listing PRs uses no AI. A review is prepared only when you click Prepare, unless you opt in to automatic preparation.

## Requirements

- [GitHub CLI](https://cli.github.com/) (`gh`), signed in
- [Claude Code](https://claude.com/claude-code), signed in

## Docs

- [Spec](docs/plans/sift-spec.md)
- [Design canvas](https://claude.ai/artifact/EnKMCRHPJzvogRGD68yc6Q) (Playful Dark is the v1 theme)

## License

[MIT](LICENSE)
