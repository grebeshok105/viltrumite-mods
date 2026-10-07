# Variant A — «Surgeon»: AGENTS.md restructure proposal

## Approach

- Radicalness: lowest of the three. Same architecture — numbered sections, same headings and same voice (short imperative sentences, simplified technical English), so the owner can diff section by section. Only §11 disappears entirely (every row failed scrutiny individually), which renumbers §§12–16 to 11–15; §9 keeps its heading as a one-line pointer.
- Optimised for provable deletions. Every cut below is quoted and labelled: no-op, near-verbatim duplication, or environment cache. No behavioural rule is deleted — several move, each behind an explicit pointer.
- `CODING_STANDARDS.md` gets only unambiguous coding-standard material: mixin style, networking conventions, naming/registry-id and API-use rules, lang-file sync, decompiled-code handling. Workflow, collaboration, verification, DoD, git, versioning and memory-bank rules stay in `AGENTS.md`.
- Progressive disclosure is used twice, both to files that already exist: §3 rule 2's component inventory behind the already-mandatory `animation-system` skill (verified on disk — its §2 System map, §3.1 priority table and §7 already enumerate it, so nothing needs writing), and §9 behind `CODING_STANDARDS.md`. No other new documents.
- Deliberately NOT done: no merging or renaming of sections, no edits to `SKILL.md` or `.memory-bank/` (nothing required it), no touching load-bearing rules (all of §3, client/server split, dependency direction, versioning rule). The server/client split stays in §8 — it is an architecture guardrail tied to §12-Verification, not a coding convention.
- Assumptions: exact version numbers are environment caches (the file's own pointer to `gradle.properties` is kept); moved content stays near-verbatim in the new file; the renumbering after §11 is safe (verified: the only external `§` reference in the repo is `.memory-bank/project.md` → §6, unchanged; all headings are preserved).

## Deletions

### Header
- `(Forge 47.3.0, … Mixin 0.8.5)` — environment cache: `gradle.properties` confesses `forge_version=47.3.0`; mixin/geckolib versions live in the build files. The file's own pointer `The exact versions are in gradle.properties and the build.gradle files.` is kept on the next line. Java 17, Mojang mappings and Mixin presence stay — coding-relevant orientation, not lookup values.
- `If a document disagrees with the current code, the code wins.` — duplication of the Documentation-section authority chain, which states the fuller rule (code and original JARs outrank every doc).

### §1 What we are building
- `Each change must make the whole project stronger. Reuse a healthy shared mechanism, extend it, or replace it on purpose. Do not add a disconnected island to avoid legacy code.` — near-verbatim duplication of §7 ¶1 (`create no duplicate systems … Do not take the shortest path when it adds duplication, one-off workarounds or future cost`). §7 is the enforceable statement of the same rule; §1 keeps its vision role.

### §2 Repository map
- The four layout rows (`viltrumitecore/…`, `…/client/`, `viltrumiteflight/…`, `*/src/main/resources/`) — environment cache: `find viltrumite*/src -type d` reproduces them verbatim. The mixin-config naming convention inside the resources row is preserved — it moves to `CODING_STANDARDS.md` §1.
- `original-jars/ | original release JARs. Read-only.` — duplication of the §8 bullet `original-jars/ is read-only` (which stays).
- `.agents/skills/ | project skills` and `.memory-bank/ | persistent agent memory (see §6)` — duplication of §10 and §6, which define both locations.

### §3 Animations and visual effects
- Rule-2 inventory — the anim-core class list (`AnimCache`, `AnimationController`, `Animation`, `AnimationParser`, `KeyframeStack`, `Easing`, `BakedGeoModel`, `GeoBone`, `AnimRenderer`), the nine model mixins, the effect paths — near-verbatim duplication: `SKILL.md` §2 System map, §3.1 priority table and §7 already enumerate the same components at equal or better granularity, and §3 rule 1 makes the skill mandatory reading for exactly this branch. Residual class names the skill does not literally name (`Animation`, `AnimationParser`, `KeyframeStack`, `Easing`, `GeoBone`) are a `ls client/anim/` environment cache; an optional one-line append to skill §7 is offered under New/changed files for zero information loss.

### §4 Collaboration
- `Do not reopen settled design decisions without a reason.` — near-verbatim duplication of the preceding `A plan never changes the user's design decisions silently.` Merged into one sentence; the rule survives intact.

### §5 Autonomy
- `Work end to end: understand the goal → read the memory bank and skills → study the code and git history → write the plan if needed → implement → build → verify in game when runtime changes → fix findings → self-review → ship.` — duplication: every element is governed by its own section (plan §4, memory bank §6, skills §10, build and in-game §12-Verification, self-review + ship §13-DoD, GitHub §14). A checklist of restated gates changes nothing versus the sections themselves.

### §6 Memory bank
- `Sources of truth: the code for implementation; the original JARs for original runtime behavior; AGENTS.md for workflow and repository rules.` — near-verbatim subset of the authority chain (`current code (implementation) and original JARs (original runtime behavior) → AGENTS.md → …`).

### §11 Tools (whole section)
- `gradle (build, runClient) | compilation, dev client with both mods` — duplication of §12-Verification's commands plus environment cache (`gradlew tasks`).
- `javap on original-jars/ | original behavior and decompile checks` — duplication of the decompiled-code rule, which moves to `CODING_STANDARDS.md` §5 carrying the same `javap -c -p` instruction.
- `git / gh | branches, history, PRs, CI runs` — environment cache; §14 covers the git/GitHub workflow.
- `Use the lightest tool that answers.` — weakest deletion in this proposal: a vague preference with no observable decision boundary, borderline no-op. Flagged in Risks.
- (The `web docs` row is not deleted — its rule moves to `CODING_STANDARDS.md` §3.)

### §15 Versioning
- `(inherited: core 1.10.3, flight 1.6.7)` — environment cache guaranteed to go stale: the same sentence orders a version bump before every jar, so the pinned values rot by design. `grep '^version' viltrumite*/build.gradle` confesses them (verified: 1.10.3 / 1.6.7 today).

## Moves

| Content | Destination | Pointer left in `AGENTS.md` |
|---|---|---|
| §8 bullets: mixin style (narrow, `@Unique`, `(Target)(Object)this`, register in correct config, `ViltrumiteMixinPlugin`); networking via `CoreMessages`/`network/packet/` + C2S validation; Mojang names + public APIs; no renames of mod ids/packages/registry ids/synced data/NBT/config keys without migration; lang-file sync | `CODING_STANDARDS.md` §1–§4 | `- Code conventions — mixin style, networking, naming and registry ids, API use, lang files — are in CODING_STANDARDS.md. Read it before writing or refactoring code.` (new §8 bullet) |
| §9 decompiled-code paragraph (Vineflower caveats, `javap -c -p` check against `original-jars/`, fix log in `.memory-bank/decompile.md`) | `CODING_STANDARDS.md` §5 | `The rules for decompiled sources are in CODING_STANDARDS.md. Read it before you trust or fix strange decompiled code.` (§9 keeps its heading, becomes this one line) |
| §2 resources-row mixin-config naming (`<modid>.mixins.json`, `<modid>.client.mixins.json`) | `CODING_STANDARDS.md` §1 — the registration bullet now names both configs | covered by the §8 conventions pointer |
| §11 `web docs` rule (`Do not guess unstable APIs from memory`) | `CODING_STANDARDS.md` §3 | covered by the §8 conventions pointer (API use) |
| §3 rule-2 component inventory | disclosed behind the existing mandatory pointer — §3 rule 1 already sends this branch to `SKILL.md`, whose §2/§3.1/§7 hold the inventory; no write needed | `2. Make all animations with the existing animation and visual system only; the skill maps every layer and its classes.` |

Supporting edits (not moves): the Documentation bullet list gains a `CODING_STANDARDS.md` entry; the authority chain gains `CODING_STANDARDS.md` between `AGENTS.md` and skills; the write-style sentence now names `CODING_STANDARDS.md`.

## Resulting AGENTS.md

```markdown
# AGENTS.md — Viltrumite Mods

Forge multi-project for Minecraft 1.20.1 (Java 17, Mojang official mappings, Mixin). This repository builds two mods:

- `viltrumitecore/`: mod id `viltrumitecore`, display name ViltrumiteForge, package `dev.baranhan.viltrumitecore`. It holds abilities, combat, grab, race, world events, ability animations and VFX. It depends on `viltrumiteflight`.
- `viltrumiteflight/`: mod id `viltrumiteflight`, package `dev.baranhan.viltrumiteflight`. It holds flight physics, flight animations and flight VFX.

The exact versions are in `gradle.properties` and the `build.gradle` files.

## 1. What we are building

This repository revives the Viltrumite mod on a new architecture. The target is a **multi-hero** superhero mod. Viltrumites come first. The owner then adds more heroes and characters, one by one. Do not design anything for one character only.

A hero must feel like its source material: mechanics, timings, interactions, animations and VFX. A hero must also be readable and playable in Minecraft. Recognizability is more important than literal copying. A hero that is faithful but not playable is a failed hero.

The source code came from decompilation of the original JARs. It is working history. It shows the current behavior. It does not define the target architecture. Keep the player-facing behavior and the current visual identity. Replace old structure when it blocks new heroes, duplicates logic, or puts one character into shared code.

`grebeshok105/Codex-Superheroes` (Fabric 1.21.1) is dropped. Use it only as a source of ideas and process. Do not copy its code or Fabric APIs into this repository.

## 2. Repository map

Only non-obvious entries are listed; the module trees are self-describing.

| Path | Content |
|---|---|
| `tools/decompile.py`, `DECOMPILE_REPORT.md` | decompile pipeline and its report |
| `.github/workflows/build.yml` | CI build. Publishes the `mod-jars` artifact. |
| `.github/workflows/decompile.yml` | one-shot decompile. Do not run it again: the sources now contain manual fixes. |

## 3. Animations and visual effects: hard rules

The current visuals are the identity of the mod. You must not break these rules without explicit approval from the user.

1. Before any work on animations, poses, model mixins, renderers, particles, shaders or VFX, read `.agents/skills/animation-system/SKILL.md` and follow it.
2. Make all animations with the existing animation and visual system only; the skill maps every layer and its classes.
3. Do not add another animation or VFX system. This includes PlayerAnimator, Emotecraft, GeckoLib animation, Veil, and any second keyframe engine or pose framework. GeckoLib is only a `compileOnly` compat dependency for one render mixin. Do not use it for new animations.
4. A hero without animations is not acceptable. A hero or ability is not done until each ability has its own animation and its own visual feedback in the current style.
5. Keep the current visual effects. Do not remove, replace, simplify or restyle an existing effect unless the user asks. A refactor must give the same visual result.
6. If the existing system cannot show a needed animation, extend the existing system in its current packages. Update the skill in the same task. Do not build a side system.
7. If the skill is missing or disagrees with the code, study `client/anim/` and the model mixins first. Then fix the skill.

## 4. How user and agent work together

The user writes the **design spec**: what and why — behavior, feel, constraints, edge cases, interactions, acceptance criteria.

The agent owns how. Before a non-trivial implementation, an implementation plan must exist. If it is missing, the agent studies the repo, the skills, the memory bank and the code, and writes the plan. Small, obvious fixes need no plan. A plan never changes the user's design decisions silently; do not reopen settled decisions without a reason.

When the user asks to invent something large with no spec (a hero, a system, a mechanic, a VFX direction, a UI), use the brainstorming gate: research, 2–3 approaches, trade-offs, alignment with the user, then a final design spec. The implementation plan comes after that.

## 5. Autonomy

Resolve every question that code, git, skills, the memory bank, the original JARs or docs can answer. Do not ask the user for routine confirmations or status checks. Escalate only real design or product forks.

## 6. Memory bank

`.memory-bank/` is the persistent project memory for AI agents. The main file is `.memory-bank/project.md`. Topic files are `.memory-bank/<topic>.md`.

- Before work: read `project.md`. Then read only the topic files for your task.
- Store only knowledge that is expensive, ambiguous or impossible to recover from the code:
  - discovered system behavior;
  - architecture relations;
  - decompile artifacts and their known fixes;
  - important invariants;
  - project-specific implementation details;
  - decisions and their reasons;
  - behavior confirmed against the original JARs.
- Do not store facts that the code shows directly.
- Do not use the memory bank as a task log, changelog, scratchpad or dump of session notes.
- After work: update the memory bank only when the task produced durable knowledge for future agents.
- Write short, factual entries that are easy to scan. Write one fact per bullet. Mark facts that are not verified as `unverified`.
- If an entry conflicts with the current code or verified JAR behavior, update or remove the entry in the same task.

## 7. Architecture quality

The implementation must work fully, fit the project logically, create no duplicate systems, live in the right place, and stay extensible. Do not take the shortest path when it adds duplication, one-off workarounds or future cost. Keep refactors in scope: a feature task is not a full rewrite.

Today the code assumes one race (Viltrumite) and puts most player logic in a few large mixins. New heroes need a hero seam. The first task that adds a second hero must introduce a shared hero contract in a plan. Do not copy Viltrumite code paths and add `if (hero == ...)` branches. Shared code asks the hero abstraction. It never asks which concrete character the player is.

## 8. Hard rules

- Gameplay is server-authoritative. Rendering, HUD, particles, camera, keybinds, screens and model mixins are client-only. Put them in `client` packages and in `*.client.mixins.json`. A dedicated server must never load a client class.
- `viltrumitecore` depends on `viltrumiteflight`. `viltrumiteflight` must not depend on `viltrumitecore`.
- `original-jars/` is read-only.
- Code conventions — mixin style, networking, naming and registry ids, API use, lang files — are in `CODING_STANDARDS.md`. Read it before writing or refactoring code.

## 9. Decompiled code

The rules for decompiled sources are in `CODING_STANDARDS.md`. Read it before you trust or fix strange decompiled code.

## 10. Skills

Project skills live in `.agents/skills/<name>/SKILL.md` with `name` and `description` frontmatter. A matching skill is a procedure: read it and follow it.

| Skill | Use for |
|---|---|
| `animation-system` | all animation, pose, model-mixin, renderer and VFX work (mandatory, see §3) |

`AGENTS.md` owns global rules. Skills own repeatable workflows. Do not make a skill for a micro-action. If a skill disagrees with the current code or this file, fix the skill.

## 11. Verification

Compilation proves nothing about behavior.

- `./gradlew build` must pass locally or in the CI `build.yml` run.
- Runtime changes (gameplay, input, rendering, entities, networking, VFX, HUD) need a check in game with `./gradlew runClient`. Check animations and VFX in first and third person. If you cannot launch the game, write this in the PR. Do not claim a check you did not do.
- If you change the client/common split or mixin configs, prove that a dedicated server starts.
- There is no automated test suite yet. For new non-trivial pure logic, add JUnit tests. The first task that needs them sets up JUnit in that module.

## 12. Definition of Done

Done means all of these are true: the design spec is fully implemented; each new ability has its animation and VFX (§3); existing visuals are unchanged unless the user asked; `./gradlew build` is green; the in-game check is done or its absence is stated; acceptance criteria are checked; the memory bank and skills are updated if the task produced durable knowledge; self-review is done; all work is committed and pushed.

## 13. Git workflow and GitHub

GitHub is the only home of the work. Work on one branch per task. Make small logical commits in English, conventional style: `feat(scope): ...`, `fix(scope): ...`, `docs(scope): ...`. Ship each finished task as its own PR. Nothing task-related stays only on a local machine.

PR format:
- **Title in Russian**, explicit, names the task («фикс захвата у вилтрумитов», not «fix» or «wip»).
- **Body starts with «Для игрока»**: what changed and how it works, for players, in Russian, no technical part. Emojis are welcome.
- **Below: the technical part for other agents**: what, why, key decisions, what you verified and how, known limits.

## 14. Versioning

Each mod has its own version in the gradle files. Before you build a jar for the user, bump the version of each changed mod: bugfix only → patch +1; features or content → minor +1, patch resets. Never ship two different jars with the same version.

## 15. Documentation

- `README.md`: public overview, in Russian.
- `AGENTS.md`: agent contract and global rules, in English.
- `CODING_STANDARDS.md`: code conventions for Java, Mixin, networking and resources, in English.
- `.agents/skills/`: current procedures.
- `.memory-bank/`: durable agent memory.

Write `AGENTS.md`, `CODING_STANDARDS.md`, skills and the memory bank in simplified technical English (ASD-STE100 principles): short sentences, active voice, one term for one concept, explicit rules, no vague words. Strict compliance is not required. Update a document in the same task that made it stale. Do not create a new markdown file if an existing one is the right place.

Authority when sources disagree: current code (implementation) and original JARs (original runtime behavior) → `AGENTS.md` → `CODING_STANDARDS.md` → skills → memory bank → other docs.
```

## New/changed files

### `CODING_STANDARDS.md` (new, repo root) — full text

```markdown
# CODING_STANDARDS.md — Viltrumite Mods

Code conventions for Java, Mixin, networking and resources in this repository. `AGENTS.md` owns workflow and global rules; this file owns how code is written. Read it before writing or refactoring code.

## 1. Mixins

- Mixins stay narrow.
- Mark added members `@Unique`.
- Write self-casts as `(Target)(Object)this`.
- Register each mixin in the correct config: `<modid>.mixins.json` for common code, `<modid>.client.mixins.json` for client-only code.
- Respect `ViltrumiteMixinPlugin`.

## 2. Networking

- Networking goes through `CoreMessages` and `network/packet/` (flight: its own `network/`).
- A C2S handler validates the sender and the player state before it acts.

## 3. Naming and API use

- Use Mojang official names and public Forge/Minecraft APIs.
- Do not guess unstable APIs from memory. Check the Forge 1.20.1 and library docs.
- Do not rename mod ids, packages, registry ids, synced data, NBT keys or config keys without a migration. Renames break worlds and configs.

## 4. Resources

- Update all lang files of a mod together.

## 5. Decompiled code

The sources are Vineflower output. Loops, casts, lambdas, switches and generics can be wrong. Before you trust strange code, compare it with the original bytecode (`javap -c -p` on the class from `original-jars/`). Record each confirmed decompile fix in `.memory-bank/decompile.md`.
```

### `.agents/skills/animation-system/SKILL.md` — no change required

The inventory already exists (§2 System map, §3.1 priority table, §7 anim-core API). Optional zero-loss append to §7 for the five class names the skill does not literally name: `- anim/ internals: Animation, AnimationParser, KeyframeStack, Easing, GeoBone.` (1 line; only needed if the owner wants every name from the old §3.2 discoverable inside the skill).

### `.memory-bank/project.md` — no change

Its pointer (`See AGENTS.md §6`) stays valid — §6 is unchanged.

## Risks

- Pointer firing: the whole of `CODING_STANDARDS.md` hangs on one §8 bullet. The trigger wording is deliberately broad (`before writing or refactoring code`) and sits in the highest-salience section, but an agent that treats §8 as an exhaustive checklist could still miss it. Mitigation kept deliberately cheap: the §9 stub is a second, branch-scoped pointer for the decompile branch.
- §5 pipeline deletion: the single-line `end to end → ship` sequence is a visible ritual some owners like. Each element survives in §4/§6/§10/§11/§12/§13, but restoring the line costs +1 line if the owner misses the ritual.
- `Use the lightest tool that answers.` is the least certain deletion in this proposal (vague preference vs. true no-op). Restore cost: +1 line.
- §2 slimming removes the at-a-glance package map; new agents orient via `ls`/`find` (one step). Kept rows preserve the non-discoverable gotchas (decompile do-not-run, CI artifact, pipeline report).
- §3 rule 2: the enumerated whitelist of classes leaves always-loaded context; if an agent violates rule 1 (skips the skill) it loses the list. Acceptable because rule 1 is unconditional and the skill's map is richer (adds mixin priorities and layers the inventory lacked).
- Renumbering §§12–16 → 11–15: verified no stale references — the only `§` link outside `AGENTS.md` is `.memory-bank/project.md` → §6 (unchanged); §10's `(see §3)` is unchanged; markdown heading anchors all keep working because every kept heading is byte-identical.
- Two pointers to `CODING_STANDARDS.md` (§8 and §9) is deliberate, not duplication: different branches (general code work vs. decompile handling).

## Line budget

| | Lines |
|---|---|
| Current `AGENTS.md` | 164 |
| Proposed `AGENTS.md` | ~136 (−28, −17%) |
| `CODING_STANDARDS.md` (new) | ~33 |

Where the 28 went (rough):
- ~24 deleted outright — no-ops, near-verbatim duplications, environment caches (including all of §11 except its one moved rule).
- ~7 moved into `CODING_STANDARDS.md` (5 §8 bullets + §9 paragraph + §11 web-docs rule + the mixin-config naming fragment).
- ~3 disclosed behind the existing skill pointer (§3 rule-2 inventory; content already lives in `SKILL.md`).
- +6 added back — §2 intro line, §8 + §9 pointer lines, §16 doc-list entry, §3.2 replacement sentence, §16 chain edits.

Everything else is unchanged text, moved or not — no behavioural rule was dropped.