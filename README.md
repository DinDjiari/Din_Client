# Dindijari Client

A **client-side** quality-of-life, HUD and cosmetics mod for Minecraft
**1.21.1** on **NeoForge**. It is explicitly **not** a cheat client — there are
no combat, movement or gameplay-advantage modules.

- **Mod ID:** `dindijariclient`
- **Group:** `gg.dindijari`
- **Minecraft:** 1.21.1
- **Loader:** NeoForge 21.1.x
- **Java:** 21
- **License:** GPL-3.0-only
- **Distribution:** client-only (`Dist.CLIENT`) — never loads on a dedicated
  server, adds no packets, and is safe to use on vanilla servers.

## Project status

Implemented in phases (see the specification). **Phase 1 — Foundation** is in
place:

- Gradle + ModDevGradle project for a client-only NeoForge 1.21.1 mod.
- **Module framework:** `Module` base class (name, description, category,
  enabled state, toggle keybind, settings, `onEnable`/`onDisable`/`onTick`
  hooks) and a `ModuleManager` with name/category lookup and keybind dispatch
  via NeoForge input events.
- **Typed settings:** `BooleanSetting`, `NumberSetting` (min/max/step),
  `ColorSetting` (with RGB-cycle mode), `EnumSetting`, `KeybindSetting`, all with
  change callbacks and JSON serialization.
- **Config system:** Gson persistence under `config/dindijariclient/`, debounced
  autosave, and named profiles (create / switch / delete / import / export).
- **Reference module:** `Sprint` (auto-sprint-while-moving), toggled by a keybind
  (default `J`), with state persisted across restarts.

## Building

Requires **JDK 21** and network access to the NeoForge Maven repository.

```bash
./gradlew build       # compile + assemble the mod jar (build/libs)
./gradlew runClient   # launch a development client
```

> **Note:** The NeoForge toolchain is downloaded from `maven.neoforged.net`. If
> that host is unreachable in your environment the build cannot fetch NeoForge —
> see [`docs/LIMITATIONS.md`](docs/LIMITATIONS.md) for details and for how the
> Minecraft-independent core was verified in isolation.

## Installing

Build the jar (`build/libs/dindijariclient-<version>.jar`) and drop it into the
`mods/` folder of a NeoForge 1.21.1 profile (e.g. via the vanilla launcher with
NeoForge installed, or a launcher such as Prism).

## Repository layout

```
src/main/java/gg/dindijari/client/
├── core/     # @Mod entrypoint + service registry
├── module/   # Module base, ModuleManager, categories, reference modules
├── setting/  # Typed, serializable settings
└── config/   # JSON persistence + profiles
```

## License

GPL-3.0-only. See [`LICENSE`](LICENSE).
