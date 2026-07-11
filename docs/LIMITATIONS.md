# Limitations

This document records anything that cannot be implemented as originally
specified, together with the reason and the closest workable alternative. It is
maintained per the project's golden rules (do not fake features — document them).

---

## Build verification (Phase 1) — RESOLVED

**Status: resolved. Phase 1 is build- and runtime-verified.**

The Phase 1 code was originally authored in a sandbox whose network policy
blocked `maven.neoforged.net`, so the NeoForge glue layer
(`core/DindijariClient`, `module/ModuleManager`, `modules/SprintModule`)
shipped compile-unverified while the MC-independent core was verified with
`javac` plus a round-trip harness.

That gap has since been closed in an environment with full network access:

- `gradle build` (Gradle 8.14.3, NeoForge 21.1.193, Minecraft 1.21.1,
  official Mojang mappings) **passes with no changes** to the glue layer.
- `gradle runClient` boots to the title screen; the log shows
  `Module framework initialized with 1 modules` and `Dindijari Client ready`.
- Phase 1 acceptance was exercised end-to-end in the running client (driven
  via Xvfb + xdotool): pressing `J` in-world toggled Sprint, the debounced
  autosave wrote `enabled: true` to
  `config/dindijariclient/profiles/default.json`, the value survived a clean
  quit, and after a full client restart the restored in-memory state was
  confirmed by toggling again.

One caveat only applies to the wrapper: `gradlew` downloads its Gradle
distribution from GitHub Releases, which some egress policies block. A locally
installed Gradle 8.x works identically.

---

## Reserved for later phases

Feature-level limitations called out by the specification (GUI blur fallback,
GPU/CPU utilisation HUD, reach display, emotes, client-side-only cosmetics,
loading-overlay hook availability, etc.) will be documented here as each phase
is implemented, rather than pre-emptively.
