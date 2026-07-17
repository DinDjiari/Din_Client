# Dindijari Client

A **client-side** quality-of-life, HUD and cosmetics mod for Minecraft
**1.21.1** on **NeoForge**. It is explicitly **not** a cheat client — there are
no combat, movement or gameplay-advantage modules.

- **Version:** 2.0.0 (keep `gradle.properties`, `DindijariClient.MOD_VERSION`
  and this line in sync)
- **Mod ID:** `dindijariclient`
- **Group:** `gg.dindijari`
- **Minecraft:** 1.21.1
- **Loader:** NeoForge 21.1.x
- **Java:** 21
- **License:** GPL-3.0-only
- **Distribution:** client-only (`Dist.CLIENT`) — never loads on a dedicated
  server, adds no packets, and is safe to use on vanilla servers.

## What's new in 2.0.0

- **Sodium support (recommended):** [Sodium](https://modrinth.com/mod/sodium)
  is now a declared *optional* dependency — it ships native NeoForge builds
  for 1.21.1 (`mc1.21.1-0.8.12-neoforge`, Modrinth project `AANobbMI`). The
  client detects it by mod id only (`ModList.isLoaded("sodium")`), never
  bundles or touches its code, and works identically without it. The
  Performance panel shows whether Sodium is present; if it's missing, a
  one-time dialog at the main menu recommends it ("Nicht mehr anzeigen"
  suppresses it permanently). The client's UI ships **no mixins** and renders
  through the vanilla `GuiGraphics` pipeline, so there is nothing for Sodium
  to conflict with.
- **Branding:** set a custom OS **window title** (empty = default
  "Dindijari Client") and a custom **window icon** from any common image file.
  The built-in converter center-crops non-square images and produces RGBA PNGs
  at 16/32/48 px under `config/dindijariclient/icon/`, applied immediately and
  re-applied on every launch. Non-image files are rejected with a
  notification. (macOS/Wayland cannot change window icons at runtime — see
  `docs/LIMITATIONS.md`.)
- **Crash assistant:** crashes are recorded (uncaught-exception hook plus a
  `crash-reports/` scan on shutdown) and presented on the **next** start in a
  themed crash screen — scrollable monospace report, *Copy report*, *Open
  folder*, *Dismiss*, and **Analyze with AI**. Analysis runs **entirely
  locally** against your own [Ollama](https://ollama.com) install
  (`http://localhost:11434`, model `llama3.2` by default; both configurable in
  Client Settings → Crash Assistant). No API key, no cost, nothing leaves your
  machine — and if Ollama isn't running, the screen says so honestly instead
  of fabricating an answer.
- **UI animations & sounds:** screen/dialog entrance and exit transitions,
  module-toggle ripples, animated settings expansion, button press flashes and
  sliding notification toasts — all frame-rate independent and fully disabled
  by Performance Mode or the "UI Animations" toggle. Subtle UI sounds (hover
  tick, click, rising/falling toggle tones, notification pop, dialog swell,
  error buzz) play through the vanilla sound system on the master category, so
  the vanilla master volume applies; a "UI Sounds" toggle and volume slider
  live in Client Settings → Interface. All sound effects were **synthesized
  from scratch for this project** (see `tools/make_sounds.py`) and are
  released **CC0** — no third-party audio is bundled.

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
