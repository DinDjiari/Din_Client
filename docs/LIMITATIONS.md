# Limitations

This document records anything that cannot be implemented as originally
specified, together with the reason and the closest workable alternative. It is
maintained per the project's golden rules (do not fake features — document them).

---

## Build verification in the authoring environment (Phase 1)

**Status: environmental, not a code limitation.**

The code in this repository was authored in a sandbox whose outbound network
policy **blocks the NeoForge Maven repository** (`maven.neoforged.net`) and the
Minecraft library host (`libraries.minecraft.net`). Both return HTTP `403
Forbidden` through the egress proxy, and NeoForge is **not** mirrored on Maven
Central (verified: `404`).

Because ModDevGradle downloads the NeoForge toolchain (NeoForge itself, the
`neoform-runtime` decompilation pipeline, and the Minecraft artifacts) from that
host, a full `gradle build` / `gradle runClient` **cannot complete in the
authoring environment**. The exact, reproducible failure is:

```
> Task :createMinecraftArtifacts FAILED
  > Could not resolve net.neoforged:neoform-runtime:1.0.24.
    > Received status code 403 from server: Forbidden
        (https://maven.neoforged.net/.../neoform-runtime-1.0.24.pom)
```

The ModDevGradle plugin itself resolves and the Gradle build script evaluates
cleanly (`gradle help` succeeds); only the gated artifact download fails.

### What *was* verified

To avoid shipping unverified logic, the framework core was deliberately written
to be **free of any Minecraft/NeoForge dependency** (it uses only Gson and the
SLF4J API, both of which the NeoForge runtime also provides). That core —
`gg.dindijari.client.setting.*`, `gg.dindijari.client.module.Module`,
`gg.dindijari.client.module.Category`, and `gg.dindijari.client.config.*` — was
compiled with `javac` and exercised by a round-trip harness that confirmed:

- boolean / number / colour / enum / keybind settings serialize and restore;
- `NumberSetting` clamping and step-snapping;
- module enabled-state and setting values persist across a simulated restart
  (save with one object graph, reload into a fresh one);
- profile create / switch / delete / export / import;
- refusal to delete the active profile.

### What remains compile-unverified

Only the thin NeoForge glue layer could not be compiled here, because it
references Minecraft/NeoForge types that require the blocked toolchain:

- `gg.dindijari.client.core.DindijariClient` (the `@Mod` entrypoint)
- `gg.dindijari.client.module.ModuleManager` (event subscriptions)
- `gg.dindijari.client.module.modules.SprintModule`

It is written against the NeoForge 21.1.x / Minecraft 1.21.1 (official Mojang
mappings) API. **To fully satisfy the Phase 1 acceptance criteria, build it in
an environment where `maven.neoforged.net` and `libraries.minecraft.net` are
reachable** (`./gradlew build` then `./gradlew runClient`). If the pinned
`neo_version` in `gradle.properties` is not the latest 21.1.x, bump it there.

---

## Reserved for later phases

Feature-level limitations called out by the specification (GUI blur fallback,
GPU/CPU utilisation HUD, reach display, emotes, client-side-only cosmetics,
loading-overlay hook availability, etc.) will be documented here as each phase
is implemented, rather than pre-emptively.
