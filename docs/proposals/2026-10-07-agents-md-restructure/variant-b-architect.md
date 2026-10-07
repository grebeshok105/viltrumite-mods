# Variant B — «Architect»: AGENTS.md restructure proposal

## Approach

- **Radicalness: middle.** A surgical pass keeps §§2/6/9–16 inline; a maximal pass would dissolve the file into pointers. This proposal instead rebuilds `AGENTS.md` as a thin *contract* — identity, direction, collaboration protocol, guardrails, hard rules — and discloses everything branch-scoped behind a trigger-worded pointer map.
- Optimised for **trigger fidelity**: every disclosed doc sits behind a one-line pointer that names the *branch* that should reach it ("Writing or changing code", "Shipping a PR or building a jar", "Decompiled code looks wrong"), so the material loads exactly when its task needs it.
- **Deduped by meaning, not by section.** "Reuse shared mechanisms / no islands" (§1+§7), the autonomy/escalation paragraph (§4+§5), the authority order (header + §3.7 + §6 + §16), and `original-jars/` read-only (§2+§8) each now live in exactly one place.
- **Two new docs beyond the mandatory `CODING_STANDARDS.md`:** `CONTRIBUTING.md` (ship-time process) and `.memory-bank/README.md` (memory write rules). Justification below — each owns a distinct trigger branch that `CODING_STANDARDS.md` doesn't.
- **Not done:** no `docs/` directory (would create a doc whose only job is restating `ls`); no splitting the animation skill (it's already the pointer target); no rewriting `README.md` (Russian, player-facing, out of scope); the skill's own content untouched.
- Assumption: agents honor mandatory pointers that name their branch — the same bet goal 3 already makes; the pointer table makes the obligation explicit rather than hoping a doc title suffices.

## Deletions

Grouped by current section. Verdicts: **no-op** (removal changes no behaviour vs. the default), **duplication** (same meaning stated elsewhere), **env cache** (one-command lookup confesses it).

### Header
- "(Forge 47.3.0, Java 17, Mojang official mappings, Mixin 0.8.5)" — **env cache** for Forge/Mixin versions (`gradle.properties` confesses both). Kept `Java 17` (changes which JDK runs gradle — the machine default is 21) and `Mojang mappings` (shapes how every signature reads).
- "The exact versions are in `gradle.properties` and the `build.gradle` files." — kept compressed; it is a useful anti-stale pointer, not a cache itself.
- "If a document disagrees with the current code, the code wins." — **duplication**; this authority claim appears four times (here, §3.7's "study the code first", §6's "sources of truth", §16's ladder). Single-sourced into the §5 authority line.

### §1 — What we are building
- "Each change must make the whole project stronger. Reuse a healthy shared mechanism, extend it, or replace it on purpose. Do not add a disconnected island to avoid legacy code." — **duplication** of §7 ¶1 ("fit the project logically, create no duplicate systems, live in the right place"). Kept once, compressed, in §1.
- "It shows the current behavior." / "It does not define the target architecture." — kept compressed ("working history, not target architecture"); the meaning is load-bearing, the sentences were just long.

### §2 — Repository map (whole section, ~13 lines)
- The path table — **env cache**. `ls`/`find` confesses the layout; mixin-config filenames (`<modid>.client.mixins.json`) confess themselves in `resources/`; `original-jars/`'s read-only rule is restated in §8 (kept, in the pointer line); the `decompile.yml` do-not-rerun warning is restated verbatim in `.memory-bank/decompile.md` line 7 (single-sourced there). The two bits the layout can't confess — `PlayerEntityCoreMixin` as refactor target, the `client/anim/` geo loader — already live in `.memory-bank/project.md` "Architecture relations", which the new map reads before every task.

### §3 — Animations and VFX
- Rule 2's class enumeration ("`AnimCache`, `AnimationController`, `Animation`, `AnimationParser`, `KeyframeStack`, `Easing`, `BakedGeoModel`, `GeoBone`, `AnimRenderer`" + the 9-mixin list + render dirs) — **duplication**: the skill's §2 system map is a strictly richer version of the same list. The guardrail keeps the *layer names* ("animation core, model mixins, render layers, particles, post shaders") and hands the map to the skill.
- Rule 3's framework list — trimmed but kept: PlayerAnimator, Emotecraft, GeckoLib animation, Veil are the *guardrail's* load-bearing names (they recruit the exact libraries an agent would otherwise reach for). The GeckoLib compat caveat stays.
- Rule 7's "study `client/anim/` and the model mixins first" — **no-op** (that is what an agent does when the skill disagrees with code; the authority ladder covers it). Kept the second half: "fix the skill."

### §4 — Collaboration
- "If it is missing, the agent studies the repo, the skills, the memory bank and the code, and writes the plan." — compressed into the §2 pipeline (one statement of the same meaning).
- "The implementation plan comes after that." — **no-op**; the gate's arrow already orders it.

### §5 — Autonomy
- "Resolve every question that code, git, skills, the memory bank, the original JARs or docs can answer." + "Escalate only real design or product forks." — kept once (was partially restated across §4–§5). Not a no-op: this owner has corrected agents for asking routine questions; it overrides the default politeness threshold.

### §6 — Memory bank (whole section)
- Moved, not deleted — see Moves. The read-protocol sentence survives inline in the map row; the write/store/format/conflict rules go to `.memory-bank/README.md` (they fire only on the memory-write branch).

### §7 — Architecture quality
- ¶1 ("must work fully, fit the project logically, create no duplicate systems, live in the right place… Do not take the shortest path… Keep refactors in scope") — **duplication** of §1's island rule plus default-behaviour filler ("work fully", "right place"). Deleted; §7 ¶2's hero-seam rule moves to §1 next to "design nothing for one character" (co-location: it is the multi-hero direction's enforcement clause).

### §9 — Decompiled code
- Moved to `.memory-bank/decompile.md` — it's a procedure that fires on one branch (suspicious decompiler output); the memory-bank file is its natural home and already hosts the decompile.yml warning. Pointer row carries the trigger.

### §10 — Skills
- The one-row table — **duplication**: it restates §3's mandatory pointer. The meta-rules ("skills own repeatable workflows", "not a micro-action", "fix a disagreeing skill") compress into the map row.

### §11 — Tools (whole section)
- "Use the lightest tool that answers." — **no-op** (default tool-selection behaviour).
- gradle and git/gh rows — **env cache** (README documents `runClient`; git is default tooling).
- "javap on `original-jars/`" row — absorbed by the decompile pointer.
- "web docs … Do not guess unstable APIs from memory." — **moved** to `CODING_STANDARDS.md` (a real steer; agents hallucinate MC/Forge API names on decompiled code).

### §12 — Verification
- "Compilation proves nothing about behavior." — rhetorical frame; the rules carry the demand. **no-op** once the rules exist.
- "Check animations and VFX in first and third person." — **duplication**: the skill's §10 recipe already contains the full animation checklist (grounded/flying/sneaking/left-right/shadow pass). Single-sourced to the skill; `CONTRIBUTING.md` references it instead of restating.
- The build/runClient/never-claim core — kept in §4 (every-task rule). The dedicated-server clause is folded into the server/client bullet itself — co-location: the rule and its proof now sit together.

### §13 — Definition of Done
- The whole checklist — **duplication by design** (every item restates §3/§12/§6/§14). Compressed to one `Done` line in §2 that points at §3/§4 instead of restating them.

### §15 — Versioning
- "(inherited: core 1.10.3, flight 1.6.7)" — **env cache** (`*/build.gradle` `version = '...'`). The bump rule moves to `CONTRIBUTING.md` verbatim.

## Moves

| Content | Destination | Exact pointer line(s) left in `AGENTS.md` |
|---|---|---|
| §8 mixin style (`@Unique`, `(Target)(Object)this`, register-in-config, `ViltrumiteMixinPlugin`), networking (`CoreMessages`, C2S validation), Mojang names, rename-stability, lang sync; §11 Forge-docs rule | `CODING_STANDARDS.md` (new) | `\| Writing or changing code \| `CODING_STANDARDS.md` \| mixin style, networking, naming and id stability, lang sync \|` |
| §12 ship-check detail (CI artifact, dedicated-server proof, JUnit), §14 git/PR format, §15 versioning | `CONTRIBUTING.md` (new) | `\| Shipping a PR or building a jar \| `CONTRIBUTING.md` \| commit and PR format, version bumps, JUnit, ship checks \|` |
| §6 write/store/format/conflict rules | `.memory-bank/README.md` (new) | `\| Before every task \| `.memory-bank/project.md` (+ the task's topic files) \| durable agent memory; write rules: `.memory-bank/README.md` \|` |
| §9 decompile procedure (Vineflower caveats → `javap` check → record fix); `original-jars/` read-only (from §2/§8) | `.memory-bank/decompile.md` (existing — adds a "Suspicious code" section) | `\| Decompiled code looks wrong \| `.memory-bank/decompile.md` \| `javap` check, known artifacts and fixes; `original-jars/` is read-only \|` — the read-only rule rides inside the pointer (0 hops, always loaded, 5 words) |
| §10 skill mechanism (mandatory procedure, workflows-not-micro-actions, fix-on-disagree) | stays in `AGENTS.md`, compressed | `\| A repeatable workflow applies \| `.agents/skills/<name>/SKILL.md` \| mandatory procedure — `animation-system` for all animation/VFX work (§3); skills own workflows, not micro-actions; fix a skill that disagrees with the code \|` |
| §2's non-confessable subsystem knowledge | `.memory-bank/project.md` (already there — no edit needed) | the "Before every task" row covers it |
| §16 doc inventory, ASD-STE100 style rule, authority ladder, update-in-same-task rule | stays, compressed into §5 bullets | — |

**Why `CONTRIBUTING.md` and not folding ship-process into `CODING_STANDARDS.md`:** the two files answer different triggers — "writing code" vs "shipping". Verification detail, version bumps and PR format all fire at the same moment (the agent is about to commit/PR/build a jar); putting them behind one pointer means one file read per ship, and the code-conventions file stays a pure reference consulted while writing.

**Why `.memory-bank/README.md` and not keeping §6 inline:** the read protocol fires every task (kept inline in the map), but the write rules fire only when an agent has durable knowledge to store — a minority branch. An agent writing memory has by definition already followed the pointer. `project.md` already redirects ("See `AGENTS.md` §6"), so the change is a natural home move, not a new concept.

## Resulting AGENTS.md

```markdown
# AGENTS.md — Viltrumite Mods

Forge 1.20.1 multi-project (Java 17, Mojang mappings). One build, two mods:

- `viltrumitecore/` — `dev.baranhan.viltrumitecore`: abilities, combat, race, world events, animations, VFX. Depends on `viltrumiteflight`.
- `viltrumiteflight/` — `dev.baranhan.viltrumiteflight`: flight physics, flight animations, flight VFX.

Exact versions live in `gradle.properties` and `*/build.gradle`.

## 1. What we are building

A multi-hero superhero mod on a new architecture. Viltrumites come first; more heroes and characters follow one by one. Design nothing for one character only.

- A hero must feel like its source material — mechanics, timings, interactions, animations, VFX — and stay playable in Minecraft. Recognizability beats literal copying; faithful but unplayable is a failed hero.
- The sources are decompiled release JARs: working history, not target architecture. Keep player-facing behavior and visual identity; replace old structure when it blocks new heroes or duplicates logic.
- `grebeshok105/Codex-Superheroes` (Fabric 1.21.1) is dropped. Ideas and process only — never its code or Fabric APIs.
- Reuse or deliberately extend the shared mechanisms; a disconnected island is not acceptable. The first task adding a second hero introduces a shared hero contract in a plan: shared code asks the hero abstraction, never which concrete character the player is (no `if (hero == ...)` forks of Viltrumite paths).

## 2. How we work

- The user writes the **design spec** — what and why. The agent owns how. Non-trivial work starts from a written implementation plan (agent-produced from the repo, skills and memory bank); small obvious fixes need none. A plan never changes user design decisions silently, and settled decisions stay settled.
- A large unspecced ask (a hero, a system, a mechanic, a VFX direction, a UI) goes through the brainstorming gate first: research → 2–3 approaches with trade-offs → alignment with the user → final spec.
- Work end to end: study the repo, the memory bank and matching skills → plan → implement → verify → fix findings → self-review → commit and push.
- Answer every question that code, git, skills, the memory bank, the original JARs or docs can answer — no routine confirmations or status checks. Escalate only real design or product forks.
- **Done** = spec implemented, §3 satisfied, §4 verified, acceptance criteria checked, memory bank and skills updated when the task produced durable knowledge, work committed and pushed.

## 3. Animation and VFX guardrails

The current visuals are the identity of the mod. Breaking these rules needs explicit user approval.

1. **Skill first.** Before any work on animations, poses, model mixins, renderers, particles, shaders or VFX, read `.agents/skills/animation-system/SKILL.md` and follow it.
2. **Existing system only.** Animations and effects use the mod's own animation core, model mixins, render layers, particles and post shaders (mapped in the skill). Never add a second animation or VFX system — no PlayerAnimator, Emotecraft, GeckoLib animation, Veil, or another keyframe/pose engine. (GeckoLib is `compileOnly` compat for one mixin.)
3. **Full coverage.** A hero or ability is not done until each ability has its own animation and visual feedback in the current style.
4. **Preserve.** Do not remove, replace, simplify or restyle an existing effect unless the user asks; a refactor must give the same visual result.
5. **Extend in place.** If the system cannot show a needed animation, extend the existing packages and update the skill in the same task. If the skill is missing or disagrees with the code, study the code and fix the skill.

## 4. Hard rules

- Gameplay is server-authoritative. Rendering, HUD, particles, camera, keybinds, screens and model mixins are client-only: `client/` packages, `*.client.mixins.json`. A dedicated server must never load a client class — if the split or a mixin config changed, prove one starts (`:viltrumitecore:runServer`).
- `viltrumitecore` depends on `viltrumiteflight`; `viltrumiteflight` must not depend on `viltrumitecore`.
- `./gradlew build` must pass. Runtime changes (gameplay, input, rendering, entities, networking, VFX, HUD) need a check in `./gradlew runClient`. Never claim a check you did not run — if the game cannot launch, say so in the PR.

## 5. Reference map

Open a file when its trigger fires. Each file is the mandatory source for its branch.

| Trigger | File | It owns |
|---|---|---|
| Before every task | `.memory-bank/project.md` (+ the task's topic files) | durable agent memory; write rules: `.memory-bank/README.md` |
| Writing or changing code | `CODING_STANDARDS.md` | mixin style, networking, naming and id stability, lang sync |
| Shipping a PR or building a jar | `CONTRIBUTING.md` | commit and PR format, version bumps, JUnit, ship checks |
| A repeatable workflow applies | `.agents/skills/<name>/SKILL.md` | mandatory procedure — `animation-system` for all animation/VFX work (§3); skills own workflows, not micro-actions; fix a skill that disagrees with the code |
| Decompiled code looks wrong | `.memory-bank/decompile.md` | `javap` check, known artifacts and fixes; `original-jars/` is read-only |

Other docs: `README.md` — public overview (Russian).

- Write agent docs in simplified technical English: short sentences, one term per concept, explicit rules. Update a doc in the task that made it stale; prefer editing an existing file over creating one.
- Authority when sources disagree: current code and original JARs → `AGENTS.md` → skills → memory bank → other docs.
```

**58 lines** — 35% of the current 164, under the half-budget with the guardrail sections kept in full.

## New/changed files

### `CODING_STANDARDS.md` (new, ~27 lines) — full text

```markdown
# Coding standards — Viltrumite Mods

Code-level conventions. Global rules (server/client split, module dependencies, verification) live in `AGENTS.md`; animation and VFX procedure lives in the `animation-system` skill. If this file and the code disagree, the code wins.

## Mixins

- Keep mixins narrow.
- Mark added members `@Unique`.
- Write self-casts as `(Target)(Object)this` — javac rejects the decompiler's `(Target)this` form.
- Register each mixin in the correct config: `<modid>.mixins.json` (common) or `<modid>.client.mixins.json` (client). `defaultRequire` is 1 — a bad target crashes the game.
- Respect `ViltrumiteMixinPlugin` when adding or moving mixins.

## Networking

- Core networking goes through `CoreMessages` and `network/packet/`; flight uses its own `network/`.
- A C2S handler validates the sender and the player state before it acts.

## Naming and APIs

- Use Mojang official names and public Forge/Minecraft APIs.
- Forge 1.20.1 APIs: check current docs before using an unfamiliar API. Do not guess unstable APIs from memory.
- Never rename mod ids, packages, registry ids, synced data keys, NBT keys or config keys without a migration — renames break worlds and configs.

## Resources and decompiled sources

- Update all lang files of a mod together.
- The sources are decompiler output. When decompiled code looks wrong, use the `javap` check in `.memory-bank/decompile.md` before trusting it.
```

### `CONTRIBUTING.md` (new, ~23 lines) — full text

```markdown
# Contributing — Viltrumite Mods

Ship-time process: read before opening a PR or building a jar. Code conventions: `CODING_STANDARDS.md`. Global rules: `AGENTS.md`.

## Ship checks

- `./gradlew build` must pass locally or in CI (`build.yml`, `mod-jars` artifact).
- Runtime changes (gameplay, input, rendering, entities, networking, VFX, HUD): check in `./gradlew runClient`. Animation/VFX checks cover first and third person — checklist in the `animation-system` skill §10.
- Changes to the client/common split or mixin configs: prove a dedicated server starts (`./gradlew :viltrumitecore:runServer`).
- There is no automated test suite yet. Add JUnit tests for new non-trivial pure logic; the first task that needs them sets up JUnit in that module.

## Versioning

- Each mod versions in its `build.gradle` (`version = '...'`).
- Before building a jar for the user, bump each changed mod: bugfix only → patch +1; features or content → minor +1, patch resets.
- Never ship two different jars with the same version.

## Git and PR format

- GitHub is the only home of the work: one branch per task, small logical commits in English, conventional style (`feat(scope): ...`, `fix(scope): ...`, `docs(scope): ...`). Each finished task ships as its own PR; nothing task-related stays only on a local machine.
- **Title in Russian**, explicit, names the task («фикс захвата у вилтрумитов», not «fix» or «wip»).
- **Body starts with «Для игрока»**: what changed and how it works, for players, in Russian, no technical part. Emojis are welcome.
- **Below: the technical part for other agents**: what, why, key decisions, what you verified and how, known limits.
```

### `.memory-bank/README.md` (new, ~26 lines) — full text

```markdown
# Memory bank — write rules

`.memory-bank/` is persistent project memory for AI agents. Main file: `project.md`; topic files: `<topic>.md`. The read protocol and the authority order live in `AGENTS.md`.

## What to store

Only knowledge that is expensive, ambiguous or impossible to recover from the code:

- discovered system behavior;
- architecture relations;
- decompile artifacts and their confirmed fixes;
- important invariants;
- project-specific implementation details;
- decisions and their reasons;
- behavior confirmed against the original JARs.

## What not to store

- Facts the code shows directly.
- Task logs, changelogs, scratchpad notes, session dumps.

## Format

- Update the memory bank only when a task produced durable knowledge for future agents.
- Short, factual entries; one fact per bullet; mark unverified facts `unverified`.
- If an entry conflicts with the current code or verified JAR behavior, update or remove the entry in the same task.
```

### `.memory-bank/decompile.md` (edit — append a section; the §9 procedure lands in its natural home)

```markdown
## Suspicious code

- Vineflower output can be wrong in loops, casts, lambdas, switches and generics.
- Before trusting strange code, compare it with the original bytecode: `javap -c -p` on the class inside `original-jars/`.
- Record each confirmed decompile fix in this file.
```

Also trim its existing "Use this form in all new mixins" tail of the self-cast line — the imperative now lives in `CODING_STANDARDS.md`; keep the fact ("fixed in 39 files").

### `.memory-bank/project.md` (one-line edit)

Line 3: "See `AGENTS.md` §6 for the rules." → "See `.memory-bank/README.md` for write rules." (the section number it cites no longer exists).

### Not created

- `docs/` — rejected: a `docs/repo-map.md` would be a stale copy of `ls`; the non-confessable parts of the map already live in `project.md`.
- No separate verification doc — split across `AGENTS.md` (the rule) and `CONTRIBUTING.md` (the ship-time detail) instead.

## Risks

- **Pointer under-fire on `CODING_STANDARDS.md`.** The pointer's branch ("writing or changing code") is nearly universal for a code agent — an agent that skips the read loses mixin/networking/id-stability rules it *half*-knows from memory. Mitigations in the text: the map intro states files are "the mandatory source for their branch", and the pointer lives in a table column literally named "Trigger". Residual risk is real — nothing enforces the read — but it's the same exposure goal 3 accepts.
- **`CONTRIBUTING.md` fires late.** If an agent drafts a PR from habit before consulting the map, it produces a non-«Для игрока» body once, gets corrected, then complies. The `Done` line ends on "committed and pushed", which is adjacent to the shipping trigger — partial mitigation, not a guarantee.
- **Repo-map deletion raises orientation cost.** A cold agent must run `ls`/`find` where §2 previously handed it the layout. Deliberate: the layout confesses itself cheaply, and the non-obvious knowledge (PlayerEntityCoreMixin refactor target, anim loader) survives in `project.md`, which the "Before every task" trigger now reads earlier than §2 was ever reached.
- **`decompile.md` becomes mixed-content** (artifacts log + a procedure). Acceptable: it already carried process rules (don't re-run `decompile.yml`, self-cast form); the procedure is the same domain.
- **Two-hop ceiling respected but shallow-proofed.** Worst path is `AGENTS.md` → `CODING_STANDARDS.md` → `.memory-bank/decompile.md` for the javap check = 2 hops. No rule exceeds it; several deleted lines were 0-hop but env-confessable.
- **§5 table does double duty** (pointer index + residual meta-rules). An agent reading only the table could miss the two bullets below it (style rule, authority order). Low impact: both are restatement-resistant conventions, and the authority order is also implied by `CODING_STANDARDS.md`'s "code wins" line.
- **Version numbers deleted** — if a doc elsewhere cites "1.10.3" it now disagrees with nothing; versions are read from `build.gradle` at bump time. Low risk, correct-by-construction.

## Line budget

| | Lines | Loaded when |
|---|---|---|
| Current `AGENTS.md` | 164 | every turn |
| Proposed `AGENTS.md` | **58** (−106, −65%) | every turn |
| `CODING_STANDARDS.md` (new) | 27 | code-change branches |
| `CONTRIBUTING.md` (new) | 23 | ship branches |
| `.memory-bank/README.md` (new) | 26 | memory-write branches |
| `.memory-bank/decompile.md` | +4 net | decompile-weirdness branches |

Where the 106 lines went: ~45 deleted outright (repo-map table, tools table, DoD restatement, §3's skill-duplicated enumerations, duplicate reuse/autonomy/authority paragraphs, env-cached version strings, rhetorical framing); ~61 disclosed behind the five trigger pointers (coding conventions, ship process, memory write rules, decompile procedure). Always-loaded cost drops ~65% while every behavioural rule stays reachable in ≤2 hops — most in 1.
