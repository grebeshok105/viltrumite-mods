# AGENTS.md restructure — three proposals

Date: 2026-10-07. Status: **proposals only** — `AGENTS.md` is unchanged; pick one variant (or a mix) before any of this is applied.

Three independent agent sessions each wrote a restructuring proposal for the current 164-line `AGENTS.md`, with increasing radicalness. All three optimise for the same three goals:

1. **Delete instructions that do nothing** — no-ops, near-verbatim duplications, environment caches (things `ls`, `find`, or `gradle.properties` already confess).
2. **Disclose details on demand** — keep only what every task needs always-loaded; push branch-scoped material behind one-line pointers that name their trigger.
3. **Move code conventions into `CODING_STANDARDS.md`** — a new file at repo root, pointed at from `AGENTS.md`.

## The variants at a glance

| | [A — Surgeon](variant-a-surgeon.md) | [B — Architect](variant-b-architect.md) | [C — Radical](variant-c-radical.md) |
|---|---|---|---|
| Resulting `AGENTS.md` | ~136 lines (−17%) | ~58 lines (−65%) | ~27 lines (−84%) |
| Shape | Same numbered sections, same voice | Thin contract + trigger-worded reference map | Router: identity, 5 always-on rules, pointer index |
| New files | `CODING_STANDARDS.md` only | `CODING_STANDARDS.md`, `CONTRIBUTING.md`, `.memory-bank/README.md` | `CODING_STANDARDS.md`, `task-workflow` skill, `.memory-bank/README.md` |
| `docs/` created? | no | no (`CONTRIBUTING.md` at root instead) | no |
| §3 animation rules | Keep guardrails inline; class inventory → existing skill | Guardrails inline; detail → existing skill | Two catastrophic guardrails inline; body → existing skill (it already duplicates §3) |
| Main risk | Hardly any — barely lighter | Pointer-firing: agent must follow the trigger lines | Same, amplified: process lives in a *skill description*, weakest pointer in the tree |
| Best if | You want a tighter file with zero structural bets | You want real disclosure without dissolving the contract | You want minimum always-loaded context and trust pointers/skills to fire |

## What all three agree on (convergent findings)

These cuts/moves appear in every variant independently — they are the safest part of each proposal:

- **Delete env caches**: exact Forge/Mixin version strings (the `gradle.properties` pointer stays), the §2 repo-map table (filesystem confesses it), most of the tools table.
- **Delete duplicated meanings**: "reuse shared mechanisms / no islands" (stated 3×), the autonomy/escalation paragraph (2×), the doc-authority order (stated 3–4×), `original-jars/` read-only (2×).
- **§3's component inventory is already in `.agents/skills/animation-system/SKILL.md`** — the mandatory skill duplicated the section at birth (both landed in `aeec9c7`). Keep the hard rules inline; drop the copy.
- **`CODING_STANDARDS.md` owns**: mixin style (`@Unique`, self-casts, registration), networking conventions (`CoreMessages`, C2S validation), API-use rules, naming/registry-id no-renames, lang-file sync, OGG/resources, decompiled-code handling.
- **`.memory-bank/README.md`** is the natural owner of the §6 write-procedure (all three discovered it removes memory rules from the always-on path while staying one hop away).

## Where they diverge

- **Process material** (spec→plan→implement→verify, brainstorming gate, git/PR format, versioning, DoD): A keeps it all inline; B moves ship-process to `CONTRIBUTING.md`; C folds all of it into a new `task-workflow` skill.
- **Repo map**: A slims to non-discoverable rows; B/C delete the table entirely.
- **§3 animation/VFX**: A keeps the section mostly intact; B keeps a compressed guardrail block; C keeps only two catastrophic-failure lines plus the pointer.
- **Verification/versioning**: A leaves in `AGENTS.md`; B splits ship-checks into `CONTRIBUTING.md`; C routes through `task-workflow`.

## Recommendation note

B is the middle ground that achieves all three goals for real; A is the no-regret first increment (its deletions are a subset of B's); C shows the endpoint if pointer-firing proves reliable in practice. A defensible path: apply A now, adopt B's pointer map if the pointers hold, treat C as the reference for how far disclosure can go.

Each variant document is self-contained: full `AGENTS.md` draft, deletion/moves ledger with per-line justification, target-file contents, risks, and line budget.

Sessions that produced the variants: [A](https://app.devin.ai/sessions/85199217dd984835aa70323d3e9e544d) · [B](https://app.devin.ai/sessions/70a296669924483781d43eed21019b57) · [C](https://app.devin.ai/sessions/82605c95e2c045c28ddf9bf109ffe768)
