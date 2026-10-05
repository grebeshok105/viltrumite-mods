# Decompile artifacts and fixes

## Pipeline
- Tool: Vineflower, remapped to Mojang official names (`tools/decompile.py`, CI `decompile.yml`).
- Result: 189 Java files in core, 41 in flight. 0 unmapped SRG names.
- `DECOMPILE_REPORT.md` "Compile check" is stale: it shows the first failed build. The build is green after the fixes below.
- Do not run `decompile.yml` again. It would overwrite the manual fixes.

## Fixes
- Mixin self-casts: Vineflower wrote `(Target)this`. javac rejects it. Fixed to `(Target)(Object)this` in 39 files. Use this form in all new mixins.
- GeckoLib 4.4.9 is `compileOnly` in core: one mixin renders GeckoLib mobs. Runtime need of GeckoLib: unverified.
- `PlayerEntityCoreMixin.tickGrab`: the target-search loop was broken by the decompiler and was rewritten by hand. Equivalence with the JAR: unverified. Check grab in game before you build on it.