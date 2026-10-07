# Variant C — Radical: AGENTS.md as a router

## Approach

- **Radicalness level: maximum defensible.** `AGENTS.md` goes from a 164-line contract to a 27-line router: identity, five always-on rules, one pointer index, and a meta-rules footer. That is an ~84% cut in always-loaded lines.
- **Optimised for:** every surviving line must either fire on *every* task or be a pointer whose trigger names its branches. Everything else pays zero context load.
- **Key discovery that enables the cut:** §3's entire body already exists nearly verbatim in `.agents/skills/animation-system/SKILL.md` — the file duplicated its own mandatory pointer target at birth (both landed in `aeec9c7`). The radical move is not moving §3; it is *deleting the copy* and keeping only the pointer plus the two catastrophic-failure guardrails inline.
- **Disclosure tree:** three new files, zero new directories (no `docs/`). `CODING_STANDARDS.md` at root owns all "how to write code here" material including verification; one new skill `task-workflow` owns process (planning, brainstorming gate, git/PR, versioning, DoD); `.memory-bank/README.md` owns the memory procedure. The memory bank and the animation skill — the two pointer targets that already exist — stay unchanged.
- **Deliberately NOT done:** no behavioral rule is silently deleted (every rule lands somewhere named; each deletion below is a duplication, an environment cache, or a provable no-op); `task-workflow` is not split into planning/shipping skills (one pipeline, one pointer — a split would double pointer surface for no branch gain); `README.md`, `DECOMPILE_REPORT.md`, `.memory-bank/*.md` are untouched.
- **Stated assumption:** the harness indexes `.agents/skills/*/SKILL.md` descriptions (verified in this session — `animation-system` is visible in the skill index). For agents that do not index skills, the index rows still work as plain file+trigger pointers.

## Deletions

Judgement per line: **dup** = same meaning lives elsewhere (deleted, single source kept); **cache** = the environment confesses it cheaper; **no-op** = removing it changes no behaviour vs. the default.

### Header / §1 What we are building

| Deleted text | Why |
|---|---|
| "(Forge 47.3.0, …, Mixin 0.8.5)" exact versions | **cache** — `gradle.properties` (forge_version, mapping_*, geckolib_version) and the `build.gradle` plugin block confess them. Kept only the identity qualifiers "Java 17, Mojang mappings, Mixin". |
| "The exact versions are in `gradle.properties` and the `build.gradle` files." | **no-op meta-line** — tells the agent where a cache would live; deleting it changes nothing. |
| "This repository revives the Viltrumite mod… Viltrumites come first. The owner then adds more heroes… one by one." | **dup** — `project.md` "Direction" says the same, and always-on rule 4 makes `project.md` the first file every task reads. Behavioural residue ("never design for one character") moves to `CODING_STANDARDS.md`. |
| "A hero must feel like its source material… a failed hero." | branch-specific design truth — **moved** to the brainstorming gate in `task-workflow`, where it fires exactly when a hero is being designed (see Moves). |
| "The source code came from decompilation… does not define the target architecture… Replace old structure when…" | **dup + move** — origin fact duplicates `project.md` "Origin"; the behavioural part moves to `CODING_STANDARDS.md` → Architecture. |
| "`grebeshok105/Codex-Superheroes` (Fabric 1.21.1) is dropped. Use it only as a source of ideas…" | **dup** — `project.md` carries it. The prohibition half moves to `CODING_STANDARDS.md` → Boundaries (one line). |

### §2 Repository map — whole table deleted

| Deleted text | Why |
|---|---|
| Package-content rows (`ability/`, `mixin/`, `client/anim/`…) | **cache** — `find src -type d` answers it in one command. The relations *not* derivable (synced-data pattern, `PlayerEntityCoreMixin` ~1500 lines as refactor target) already live in `project.md`, which is always read. |
| "`*/src/main/resources/` | mixin configs…" | **cache/dup** — the two-config convention is restated in `CODING_STANDARDS.md` → Boundaries where it is a rule, not a tour stop. |
| "`original-jars/` | …Read-only" | **dup** — one mention survives in the header + `CODING_STANDARDS.md` → Boundaries. |
| "`tools/decompile.py`, `DECOMPILE_REPORT.md`" | **cache** — README explains the pipeline for humans; `decompile.md` for agents. |
| "`.github/workflows/build.yml` | CI build…" | **cache** — README + the Actions tab confess it. |
| "`.github/workflows/decompile.yml` | one-shot decompile. Do not run it again…" | **dead-branch guard, triplicated**: it already exists verbatim in `.memory-bank/decompile.md` ("Do not run `decompile.yml` again…") and in `README.md`. The agent-facing single copy lands in `CODING_STANDARDS.md` → Decompiled code (an agent editing CI is on the "writing/changing code" branch). |
| "`.agents/skills/`, `.memory-bank/` rows | **dup** — the index rows replace them with triggers attached. |

### §3 Animations and VFX — replaced by pointer + one inline guardrail

| Deleted text | Why |
|---|---|
| Rule 1 "read `.agents/skills/animation-system/SKILL.md`" | becomes the pointer itself (index row 2). |
| Rule 2 (use only the existing system; the class inventory) | **dup** — SKILL.md §1 rules 2+4 and §2's layer map carry it. Class inventories are also **cache** (the skill names the canonical entry points already). |
| Rule 3 (no second engine: PlayerAnimator, Emotecraft, GeckoLib animation, Veil…) | **dup** — SKILL.md §1 rule 2 is word-for-word the same ban list. The *guardrail* survives inline as always-on rule 2 because it is the catastrophic variant an agent could hit without classifying its work as "VFX work". |
| Rule 4 "A hero without animations is not acceptable…" | **dup** — SKILL.md §1 rule 3. |
| Rule 5 "Keep the current visual effects… same visual result" | **dup** — SKILL.md §1 rule 1. Catastrophic half survives in always-on rule 2. |
| Rule 6 "extend the existing system… Update the skill in the same task" | **dup** — SKILL.md §1 rules 4–5 + the footer's stale-doc rule covers "update the skill". |
| Rule 7 "If the skill is missing or disagrees with the code, study `client/anim/`… Then fix the skill" | **dup** — SKILL.md header ("trust the code and update this file") + footer fix-the-skill rule. |

### §4–§5 Process sections

| Deleted text | Why |
|---|---|
| §5 arrow chain "understand the goal → read the memory bank → … → ship" | **dup four times over** — §4 owns plans, §6 owns the pre-work read, §12 owns build/verify, §13 owns ship. One rule in four costumes. |
| §5 "Resolve every question… Do not ask the user for routine confirmations… Escalate only real design or product forks" | borderline **no-op for Devin** — the org's `engineering-workflow` plugin rule already mandates this verbatim. **Kept anyway** (compressed, in `task-workflow` + an echo in always-on rule 4) because non-Devin agents read this repo and the owner stated it explicitly. Flagged as a judgement call, not a hidden cut. |
| §4 "studies the repo, the skills, the memory bank and the code" | **dup** of the §5 chain's study step; kept once in `task-workflow`. |

### §8–§11

| Deleted text | Why |
|---|---|
| §8 "Use Mojang official names…", mixin style, networking, lang bullets | **moved** to `CODING_STANDARDS.md` (branch-reached, not every-task). Client/server + dependency direction stay inline — they fire on every file creation. |
| §10 skill table + "AGENTS.md owns global rules. Skills own repeatable workflows" | **dup/meta** — the index table now *is* the skill table with triggers; the mechanism sentence compresses into the footer. "Do not make a skill for a micro-action" → footer. |
| §11 "Use the lightest tool that answers." | **no-op exhortation** — it is the default; the sentence can be deleted whole. |
| §11 tool table (gradle / javap / git / web docs) | **cache** — `gradlew tasks`, README and `--help` confess it. Salvage: `javap` usage lands in `CODING_STANDARDS.md` → Decompiled code; "do not guess unstable APIs" → External APIs. |

### §15–§16

| Deleted text | Why |
|---|---|
| §15 "(inherited: core 1.10.3, flight 1.6.7)" | **cache, already rotten** — the unmerged Regulus branch carries `1.11.0`/`1.7.0`. Live proof that embedded versions go stale; the rule survives, the numbers die. |
| §16 four-bullet doc inventory | **dup** — the index table is the inventory, now with triggers. |
| §16 "Authority when sources disagree: …" | **dup ×3** — the same chain appears as the header's "the code wins" and §6's "Sources of truth" bullet. One chain kept as always-on rule 5; `memory-bank/README.md` keeps its own narrow version for that file's scope. |
| §16 ASD-STE100 paragraph + "Update a document in the same task…" | **moved+compressed** into the footer (one sentence each). |
| §12 "There is no automated test suite yet. … The first task that needs them sets up JUnit" | **stale-on-arrival** — the Regulus branch already added JUnit 5.9.3 + tests to `viltrumitecore`. Reworded in `CODING_STANDARDS.md` so it survives the merge. |

## Moves

| Content (from) | Destination | Exact pointer line(s) left in `AGENTS.md` |
|---|---|---|
| §8 mixin/networking/naming/lang rules, §7 architecture + hero seam, §9 decompile rules, §12 verification, Codex-Superheroes ban | `CODING_STANDARDS.md` (new, root) | Index row: `\| `CODING_STANDARDS.md` \| writing or changing code — mixin, networking and naming rules, the hero-seam rule (adding a hero), decompile traps, verification \|` |
| §3 rules 2–7 (already duplicated there) | `.agents/skills/animation-system/SKILL.md` — **unchanged** | Index row: `\| `.agents/skills/animation-system/SKILL.md` \| **mandatory** before any work on animations, poses, model mixins, renderers, particles, shaders or VFX \|` + always-on rule 2 (guardrail echo) |
| §4 roles/plan, §4 brainstorming gate + §1 hero-design truths, §5 autonomy, §13 DoD, §14 git/PR, §15 versioning | `.agents/skills/task-workflow/SKILL.md` (new) | Index row: `\| `.agents/skills/task-workflow/SKILL.md` \| every non-trivial task — plan vs. spec, the no-spec brainstorming gate, git and PR format, version bumps, definition of done \|` |
| §6 memory-bank procedure | `.memory-bank/README.md` (new) | Index row: `\| `.memory-bank/README.md` \| before writing memory-bank entries \|` + always-on rule 4 keeps "read `project.md`" inline (a pointer cannot load its own trigger) |
| §9 "record each confirmed decompile fix" ledger | `.memory-bank/decompile.md` — **unchanged**, no index row | Deliberately two-hop: reachable via `project.md`'s "Topic files" list and `CODING_STANDARDS.md` → Decompiled code. A fifth index row would pay a line for a branch already covered. |

## Resulting AGENTS.md

```markdown
# AGENTS.md — Viltrumite Mods

Forge 1.20.1 workspace (Java 17, Mojang mappings, Mixin) building two mods under `dev.baranhan.*`:

- `viltrumitecore` — abilities, combat, race, world events, animations and VFX. Depends on `viltrumiteflight`.
- `viltrumiteflight` — flight physics, flight animations and VFX.

The sources are decompiled from `original-jars/` (read-only). When a document disagrees with the code, the code wins.

## Always-on rules

1. Client-side code (rendering, HUD, particles, camera, keybinds, screens, model mixins) goes in `client` packages and `*.client.mixins.json`; a dedicated server must never load a client class. `viltrumiteflight` never depends on `viltrumitecore`.
2. This repo has exactly one animation/VFX system, and its visuals are the mod's identity: never add a second engine (PlayerAnimator, Emotecraft, GeckoLib animation, Veil, another keyframe or pose framework), and never remove, simplify or restyle an existing effect unless the user asks.
3. Never rename mod ids, packages, registry ids, synced data, NBT keys or config keys without a migration — renames break worlds and configs.
4. Before work, read `.memory-bank/project.md`. Resolve questions from the code, git history, skills, the memory bank and the original JARs; escalate only real design or product forks.
5. When sources disagree: current code and original JARs > `AGENTS.md` > skills > memory bank > other docs.

## Index

| Doc | Reach for it when |
|---|---|
| `CODING_STANDARDS.md` | writing or changing code — mixin, networking and naming rules, the hero-seam rule (adding a hero), decompile traps, verification |
| `.agents/skills/animation-system/SKILL.md` | **mandatory** before any work on animations, poses, model mixins, renderers, particles, shaders or VFX |
| `.agents/skills/task-workflow/SKILL.md` | every non-trivial task — plan vs. spec, the no-spec brainstorming gate, git and PR format, version bumps, definition of done |
| `.memory-bank/README.md` | before writing memory-bank entries |

A skill is a procedure: when its trigger matches, read the SKILL.md and follow it; if it disagrees with the code or this file, fix the skill in the same task. Do not create a skill for a micro-action. Write agent-facing docs in simplified technical English — short sentences, active voice, one term per concept — and update a doc in the same task that made it stale.
```

**27 lines** — versus 164.

## New/changed files

### `CODING_STANDARDS.md` (new, repo root, 48 lines) — full text

```markdown
# CODING_STANDARDS.md — Viltrumite Mods

Rules for writing and changing code in this repository. `AGENTS.md` holds the always-on subset; this file holds the full rules.

## Boundaries

- Gameplay is server-authoritative. Rendering, HUD, particles, camera, keybinds, screens and model mixins are client-only: put them in `client` packages and register them in `*.client.mixins.json` (not `<modid>.mixins.json`). A dedicated server must never load a client class.
- `viltrumitecore` depends on `viltrumiteflight`; flight must never depend on core.
- Packages: `dev.baranhan.viltrumitecore`, `dev.baranhan.viltrumiteflight`. Use Mojang official names and public Forge/Minecraft APIs.
- Do not copy code or Fabric APIs from `grebeshok105/Codex-Superheroes` (dropped; idea source only).
- `original-jars/` is read-only.
- Update all lang files of a mod together.

## Mixins

- Keep each mixin narrow. Mark added members `@Unique`. Write self-casts as `(Target)(Object)this` — plain `(Target)this` does not compile.
- Register each mixin in the correct config; `defaultRequire` is 1, so a bad target crashes the game.
- Respect `ViltrumiteMixinPlugin`.

## Networking

- Core packets go through `CoreMessages` and `network/packet/`; flight uses its own `network/`.
- A C2S handler validates the sender and the player state before it acts.

## Architecture

- Extend a healthy shared mechanism, or replace it on purpose. Do not add a disconnected island, a duplicate system or a one-off workaround to avoid legacy code.
- The target is multi-hero: never design shared code for one character only. No hero abstraction exists yet — the first task that adds a second hero must introduce a shared hero contract in its plan. Shared code asks the hero abstraction; it never asks which concrete character the player is. Do not copy a Viltrumite code path and add `if (hero == ...)` branches.
- Keep refactors in scope: a feature task is not a full rewrite.
- The decompiled structure is working history, not the target architecture. Keep player-facing behavior and visual identity; replace structure that blocks new heroes or duplicates logic.

## Decompiled code

- The sources are Vineflower output: loops, casts, lambdas, switches and generics can be wrong. Before you trust strange code, compare it with the original bytecode: `javap -c -p` on the class from `original-jars/`.
- Record each confirmed decompile fix in `.memory-bank/decompile.md`.
- Never run `.github/workflows/decompile.yml` again — it would overwrite the manual fixes in the sources.

## External APIs

- Do not guess unstable Forge, Minecraft or library APIs from memory. Check the decompiled usage in this repo or current docs.

## Verification

- Compilation proves nothing about behavior.
- `./gradlew build` must pass, locally or in the CI `build.yml` run. On Devin machines prefix Gradle commands with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` — the default JDK 21 fails the build.
- Runtime changes (gameplay, input, rendering, entities, networking, VFX, HUD) need an in-game check with `./gradlew runClient`; check animations and VFX in first and third person. If you cannot launch the game, say so in the PR — never claim a check you did not do.
- If you change the client/common split or mixin configs, prove that a dedicated server starts: `printf 'eula=true\n' > viltrumitecore/run/eula.txt`, then `./gradlew :viltrumitecore:runServer`.
- Add JUnit tests for new non-trivial pure logic. `viltrumitecore` has the JUnit setup; the first task that needs tests in another module sets it up there.
```

Flagged additions inside it (new content, not restructure): the `JAVA_HOME` prefix line and the concrete `runServer` command — both are verified traps/commands from this org's sessions, not derivable from the code (no toolchain pin in `build.gradle`); the JUnit bullet is reworded because the Regulus branch already adds JUnit.

### `.agents/skills/task-workflow/SKILL.md` (new, 45 lines) — full text

```markdown
---
name: task-workflow
description: Use for every non-trivial task in this repo — planning vs. the user's design spec, the brainstorming gate for spec-less inventions (a hero, system, mechanic, VFX direction or UI), git and PR format, version bumps before jar builds, and the definition of done.
---

# Task workflow (viltrumite-mods)

## Roles

- The user writes the design spec: what and why — behavior, feel, constraints, edge cases, interactions, acceptance criteria.
- The agent owns how. A non-trivial implementation needs an implementation plan before code: study the repo, the skills, the memory bank and the code, then write it. Small, obvious fixes need no plan. A plan never changes the user's design decisions silently. Do not reopen settled design decisions without a reason.
- Resolve every question that code, git history, skills, the memory bank, the original JARs or docs can answer. Do not ask the user for routine confirmations or status checks. Escalate only real design or product forks.

## Brainstorming gate

When the user asks to invent something large with no spec (a hero, a system, a mechanic, a VFX direction, a UI): research → 2–3 approaches with trade-offs → align with the user → final design spec → then the implementation plan.

Design truths for heroes:

- A hero must feel like its source material — mechanics, timings, interactions, animations and VFX — and stay readable and playable in Minecraft. Recognizability beats literal copying. A faithful but unplayable hero is a failed hero.
- The mod is PvP only; balance and mechanics never target mobs or PvE.

## Git and PRs

GitHub is the only home of the work: one branch per task, small logical commits in English conventional style (`feat(scope):`, `fix(scope):`, `docs(scope):`), one PR per finished task. Nothing task-related stays only on a local machine.

PR format:

- Title in Russian, explicit, naming the task («фикс захвата у вилтрумитов», not «fix» or «wip»).
- Body starts with «Для игрока»: what changed and how it works, for players, in Russian, no technical part. Emojis welcome.
- Below that, the technical part for other agents: what, why, key decisions, what you verified and how, known limits.

## Versioning

Each mod has its own `version =` in its `build.gradle`. Before you build a jar for the user, bump each changed mod: bugfix only → patch +1; features or content → minor +1, patch resets. Never ship two different jars with the same version.

## Definition of done

Done means all of these are true:

- the design spec is fully implemented and the acceptance criteria are checked;
- each new ability has its animation and VFX (see the `animation-system` skill), and existing visuals are unchanged unless the user asks;
- `./gradlew build` is green and the in-game check is done — or its absence is stated in the PR (see `CODING_STANDARDS.md` → Verification);
- the memory bank and skills are updated if the task produced durable knowledge;
- self-review is done; all work is committed and pushed.
```

Flagged addition: the "PvP only" bullet — an owner-stated rule (2026-10-06) that never landed in the repo docs; it belongs with hero-design material, not inline. Cross-reference fix: DoD now names `animation-system` and `CODING_STANDARDS.md` instead of dead §-numbers.

### `.memory-bank/README.md` (new, 24 lines) — full text

```markdown
# Memory bank

`.memory-bank/` is the persistent project memory for AI agents. The main file is `project.md` — read it before every task, then read only the topic files for your task. Topic files are `<topic>.md`.

## What to store

Only knowledge that is expensive, ambiguous or impossible to recover from the code:

- discovered system behavior;
- architecture relations;
- decompile artifacts and their known fixes;
- important invariants;
- project-specific implementation details;
- decisions and their reasons;
- behavior confirmed against the original JARs.

## Rules

- Do not store facts that the code shows directly.
- Not a task log, changelog, scratchpad or dump of session notes.
- After work: update only when the task produced durable knowledge for future agents.
- Write short, factual entries, one fact per bullet. Mark unverified facts `unverified`.
- Sources of truth: the code for implementation; the original JARs for original runtime behavior; `AGENTS.md` and `.agents/skills/` for rules.
- If an entry conflicts with the current code or verified JAR behavior, update or remove it in the same task.
```

### Unchanged files

- `.agents/skills/animation-system/SKILL.md` — needs nothing; §3 already lived in it.
- `.memory-bank/project.md`, `.memory-bank/decompile.md`, `README.md`, `DECOMPILE_REPORT.md` — untouched.
- Optional (flagged, not required): add one bullet to `project.md` "Direction": "The mod is PvP only; balance and mechanics never target mobs/PvE." — it is read before every task, so the rule lands where it is cheapest.

## Risks

1. **`task-workflow` pointer-firing is the biggest bet.** PR format, versioning and DoD now load only when the skill's description fires or the index row is followed. Mitigations: the description front-loads ship-time branches ("git and PR format, version bumps, definition of done"), the row says "every non-trivial task", and `.agents/skills/` is harness-indexed here. Residual risk an agent ships a malformed PR without reading it — real but bounded: the owner reviews every PR. If that is unacceptable, the fallback is +2 inline lines compressing "Russian title / «Для игрока» body / bump versions before jar builds" — the only ship-time format that has no other home.
2. **Animation detail fully behind the skill.** An agent whose work *affects* visuals but does not self-classify as VFX work (e.g. touching `ViltrumiteCorePlayer` synced state that poses read) could skip the skill. Mitigation: always-on rule 2 covers the two catastrophic failures (second engine; restyled/removed effects), and "renderers" is in the trigger list.
3. **No repo map.** First-contact orientation now costs a `find`. Accepted: the map was a directory cache; the parts that are expensive to rediscover (synced-data architecture, `PlayerEntityCoreMixin` as seam target, anim-loader location) are durable in `project.md`, which is always read.
4. **Ownership boundary between `CODING_STANDARDS.md` and `task-workflow`.** Verification lives in standards (it is about proving code correct); DoD lives in workflow (it is about shipping). They cross-reference by name, never duplicate the rule text. Risk: an agent reads one and misses the other's edge — mitigated by DoD naming the standards section.
5. **Meta-rules weakened.** "Update docs in the same task", "read the memory bank", autonomy — compressed to footer/rule lines. If these lines originally existed because agents *actually failed* them, compression bets that the failures came from length, not wording. Honest exposure: a compressed rule that was load-bearing at paragraph length could regress; the fix path is cheap (re-inflate that one rule).
6. **`decompile.md` has no index row.** Deliberate: reached via `project.md` topic list and `CODING_STANDARDS.md`. An agent fixing suspicious code without opening standards could miss the ledger — low cost, since writing to the ledger is itself instructed inside the standards section it just read.
7. **Every pointer-based design shares one systemic risk**: material an agent never reaches is invisible. This variant caps the exposure: all catastrophic or irreversible failure modes (client class on dedicated server, second VFX engine, effect removal, world-breaking renames, dependency inversion) stay inline in five rules. Everything else is recoverable error.

## Line budget

| | Lines |
|---|---|
| Current `AGENTS.md` (always loaded) | 164 |
| Proposed `AGENTS.md` (always loaded) | **27** |
| `CODING_STANDARDS.md` (loads on code tasks) | 48 |
| `task-workflow` skill (loads on non-trivial tasks) | 45 |
| `.memory-bank/README.md` (loads on memory writes) | 24 |
| Deleted outright (map, tools, version cache, duplicated chains, meta-lines, §3 copy) | ~67 |
| Total written vs. before | ~144 vs 164, but a typical task loads 27 + 1–2 targets instead of 164 always |
