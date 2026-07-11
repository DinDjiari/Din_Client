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

## Phase 2 (render library + themed UI) notes

### GUI background blur

No custom post-process shader was needed: Minecraft 1.21.1 ships a Gaussian
blur post-chain (`shaders/post/blur.json`) that vanilla screens already use.
Client screens call the vanilla `renderBlurredBackground` and lay the theme's
charcoal tint over it, so blur strength follows the user's vanilla
"Menu Background Blurriness" accessibility slider — which is also what the
Theme Editor's "Blur Intensity" slider reads and writes. Setting it to 0
disables blur entirely (the darkened overlay remains), which doubles as the
spec's darkened fallback. Verified working under Mesa/llvmpipe software GL.

### Design-reference deviations (intentional)

- **Vanilla sub-screens**: Options, Create New World, Edit Server info,
  Direct Connect and the Mods list are still the vanilla screens; the design
  reference does not cover them. They are reachable from the themed screens.
- **World/server icons**: cards draw the reference's generic icon squares
  (world initial / accent glyph), not `icon.png`/server favicons.
- **Singleplayer card actions**: the reference shows only Play / Create / Back,
  so Edit/Delete/Re-Create world management is not reachable from the themed
  screen in this phase.
- **Realms**: the themed menus have no Realms entry (not in the reference);
  disconnecting from a Realm lands on the Multiplayer screen.
- **Font dropdown**: rendered as a cycle chip (click to switch Inter ↔
  JetBrains Mono) rather than a dropdown list; with two bundled fonts a list
  adds nothing. A font change applies to screens (re)opened afterwards —
  already-built labels on the current screen keep their style until re-init.
- **Click GUI panel positions** persist for the session (dragging is kept
  while the game runs) but are not yet written to the config profile; that is
  planned alongside the Phase 3 HUD editor layout persistence.
- **Reference modules**: the mock shows future modules (FPS Display, CPS
  Counter, Zoom, Fullbright, …) that belong to later phases; category panels
  list only modules that actually exist (Sprint, Theme) and honestly label
  empty categories "No modules yet" instead of showing non-functional rows.

### Performance measurement environment

The 60+ FPS acceptance target cannot be measured meaningfully in the authoring
container: rendering runs on Mesa **llvmpipe software rasterisation** (no GPU),
which caps the whole game — vanilla screens included — well below what any real
GPU does. The render library itself introduces no per-frame allocations and
batches through the vanilla GUI render types; the F6 debug screen shows a live
FPS readout for verification on real hardware.
